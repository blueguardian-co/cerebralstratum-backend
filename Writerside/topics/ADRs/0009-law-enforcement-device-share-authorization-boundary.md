# ADR-0009: Law Enforcement Device Share — Authorization Boundary

| Field | Value |
|---|---|
| **Status** | Proposed |
| **Date** | 2026-07-21 |
| **Author** | Alex Henshaw |
| **Relates to** | `cerebralstratum-backend` ADR-0005 (UMA 2.0 Device Resources), ADR-0006 (Support Impersonation and Consent Flow), ADR-0007 (Emergency SOS) |

---

## Context

The Law Enforcement Device Share feature allows a device owner to voluntarily generate a read-only, time-limited, scope-restricted share of device telemetry (location and device identity only) for the purpose of asset recovery. The design principles for the feature itself — user-initiated, user-revocable, minimal data exposure, read-only, time-limited, scoped to specific devices, auditable — were established in the accompanying initiative document (`INITIATIVE-accessibility-equity-le-share.md`) and referenced as backlog in ADR-0006 and ADR-0007.

That document states the feature is user-initiated as a *positive* design intent. It does not state, as a *negative constraint*, that no other actor — including BlueGuardian Co itself — has any technical path to originate a share. Those are architecturally distinct claims. A policy statement that the feature "is" user-initiated does not, by itself, prevent an operator role, an admin API, or a future feature from creating a share on a user's behalf under some other justification (e.g. an internal support workflow, an "expedited recovery" shortcut, or informal accommodation of a law enforcement request).

This ADR closes that gap: it establishes an explicit, technically enforced boundary stating who may originate an LE share, and separately draws a hard line between this feature and the entirely distinct question of legally compelled disclosure (subpoena, warrant, court order).

## Decision

**1. Only the device owner may originate an LE share.**

LE shares are modeled as UMA 2.0 resource permissions on the existing `device-fleet` authorization substrate (ADR-0005), following the same pattern established for `support_grants` (ADR-0006) and `emergency_grants` (ADR-0007). The UMA policy backing an LE share resource permission MUST require the resource owner's own authenticated subject as the policy creator.

Concretely:
- No `realm-management` client role, no operator/agent role established under the admin-plane authorization ADR, and no service-account-backed identity (including `bg-internal-sub`) has permission to create the underlying UMA resource permission.
- This is enforced at the authorization layer (policy evaluation rejects non-owner subjects), not only at the API layer, consistent with the "server-side authority, no bypass" principle applied to SOS grant revocation (ADR-0007) and support impersonation consent (ADR-0006).
- At the API layer, no endpoint accepts an operator or agent identity as the creator of an LE share. This is a structural omission, not a permission check that could drift out of configuration — there is no code path for it to exist.

**2. No in-application path exists for legally compelled disclosure, at this time.**

If BlueGuardian Co receives a subpoena, warrant, or other legal process compelling production of device data, there is currently **no mechanism within CEREBRAL STRATUM** — no admin console action, no break-glass role, no elevated API scope — to fulfil that request. Compelled disclosure, if and when it needs to be supported, will be handled entirely outside the application layer (e.g. direct, counsel-supervised access to the regional data store, following a legal process that does not yet exist and is out of scope for this ADR). BlueGuardian Co does not currently have a documented legal-request procedure at all; one is expected to be defined prior to go-live.

This is a deliberate decision not to build a break-glass capability ahead of having an actual legal process, counsel involvement, or audit requirements defined for one. Building such a path prematurely risks either being wrong for the eventual legal requirements or becoming a de facto operational shortcut that erodes the owner-only guarantee in (1).

## Alternatives Considered

**A. Admin/support-role-mediated share creation on the owner's behalf** (e.g. an agent creates a share "for" a user who calls in asking for help). Rejected — reintroduces exactly the over-grant pattern already rejected for support impersonation in ADR-0006 (native Keycloak impersonation is admin-initiated and consent-free, identified as wrong for a location-tracking platform). If a user needs help generating a share, the correct pattern is a thin, owner-authenticated UI/flow — not an operator acting as the user.

**B. Break-glass admin path for compelled disclosure, with mandatory counsel sign-off and heavy audit logging.** Considered as a middle ground. Rejected for now — no legal process, approval chain, or jurisdictional analysis (AU Privacy Act / GDPR, given AU/NZ primary + EU secondary markets) exists yet to define what "sign-off" even means. Designing the technical control before the legal process risks building the wrong thing. Deferred to a future ADR once that process is defined.

**C. Allow BlueGuardian to proactively initiate a share without any request** (e.g. platform-detected theft pattern triggers an automatic LE notification). Rejected outright — inconsistent with server-side authority applying restrictions in the user's favor, not surveillance in the platform's favor, and outside the stated purpose of the feature (asset recovery assistance, not proactive monitoring).

## Consequences

- Law enforcement requesting device data informally (outside a legal process) can only obtain it if the device owner chooses to generate a share. BlueGuardian has no mechanism to accommodate such a request even if it wanted to — this is the intended outcome, not a gap.
- Legally compelled requests will require a manual, out-of-band process involving direct regional data store access under legal supervision. This is operationally heavier than an in-app path, accepted as the cost of not having a half-designed break-glass mechanism sitting in production.
- Support engineers cannot "help" a user create an LE share on the user's behalf via the support impersonation workflow (ADR-0006) — any assistance must route the user through owner-authenticated self-service.
- This decision needs to be revisited once real legal process requirements are known; the current position is explicitly a placeholder of "no path" rather than a permanent architectural stance.

## Open Items

- Legal compulsion / compelled disclosure process is entirely undefined — BlueGuardian Co has no documented legal-request procedure at present. Expected to exist prior to go-live; tracked as a future ADR.
- Maximum share duration (platform-defined ceiling on owner-set expiry) — TBD, carried over from the initiative document.
- Jurisdictional variation in compelled-disclosure obligations across AU/NZ/US/EU is not yet analyzed and will affect the design of the future compulsion-process ADR.

## Forward Pointers

- Future ADR: Legal Compulsion / Compelled Disclosure Process — will define the out-of-band process referenced in Decision (2). No target date; contingent on BlueGuardian Co's legal-request procedure being defined pre-go-live.
- ADR-0005 — UMA 2.0 resource share substrate this feature builds on.
- Admin-plane / operator authorization ADR (`cerebralstratum` project) — confirms no operator role introduced there is granted LE-share creation capability.
