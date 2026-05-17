# ADR-0001: Identity, Access Management, and Device Registration Architecture

| Field         | Value                                       |
|---------------|---------------------------------------------|
| **Status**    | Accepted                                    |
| **Date**      | 2026-03-07                                  |
| **Authors**   | Platform Architecture                       |
| **Deciders**  | Platform Architecture                       |
| **Migrated from** | Project-wide ADR-003                    |

---

## Context

The platform requires a robust, scalable identity and access management (IAM) strategy that covers:

- Per-device access control with owner and shared-access semantics
- Enterprise customer identity federation via external IDPs
- Support engineer impersonation for troubleshooting
- A two-phase device lifecycle: platform registration (device-initiated) followed by user association (user-initiated)

Device registration is a two-phase process. A device first registers itself with the platform automatically on first power-on, establishing its identity but remaining unowned. A user subsequently claims ownership by scanning or entering a claim code, or via BLE proximity detection in the mobile app. These two phases have distinct actors, failure modes, and downstream implications, and are modelled as separate sagas emitting separate Kafka events.

The backend is built on Quarkus, deployed on OpenShift, and uses a Kafka-based event bus for internal service communication. Keycloak is deployed via the Red Hat Build of Keycloak (RHBK) Operator on OpenShift, backed by a Crunchy Postgres cluster. The primary datastore uses a schema-per-tenant PostgreSQL model with PostGIS extensions.

---

## Decision

### 1. Keycloak as the Single Source of Truth for IAM

Keycloak is the authoritative source for identity and access control across the platform. All authentication and authorisation decisions are delegated to Keycloak. The Quarkus backend (`gps-api`) is the sole OIDC resource server and the single API surface for external consumers.

Frontend clients (mobile, web) and the support console are separate OIDC clients that obtain tokens and call `gps-api`. Internal services that interact directly at the database layer are outside the Keycloak authorisation scope and authenticate to Postgres using dedicated database credentials.

### 2. Authorization Services over Groups for Device-Level Access

Device-level access control will use **Keycloak Authorization Services** (resource-based permissions) rather than a group-per-device hierarchy.

#### Rejected alternative: Group-per-device hierarchy

An initial approach considered creating a Keycloak group per device (e.g. `/devices/{deviceId}/modify`, `/devices/{deviceId}/read-only`). This was rejected for the following reasons:

- At millions of devices, group hierarchy traversal queries in Keycloak's relational store degrade significantly regardless of horizontal pod scaling
- The Keycloak Infinispan cache would need to hold millions of group entries, creating memory pressure across all pods or frequent cache misses hitting Postgres
- Groups are semantically designed for collections of users, not per-object resource permissions

#### Accepted approach: Authorization Services resources

Each device is represented as a **resource** within the `gps-api` resource server client, with two **scopes**:

- `device:read` — view device location, telemetry, and configuration
- `device:modify` — update configuration, manage sharing, trigger actions

Access is controlled via **policies** attached to each device resource:

| Policy type        | Use case                                               |
|--------------------|--------------------------------------------------------|
| User-based policy  | Device owner; additional users granted explicit access |
| Group-based policy | Support engineers, platform admins                     |
| Org-based policy   | Enterprise devices shared at the organisation level    |

The `quarkus-keycloak-authorization` extension handles authorisation evaluation on the backend. Resource and policy provisioning (at registration time and for sharing operations) is performed via the Keycloak Admin REST API.

### 3. Keycloak Groups for Logical User Collections Only

Keycloak groups are retained exclusively for logical collections of users, not for device access. Examples:

- `support-engineers` — users permitted to impersonate other users
- `org-admins` — users with administrative rights within a Keycloak Organisation
- `beta-users` — users enrolled in feature flag cohorts

This keeps the group namespace small, bounded, and human-meaningful.

### 4. Keycloak Organizations for Enterprise Identity Federation

Enterprise customers will use **Keycloak Organizations** (GA in Keycloak 26 / RHBK) to federate their own IDP for authentication while Keycloak remains the authoriser.

- Enterprise users authenticate via their corporate IDP; Keycloak brokers the identity
- Org membership flows into tokens and is used to evaluate org-scoped device policies
- Devices owned by or shared with an organisation are accessible to org members based on their org role

> **Risk note:** Keycloak Organizations is a relatively new feature. The interaction between org membership, token claims, and Authorization Services policy evaluation should be validated via a prototype before relying on it in production. Some Admin API operations for Organisations may require raw HTTP calls where keycloak-admin library coverage is incomplete.

