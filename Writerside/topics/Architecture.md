# Backend Architecture

CEREBRAL STRATUM is a multi-tenant GPS tracking SaaS platform with AI-powered anomaly detection, geofencing, and a notification system — designed as an intelligent layer of services, communications, and protocols.

---

## Tenancy Model

The platform uses a **schema-per-tenant** PostgreSQL model. Each tenant (B2B organisation) has an isolated schema. BlueGuardian Co operates its own schema (`blueguardian`) which serves as the tenant for:

- Residential/consumer customers (not associated with any org)
- Unassociated or unowned fleet devices
- Fleet managed on behalf of organisations that have purchased a managed service

### User Tiers

| Tier                  | Description                                        | Scope                            |
|-----------------------|----------------------------------------------------|----------------------------------|
| **Platform Operator** | BlueGuardian Co staff                              | Cross-tenant, privileged         |
| **Tenant Operator**   | B2B org admin/fleet manager                        | Within their tenant schema       |
| **Tenant User**       | End user within a B2B org (driver, field worker)   | Within their tenant schema       |
| **Consumer**          | Residential customer                               | Within the `blueguardian` schema |

### Privileged Access

Platform operators require **step-up authentication** for elevated access, implemented via Keycloak using a token/user impersonation flow that requires explicit end-user approval. Key constraints:

- Audit log entries must record both the acting platform operator and the impersonated user
- Notifications read during an impersonation session must not mark the end user's notifications as read
- The impersonation request and approval events are themselves notification-worthy

---

## Service Architecture

The backend is composed of five services:

### 1. Primary Backend

**Technology:** Quarkus, Hibernate ORM, PostgreSQL (PostGIS)

Responsibilities:

- Exposes all REST API endpoints consumed by the frontend
- Consumes GPS ping and device status messages from AMQ Streams (Kafka) via SmallRye Reactive Messaging (`@Incoming`) and persists them to PostgreSQL
- Serves notification and entitlement data to the frontend — it does not own the generation or sync of these, only the read path

### 2. Anomaly Detection

**Technology:** Python, scikit-learn (Isolation Forest)

Responsibilities:

- Subscribes to the `gps.pings` Kafka topic
- Runs per-tenant, per-device Isolation Forest models to score incoming pings
- Publishes anomaly score events to the `anomaly.scores` topic
- Tenant and model isolation is strictly maintained — no cross-tenant data paths

### 3. Notification Dispatch

**Technology:** TBD

Responsibilities:

- Subscribes to multiple Kafka topics and evaluates trigger conditions
- Writes notification records and per-recipient delivery rows to the appropriate tenant schema
- Fans out push notifications via FCM (Android + Web) and APNs (iOS)
- Manages push token lifecycle — removes stale tokens on FCM/APNs rejection

### 4. Device Onboarding & Registration

**Technology:** TBD

Responsibilities:

- Handles device provisioning, certificate management, and tenant association
- Distinct deployment and scaling lifecycle from real-time tracking components
- Tighter audit controls around registration events

### 5. Billing & Entitlement Sync

**Technology:** TBD

Responsibilities:

- **Shopify is the source of truth** for all subscription and billing data
- Receives Shopify webhooks (order created, subscription updated, payment failed, etc.) with HMAC verification
- Runs a periodic reconciliation job — diffs Shopify state against locally cached entitlement values and flags discrepancies
- Writes canonical entitlement state back to the relevant tenant/user profile, including a `subscription_synced_at` timestamp for staleness diagnostics
- Publishes entitlement change events to the `subscription.updates` Kafka topic
- Does **not** serve entitlement data directly to the frontend — that is the primary backend's responsibility
- Does **not** make billing decisions — it reflects what Shopify says

### Message Bus

All inter-service event flow is via **Eclipse Hono → AMQ Streams (Kafka)**. All device traffic arrives structured and typed; no classification layer is required.

```
Device Fleet
    └── Eclipse Hono
            └── AMQ Streams (Kafka)
                    ├── Primary Backend       (gps.pings, device.status)
                    ├── Anomaly Detection     (gps.pings → anomaly.scores)
                    ├── Notification Dispatch (anomaly.scores, geofence.events, device.lifecycle, subscription.updates)
                    └── Billing Sync          (→ subscription.updates)
```

