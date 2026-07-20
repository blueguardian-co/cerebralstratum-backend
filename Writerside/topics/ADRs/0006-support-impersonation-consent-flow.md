# ADR-0006: Support Impersonation and Consent Flow

| Field | Value |
|---|---|
| **Status** | Proposed |
| **Date** | 2026-07-20 |
| **Author** | Alex Henshaw |
| **Relates to** | `cerebralstratum-backend` ADR-0005 (UMA 2.0 Device Resources) |

---

## Context

Support agents occasionally need to see a customer's device data to diagnose an issue — a device that won't report location, a geofence that isn't firing, telemetry that looks anomalous. The platform needs a way for an agent to get that visibility without it looking or behaving like the platform operator has standing, consent-free access to any customer's location data at any time. That distinction is not cosmetic: it is the core trust proposition of an asset-*protection* platform, and it also needs to survive scrutiny from a future operator hire who is not Alex.

ADR-0005 established UMA 2.0 resource shares on `device-fleet` as the authoritative consent substrate, with backend-mediated access (Model B) rather than direct RPTs to grantees. This ADR applies that substrate to the specific support workflow: what triggers a grant, who can request one, how long it lasts, and how the agent's own identity is bound to their actions.

## Decision

**Consent substrate:** A support session is backed by a scoped UMA resource share on the specific device(s) in question, using the `device:share` scope defined in ADR-0005. No standing role grants an agent visibility into arbitrary devices — every session is a fresh, scoped grant tied to a specific device and a specific agent.

**Workflow state:** A `support_grants` table (sibling in shape to the later `emergency_grants` table) tracks the operational state of the workflow around the UMA grant — request initiated, customer consent given (or not required, per below), grant active, time-boxed expiry, and closure/revocation. UMA is the authorization primitive; `support_grants` is the workflow ledger that makes that primitive auditable and manageable at the product layer (what triggered it, who approved it, when it should auto-expire).

**Consent model:** The default and preferred path is customer-initiated or customer-confirmed: the customer requests support, and either explicitly approves the session (e.g. via a prompt in-app) or the request itself constitutes implied consent for a time-boxed window (exact UX to be finalized — see Open Items). What is *not* in scope is an agent unilaterally opening a session on a device without any customer-side trigger.

**Access mediation — Model B, no RPT to agent:** The agent never receives a Keycloak-issued RPT that they use to call downstream services (Primary Backend, Anomaly Detection, etc.) directly. Instead:
- The agent's own identity is `bg-internal-sub` (or equivalent per-individual internal subject) — every action is attributable to a named human, never a shared/service account.
- The agent interacts with a thin, service-account-backed support UI/API surface.
- That surface checks for an active `support_grants` entry + corresponding UMA permission before returning any device data, and itself calls downstream services using its own service credentials — the agent's session never touches device services directly.
- This keeps a single enforcement and audit point (the support UI/API layer) rather than distributing enforcement across every downstream service the agent might otherwise call.

**Time-boxing and expiry:** Grants expire automatically; there is no indefinite support session. Expiry is enforced by the `support_grants` workflow layer, and the underlying UMA permission is revoked (not just soft-expired) at that point.

**Revocation:** The device owner can revoke a support grant at any time, with immediate effect — consistent with ADR-0005's "no ride-out period" principle for safety- and trust-critical grants.

## Alternatives Considered

- **Native Keycloak impersonation for support.** Rejected in ADR-0005 already, restated here as the thing this ADR is explicitly designed to avoid: no consent trigger, no natural time-boxing, wrong audit shape.
- **Direct RPT issuance to the agent (Model A).** Rejected — would mean the agent's own Keycloak session carries a token valid against downstream device services, multiplying the number of places a leaked or over-long-lived token could be misused, and fragmenting enforcement across every service the agent might call instead of one mediating layer.
- **Shared "support" service account with broad device visibility, audited via logging only.** Rejected — this reintroduces exactly the over-grant problem ADR-0005 was written to avoid; a shared account also breaks the "bound to an individual agent" attribution requirement.
- **No customer consent trigger at all (silent support access).** Rejected on trust/product-positioning grounds — undermines the platform's core value proposition even if technically convenient.

## Consequences

- Every support session is individually attributable, time-boxed, and immediately revocable — matches the platform's trust positioning.
- Adds a small amount of latency/friction to the support workflow (a session must be explicitly opened and will expire), which is an accepted tradeoff for auditability.
- `support_grants` becomes a template other grant-workflow features can copy (see Forward Pointers) — the pattern, not just the table, is reusable.
- Support tooling must be built as a distinct, thin service-account-backed surface rather than agents being handed generic API access — this is additional upfront engineering scope but avoids much larger scope creep later if broad access were granted informally.

## Open Items

- Finalize the exact consent UX: does the customer need to explicitly tap "approve" per session, or is a support-ticket-initiated request sufficient implied consent for a bounded window? Needs product input, not just architecture.
- Default and maximum time-box duration for a support grant — needs a concrete number before implementation.
- What happens if a support session is active when the customer revokes — does the in-progress session hard-stop mid-request, or complete the in-flight request and then block further access? Leaning hard-stop for consistency with the "immediate" principle in ADR-0005, to be confirmed.
- Whether `support_grants` lives in the Primary Backend's schema or a dedicated schema shared only with the support UI service.

## Forward Pointers

- Emergency SOS ADR — reuses this same grant/mediation pattern via a sibling `emergency_grants` table, but differs materially: bypasses entitlement checks, provisions ephemeral identities at setup time rather than incident time, and has no consent-gating step (the person in the contact list receiving an SOS alert doesn't need to "approve" seeing it).
- Law Enforcement Device Share (backlog) — narrower, read-only variant of this same workflow, restricted to telemetry + device identity only (no geofence configs, no personal context).
