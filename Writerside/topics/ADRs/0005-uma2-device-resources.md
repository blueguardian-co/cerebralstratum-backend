# ADR-0005: Keycloak Authorization Services — UMA 2.0 Device Resources

| Field | Value |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-07-20 |
| **Author** | Alex Henshaw |
| **Relates to** | `cerebralstratum` ADR-0003 (Identity Infrastructure Placement), `cerebralstratum-backend` ADR-0001 (IAM & Device Registration) |

---

## Context

Every device in the fleet is owned by a tenant user, but access to a device's live location and telemetry sometimes needs to be shared beyond the owner — with support agents during a diagnostic session, with emergency contacts during an SOS event, and in future with law enforcement under a time-limited read-only grant. Each of these is a distinct *consent grant*, not a standing administrative privilege, and each must be independently and immediately revocable by the device owner.

Keycloak offers two distinct authorization mechanisms that are easy to conflate:

- **`realm-management` client roles** (e.g. `manage-users`, `impersonation`) — coarse, admin-plane privileges. `manage-users` in particular bundles capabilities (password reset, account takeover) that are far broader than "let this one agent see this one device for the next hour."
- **Authorization Services / UMA 2.0** — a resource-and-scope model where a resource owner (or the system on their behalf) grants specific scopes on a specific resource to a specific requesting party. This is fine-grained by design and maps naturally onto "this device, this scope, this grantee, this time window."

Native Keycloak user impersonation is admin-initiated and consent-free: an administrator can step into a user's session without the user granting anything. That is the wrong shape for a location-tracking platform — every access path to device data needs to originate from an explicit, auditable grant, not from platform-operator discretion.

FGAP V2 (Fine-Grained Admin Permissions v2), confirmed as default on RHBK 26.6.4, is the mechanism that bridges the two models operationally — it lets admin-plane operations themselves be scoped via permissions rather than blanket realm-management roles, but it does not replace UMA as the *consent* substrate for device access. The two remain conceptually separate: FGAP scopes what operators can do administratively; UMA resource shares scope what a device owner has consented to share with a specific party.

## Decision

Adopt **UMA 2.0 resource shares on the `device-fleet` confidential client** as the authoritative consent substrate for all non-owner access to device data.

