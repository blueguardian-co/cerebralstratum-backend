# ADR-0005: Keycloak Authorization Services — UMA 2.0 Device Resources

| Field | Value |
|---|---|
| **Status** | Proposed |
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
- Whether resource registration happens at device-onboarding time (Device Lifecycle Manager) or lazily on first share request — leaning onboarding-time for consistency, to be confirmed when the Device Lifecycle Manager's onboarding saga is revisited.
- Load/performance characteristics of UMA resource checks at fleet scale — needs validation once device counts are non-trivial.

## Forward Pointers

- Support impersonation / consent flow ADR — consumes `device:share`, adds `support_grants` workflow-state table, Model B mediation, agent bound by individual `bg-internal-sub`.
- Emergency SOS ADR — consumes this substrate, adds `emergency_grants` table (sibling to `support_grants`), ephemeral Keycloak identities provisioned at setup time (not incident time), bypasses subscription entitlement checks, immediate owner revocation.
- Law Enforcement Device Share (backlog) — narrower read-only variant (telemetry + device identity only, no geofence configs), templated on the support impersonation workflow.