### 5. Impersonation for Support Engineers

Keycloak's built-in impersonation capability is used for support engineer troubleshooting. Members of the `support-engineers` group are granted the `impersonation` role scoped appropriately within Keycloak.

Because all permissions are evaluated against the token subject, an impersonating support engineer receives a token that represents the target user. The backend requires no special casing — the impersonating engineer sees exactly what the target user sees, inheriting their device access policies automatically.

### 6. Two-Phase Device Lifecycle

Device registration is split into two distinct phases, each with its own saga, Kafka event, and set of downstream consumers.

---

#### Phase 1 — Platform Registration (device-initiated)

Triggered automatically on first power-on. The device has no owner at this stage.

**Device identity — Keycloak service accounts (client credentials grant)**

Each device is provisioned as a dedicated confidential Keycloak client. The device UUID, stored in NV memory, is used as the Keycloak client ID. This creates a single stable key that links the physical device, the Postgres record, the Keycloak client, and the Keycloak Authorization Services resource.

The registration saga:

1. Device calls the platform registration endpoint (mTLS or equivalent transport security)
2. Registration service creates the Keycloak confidential client using the device UUID as the client ID, generating a client secret
3. Client secret is returned to the device once only and stored in NV memory (NV encryption via ESP32 flash encryption is mandatory for production devices)
4. Registration service creates the device record in Postgres with `user_id = null` and `organisation_id = null`
5. Registration service creates the Keycloak Authorization Services resource for the device — no owner policy is attached yet
6. Emits `device.platform.registered` to Kafka
7. Device uses stored client secret to obtain tokens via client credentials grant for all subsequent telemetry submissions

**Token handling for intermittent connectivity**

Devices cache their current token and re-authenticate using the stored client secret on receiving a `401` response, rather than tracking token expiry locally. This approach is resilient to clock drift on embedded hardware and handles extended periods offline (flat battery, poor connectivity) cleanly — the device re-authenticates on next power-on without any time-sensitive state.

---

#### Phase 2 — User Association (user-initiated)

Triggered by the user claiming ownership of an unowned device. Two discovery mechanisms are supported:

- **Manual claim code** — user enters a code in the mobile or web app
- **BLE proximity detection** — mobile app detects a nearby unowned device via BLE advertisement and prompts the user to associate

**Claim code derivation**

The claim code is a short-lived, single-use token derived from the device UUID. A rotating code is used in the BLE advertisement rather than the raw UUID, to prevent passive enumeration of device identities by nearby parties. The derivation uses an HMAC (e.g. HMAC-SHA256) over the device UUID and a time-windowed nonce, producing a short alphanumeric code. The registration service validates the code server-side by recomputing it for the current and previous time window (to handle window boundary edge cases).

The association saga:

1. User submits claim code (manually entered or received from BLE advertisement) via the mobile/web app
2. Backend validates the claim code against the device UUID, confirms the device exists and has `user_id = null`
3. Backend validates user subscription entitlement: `subscription_active = true` and `subscription_used < subscription_entitlement`
4. Registration service updates `devices.user_id` in Postgres and increments `users.subscription_used`
5. Registration service attaches the owner policy to the Keycloak Authorization Services resource (user-based policy scoped to the claiming user's Keycloak ID)
6. If the user belongs to a Keycloak Organisation, org-level policy is attached at this point
7. Emits `device.user.associated` to Kafka

Downstream consumers (anomaly detection model seeding, geofencing defaults, etc.) react to `device.user.associated` rather than `device.platform.registered`, since meaningful per-user context is only available after association.

---

### 7. Kafka Event Schemas

All events must be self-contained — consumers must not need to call back into the registration service or Keycloak to act on them. A schema registry (Avro or Protobuf) must be used to enforce these contracts across all consumers.

#### `device.platform.registered`

| Field                | Type     | Description                                          |
|----------------------|----------|------------------------------------------------------|
| `eventId`            | UUID     | Unique event identifier                              |
| `eventTimestamp`     | ISO-8601 | UTC timestamp of event emission                      |
| `deviceId`           | UUID     | Device UUID (also the Keycloak client ID)            |
| `keycloakClientId`   | String   | Keycloak confidential client ID (same as `deviceId`) |
| `keycloakResourceId` | UUID     | Keycloak Authorization Services resource ID          |
| `deviceMetadata`     | Object   | See fields below                                     |

The `deviceMetadata` object:

| Field                        | Type     | Description                                                           |
|------------------------------|----------|-----------------------------------------------------------------------|
| `deviceMetadata.name`        | String   | Human-readable device name (varchar 255)                              |
| `deviceMetadata.description` | String?  | Optional device description (varchar 255)                             |
| `deviceMetadata.registered`  | ISO-8601 | UTC timestamp of platform registration                                |
| `deviceMetadata.imagePath`   | String?  | Path to device image asset, if provided at registration               |
| `deviceMetadata.status`      | Object   | Initial device status payload (jsonb — structure TBD per device type) |

#### `device.user.associated`

| Field                | Type     | Description                                                    |
|----------------------|----------|----------------------------------------------------------------|
| `eventId`            | UUID     | Unique event identifier                                        |
| `eventTimestamp`     | ISO-8601 | UTC timestamp of event emission                                |
| `deviceId`           | UUID     | Device UUID                                                    |
| `ownerId`            | UUID     | Keycloak user ID of the associating user                       |
| `tenantId`           | String   | Tenant schema identifier                                       |
| `organisationId`     | UUID?    | Keycloak Organisation ID, if user belongs to an enterprise org |
| `keycloakResourceId` | UUID     | Keycloak Authorization Services resource ID                    |
| `assignedScopes`     | String[] | Scopes provisioned: `["device:read", "device:modify"]`         |
| `deviceMetadata`     | Object   | Same structure as in `device.platform.registered`              |

`ownerId` maps to `devices.user_id` and `organisationId` maps to `devices.organisation_id` (nullable). User subscription details are not duplicated into the event — consumers that need subscription context should query the appropriate service.

---

## Consequences

### Positive

- Authorization Services resources scale significantly better than a group-per-device hierarchy for the anticipated data volumes
- Impersonation is handled at the identity layer — no application-level special casing required
- Two-phase registration cleanly separates device identity (platform concern) from ownership (user concern), making each saga independently testable and failure-resilient
- Per-device Keycloak service accounts give each device a cryptographically distinct identity with no shared credential risk across the fleet
- Token caching with 401-triggered refresh is resilient to clock drift and extended offline periods without requiring time-synchronisation on embedded hardware
- BLE proximity detection provides a seamless association UX with no manual code entry required, while falling back gracefully to manual claim codes
- HMAC-derived rotating BLE advertisement codes prevent passive enumeration of device identities
- Kafka decouples the registration service from downstream consumers, improving resilience
- The single resource server client model is a clean fit for one cohesive API surface
- Internal services retain the least-privilege DB role pattern without polluting the OIDC model

### Negative / Risks

- Keycloak Authorization Services is more complex to set up than group-based access; resource and policy provisioning requires careful design and more Admin API surface
- Per-device Keycloak clients at fleet scale (millions of devices) introduces a large number of clients in the realm — Keycloak's client management at this scale should be load-tested early, particularly client lookup and token issuance performance
- Every device platform registration involves multiple sequential Keycloak API calls — the registration service must be load-tested at realistic fleet onboarding volumes
- Keycloak Organizations is a maturing feature; interaction with Authorization Services policy evaluation requires early prototype validation
- NV memory encryption (ESP32 flash encryption) must be enforced for production devices — the client secret stored in NV is the long-term credential and must be protected accordingly
- Internal services writing directly to Postgres can cause drift between data state and Keycloak authorization state — any internal service operation with authorization implications must also update the corresponding Keycloak resource/policy via the registration service or a dedicated IAM sync pathway
- HMAC-derived claim code validation requires the registration service to maintain or derive the same time-windowed nonce — clock skew between device and server must be accounted for in the validation window

### Neutral

- The `quarkus-keycloak-authorization` extension handles the evaluation side well; the provisioning side (resource/policy CRUD) requires direct Admin REST API calls
- Schema registry adoption (Avro/Protobuf) is an additional infrastructure component but is strongly recommended given the multi-consumer event model
- BLE advertisement integration requires the mobile app to handle the rotating claim code derivation client-side and submit it through the standard association flow — no additional backend surface is required

---

## References

- [Keycloak Authorization Services documentation](https://www.keycloak.org/docs/latest/authorization_services/)
- [Keycloak Organizations documentation](https://www.keycloak.org/docs/latest/server_admin/#organizations)
- [Quarkus Keycloak Authorization extension](https://quarkus.io/guides/security-keycloak-authorization)
- [Red Hat Build of Keycloak Operator](https://access.redhat.com/documentation/en-us/red_hat_build_of_keycloak)
- [Crunchy Postgres Operator](https://access.crunchydata.com/documentation/postgres-operator/)