- Each device is registered as a UMA resource under `device-fleet`.
- Scopes are defined per capability needed by downstream features, starting with `device:share` (the general "grant read access to this device" scope). Additional scopes are added as new sharing features require them, rather than overloading `device:share` with feature-specific meaning.
- **Existence of a resource permission ticket/grant equals access.** There is no secondary "is this grant still active" check maintained outside Keycloak — the UMA grant itself is the source of truth.
- **Revocation is native and immediate.** The device owner revoking a resource share removes access with no ride-out period. This is non-negotiable for safety-critical grants (SOS, owner-initiated revocation of support access).
- The `device-fleet` client remains the single confidential client for the fleet (per backend ADR-0001) — UMA resources multiply per-device under this one client, not via per-device client proliferation.
- All device access mediated through UMA grants is **backend-mediated** (Model B): the party with a grant does not receive a direct Keycloak RPT to use against downstream services from their own session. Instead, the backend checks for a valid grant and mediates the actual device data access itself. This keeps a single enforcement point and keeps token contents (and therefore what crosses region boundaries) minimal — lean tokens are both a scalability control and a data-sovereignty control, since token contents that cross regions are a hidden transit-layer sovereignty risk.
- Native Keycloak impersonation and `manage-users` are **not used** for any device-access-sharing use case. Where a genuinely admin-plane action is needed (e.g. resetting a locked-out user's credentials), that goes through FGAP V2-scoped permissions and a thin service-account-backed internal UI — never a bundled `manage-users` grant to a human operator.

## Alternatives Considered

- **Native Keycloak impersonation for support access.** Rejected — admin-initiated and consent-free; no per-grant revocation; no natural time-boxing; wrong audit shape for a platform whose entire value proposition is "you control who sees your location."
- **`manage-users` client role for support agents.** Rejected — massively over-broad (password reset, full account takeover capability) for what is actually needed (read-only device telemetry for the duration of a support session).
- **Custom grant table with no Keycloak involvement (pure application-layer ACL).** Rejected — would duplicate what UMA already provides, and would require BlueGuardian to reimplement token issuance, expiry, and revocation semantics that Keycloak already handles correctly. Also loses the "existence equals access" simplicity — a parallel ACL table becomes a second source of truth that can drift from the actual grant state.
- **Per-device Keycloak clients instead of per-device UMA resources under one client.** Rejected — client proliferation was already ruled out in backend ADR-0001; UMA resources give per-device granularity without multiplying clients.

## Consequences

- All future sharing features (support impersonation, SOS, law enforcement share) build on this same substrate rather than inventing their own grant/revocation mechanism — consistent audit story across all of them.
- Safety-critical revocation (owner revokes, SOS resolves) is immediate by construction, since revocation is native to the UMA grant rather than a polled or cached state.
- Backend-mediated access (Model B) adds a hop compared to letting the grantee call downstream services directly with their own RPT, but this is accepted in exchange for single-point enforcement and lean cross-region tokens.
- Engineering cost: this ADR establishes the substrate but each consuming feature (support, SOS, law enforcement) still needs its own workflow-state table (e.g. `support_grants`, `emergency_grants`) to track time-boxing and workflow status *around* the UMA grant — UMA gives the authorization primitive, not the full workflow.

## Open Items

- Exact enumeration of scopes beyond `device:share` — to be defined as each consuming feature (support, SOS, law enforcement share) is designed, rather than speculatively upfront.
- ~~Whether resource registration happens at device-onboarding time (Device Lifecycle Manager) or lazily on first share request~~ — **Resolved 2026-07-26, see Amendment below.**
- Load/performance characteristics of UMA resource checks at fleet scale — needs validation once device counts are non-trivial.

## Amendment (2026-07-26)

Implementation planning surfaced three points this ADR left implicit. All three are now decided:

**1. Client naming reconciled.** This ADR (and ADR-0006, ADR-0009) refer to `device-fleet`, but the actual Keycloak client already carrying `authorizationServicesEnabled: true` and a placeholder resource is named `cerebral-stratum-backend`. There is no separate `device-fleet` client — `cerebral-stratum-backend` is being renamed to `device-fleet` to match this ADR family, rather than the ADRs being renamed to match the client. This is a pure rename (clientId, service-account username, client-role container, `quarkus.oidc.client-id`) with no functional change.

**2. Scope widened to cover owner-tier access, not just non-owner shares.** As written, this ADR scoped UMA strictly to "non-owner access" (support/SOS/LE), implying owner access would keep using some other mechanism. On review, the *current* mechanism for owner-tier device access is a Keycloak group-per-device hierarchy (`/{device_uuid}/modify`, `/{device_uuid}/view-only`) — exactly the design ADR-0001 already rejected in favor of Authorization Services, but never actually migrated. Decision: this UMA substrate now covers **both** tiers on the same resource —
   - `device:read` / `device:modify` (per ADR-0001's original scope naming) for owner-tier access, replacing the group hierarchy entirely.
   - `device:share` (this ADR's original scope) reserved for the non-owner consent-grant features (ADR-0006, ADR-0009, and Emergency SOS) — no policies attached to it yet, since those features aren't built.

   Practically: each device resource gets `ownerManagedAccess: true` plus a user-based policy for the owner, applied directly (no aggregation) to a `device:read`+`device:modify` permission. `device:share` support/SOS/LE grants layer their own permissions onto the same resource later, per those ADRs.
   - `ownerManagedAccess: true` is also what makes ADR-0009's "only the device owner may originate a share" enforceable natively by Keycloak, rather than needing a hand-rolled application-layer check.
   - **No standing platform-admin (or any other operator) policy is attached to `device:read`/`device:modify`.** BlueGuardian's platform team has a firm zero-standing-operator-access posture on customer device data — the `/platform-admins` group and `admins` realm role remain valid for platform-plane configuration/maintenance/debugging (organisation-admin and user-admin checks, fleet-lifecycle endpoints, realm/client management), but confer **no** implicit device data access. This was corrected on first implementation: the initial pass ported the old group-per-device model's `isADeviceAdmin` platform-admin bypass into a Keycloak group-based policy aggregated onto every device's permission, which reintroduced exactly the standing administrative privilege this ADR's own Context section (see "not from platform-operator discretion") rules out. If a platform operator genuinely needs to see a specific device (e.g. a support investigation), that must go through the same consent-grant substrate as everyone else — `device:share` via the support-impersonation workflow (ADR-0006) — not a standing group grant.

**3. Resource registration timing confirmed as onboarding-time**, resolving the Open Item above: a UMA resource is created (no owner) at device platform-registration (`DevicesResource.create`, ADR-0001 phase 1), and the owner policy/permission is attached at user-association (`DevicesResource.registerDevice`, ADR-0001 phase 2) — not lazily on first share request.

**Implementation note:** Model B (no RPT to the caller) means the backend evaluates permission on the caller's behalf using the UMA-ticket grant (resource server credentials + the caller's own access token as `subject_token`), not Quarkus's automatic Policy Enforcer path-matching (Model A). Device resources must therefore be registered *without* a `uris` match — an automatic Model A path match on the same resource would conflict with the explicit Model B check this ADR calls for.

## Forward Pointers

- Support impersonation / consent flow ADR — consumes `device:share`, adds `support_grants` workflow-state table, Model B mediation, agent bound by individual `bg-internal-sub`.
- Emergency SOS ADR — consumes this substrate, adds `emergency_grants` table (sibling to `support_grants`), ephemeral Keycloak identities provisioned at setup time (not incident time), bypasses subscription entitlement checks, immediate owner revocation.
- Law Enforcement Device Share (backlog) — narrower read-only variant (telemetry + device identity only, no geofence configs), templated on the support impersonation workflow.