---

## Notification System

### Data Model

Notifications follow an **event + recipient** pattern: one notification record is created per event occurrence, with separate per-recipient rows tracking delivery and read state.

```sql
CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    category        TEXT NOT NULL,
    severity        TEXT NOT NULL,
    title           TEXT NOT NULL,
    description     TEXT NOT NULL,
    metadata        JSONB,
    target_audience TEXT[] NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notification_recipients (
    id              BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES notifications(id),
    user_id         TEXT NOT NULL,
    role            TEXT NOT NULL,
    read_at         TIMESTAMPTZ,
    push_sent_at    TIMESTAMPTZ,
    push_ack_at     TIMESTAMPTZ
);

CREATE TABLE push_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     TEXT NOT NULL,
    platform    TEXT NOT NULL,
    token       TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen   TIMESTAMPTZ,
    UNIQUE (user_id, token)
);
```

**Design notes:**

- `target_audience` is an array to support notifications targeting multiple tiers simultaneously
- `metadata` JSONB keeps the schema stable as new event types are added
- Cross-tenant platform-level notifications are written to the `blueguardian` schema's notification tables
- Subscription threshold notifications require deduplication

### Notification Event Triggers

| Kafka Topic             | Trigger Condition                                          | Target Audience    |
|-------------------------|------------------------------------------------------------|--------------------|
| `gps.pings`             | No ping received for device within threshold window        | `operator`         |
| `anomaly.scores`        | Score exceeds configured threshold                         | `operator`         |
| `geofence.events`       | Device entry or exit event                                 | `operator`, `user` |
| `device.lifecycle`      | Registration success/failure, device online/offline        | `operator`         |
| `subscription.updates`  | Utilisation ≥ 80%, utilisation = 100%, subscription lapsed | `operator`, `user` |
| Internal                | Impersonation access requested by platform operator        | `user`             |
| Internal                | Impersonation access approved by user                      | `platform`         |

### Push Dispatch Flow

```
Kafka event
    → Dispatch service evaluates trigger condition
    → Deduplication check (subscription/access events)
    → Write to notifications + notification_recipients
    → Query push_tokens for each recipient
    → Fan out: FCM (Android + Web), APNs (iOS)
    → Update push_sent_at
    → On token-invalid response from FCM/APNs → delete stale token
    → On push tap open from client → PUT /notifications/{id}/read (sets push_ack_at)
```

### API Endpoints (Primary Backend)

```
GET    /notifications                  # Paginated notifications for authenticated user, unread-first
GET    /notifications?unread=true      # Unread count for badge display
PUT    /notifications/{id}/read        # Mark a single notification as read
PUT    /notifications/read-all         # Mark all notifications as read

POST   /push-tokens                    # Register a device push token
DELETE /push-tokens/{token}            # Deregister token on logout
```

---

## Subscription & Entitlement Model

- **Shopify** is the authoritative source for all billing and entitlement data
- The database holds a local cache of entitlement state (`subscription_entitlement`, `subscription_used`, `subscription_synced_at`) for fast query and diagnostics
- Platform operators can view both the Shopify-sourced values and the DB-cached values to diagnose discrepancies
- `subscription_synced_at` allows platform operators to immediately identify a stale cache during support investigations
- B2B orgs may have one or more Shopify billing accounts; consumers have a single account — the billing sync service handles both models

---

## Key Design Principles

- **Tenant isolation at every layer** — DB schemas, Kafka consumers, ML models, and notification tables all respect the tenant boundary with no cross-tenant data paths
- **Least-privilege DB roles** — separate roles scoped tightly to their function
- **Offline/air-gapped compatibility** — no external CDN or runtime dependencies for core platform operation
- **Shopify as billing source of truth** — the platform reflects Shopify state, never diverges from it as the authoritative record
- **Audit trail for privileged access** — all impersonation sessions are logged with both the platform operator and end user identity; end-user approval is required before access is granted
- **UTC storage** — all timestamps stored in UTC; timezone conversion is handled at the application layer
