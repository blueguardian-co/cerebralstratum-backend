# ADR-0007: Emergency SOS

| Field | Value |
|---|---|
| **Status** | Proposed |
| **Date** | 2026-07-20 |
| **Author** | Alex Henshaw |
| **Relates to** | `cerebralstratum-backend` ADR-0005 (UMA 2.0 Device Resources), ADR-0006 (Support Impersonation and Consent Flow) |

---

## Context

Emergency SOS lets a device owner (or, depending on hardware trigger design, the device itself) alert a pre-configured set of emergency contacts with location and device status during a genuine emergency. Unlike support impersonation (ADR-0006), this feature has three properties that break the assumptions of that earlier design:

1. **No time to gate on consent.** In an emergency, there is no window for a contact to "approve" receiving an alert — the whole point is that they see it immediately.
2. **Contacts may not have platform accounts.** A spouse, neighbour, or emergency responder listed as a contact is very likely not a registered CEREBRAL STRATUM user at the moment the SOS fires.
3. **Billing state must not gate safety.** A device owner whose subscription has lapsed or been cancelled must not lose SOS functionality — this is a life-safety feature, not a paid convenience, and entitlement enforcement (backend ADR — subscription/entitlement lifecycle) must have an explicit carve-out for it.

The design reuses the grant/mediation substrate from ADR-0005 and the workflow-table pattern from ADR-0006, but diverges deliberately on consent-gating, identity provisioning timing, and entitlement interaction.

## Decision

**Trigger:** SOS is device-triggered (physical trigger on the hardware or an in-app panic action) and always backend-mediated — the device or client never contacts SendGrid/Twilio or emergency contacts directly. The device/client notifies the backend; the backend's Notification Dispatch service owns all outbound communication. This is consistent with the platform-wide principle that notification logic never lives in the client.

**Identity provisioning — ephemeral, at setup time, not incident time:** When a device owner configures emergency contacts, the backend provisions an ephemeral Keycloak identity for each contact *at that time*, not when an incident occurs. This means:
- No identity-provisioning latency sits on the critical path of an actual emergency.
- A contact who has never logged into anything still has a resolvable identity the authorization model can reference when the SOS grant is created.
- If a listed contact already has a full platform account, that existing identity is used instead of a new ephemeral one.

**Grant mechanism:** An SOS event creates an entry in an `emergency_grants` table — structurally a sibling to `support_grants` (ADR-0006) — referencing the underlying UMA resource share (per ADR-0005) that gives each contact's identity `device:share` scope on the specific device for the duration of the incident.

**No consent gate:** Unlike support sessions, there is no approval step before a contact's grant becomes active — the grant is created and notifications dispatched in the same flow. The "consent" already happened when the owner configured that person as an emergency contact.

**Notification dispatch:** Notification Dispatch sends alerts via SendGrid (email) and Twilio (SMS) to each configured contact, containing location and essential device status. Dispatch and grant creation happen together — a contact isn't just informed something happened, they're simultaneously given the access needed to see live location if they follow through (e.g. a link into a scoped view).

**Entitlement bypass:** SOS functionality explicitly bypasses subscription entitlement checks. A device with `SUBSCRIPTION_LAPSED` or `SUBSCRIPTION_CANCELLED` state still triggers SOS normally. This requires an explicit carve-out in the API-layer entitlement enforcement path (rather than a blanket "entitlement always gates access" rule) — SOS is one of the few flows allowed to bypass it, and this needs to be a named, reviewed exception rather than an incidental gap.

**Revocation:** The device owner can revoke emergency access immediately, same as any UMA-backed grant (ADR-0005). In practice this matters most for closing out access after an incident resolves, or for removing a contact who should no longer have standing ability to be granted access in future incidents.

## Alternatives Considered

- **Consent-gated grants, same as support impersonation.** Rejected — an emergency alert that requires the recipient to approve before receiving it defeats the purpose of the feature.
- **Provision contact identities at incident time.** Rejected — adds identity-creation latency to the exact moment speed matters most, and risks partial failure (alert sent, but access grant not yet resolvable) during the highest-stakes path in the whole platform.
- **Device/client sends alerts directly to SendGrid/Twilio, bypassing the backend.** Rejected — violates the non-negotiable principle that notification logic never lives in the client; also would mean contact lists and credentials would need to be present on-device, which is an unnecessary attack surface and inconsistent with server-side authority.
- **Let entitlement checks apply uniformly, including to SOS.** Rejected outright — a lapsed payment must never be able to disable a safety feature; this was treated as a non-negotiable product/ethical constraint, not a cost-optimization to weigh.
- **Reuse `support_grants` table directly instead of a sibling `emergency_grants` table.** Rejected — the workflows have different lifecycles (no approval state, no agent identity binding, different trigger source) and conflating them would make both harder to reason about and audit independently.

## Consequences

- SOS is fast and doesn't depend on contact account status, at the cost of upfront complexity in provisioning ephemeral identities during emergency-contact setup rather than lazily.
- The entitlement bypass is a deliberately named exception, which means it must be kept visible and tested specifically — an entitlement refactor elsewhere in the system could accidentally reintroduce a gate here if this exception isn't explicitly covered by tests tied to this ADR.
- `emergency_grants` and `support_grants` share a structural pattern but are independently maintained — some duplication in schema/workflow code is accepted in exchange for independent evolution (e.g. SOS never needs an "agent identity" column; support never needs an "ephemeral contact identity" column).
- Notification Dispatch becomes the single owner of a life-safety-critical code path — its reliability and on-call posture need to reflect that (this pushes weight toward hardening Notification Dispatch specifically, beyond its existing role for routine alerts).

## Open Items

- Ephemeral contact identity lifecycle: does it ever expire or get cleaned up if a contact is removed, or does it persist indefinitely at low cost? Needs a decision before implementation to avoid unbounded Keycloak realm growth.
- Exact SendGrid/Twilio failover behavior if one channel fails — does Notification Dispatch retry, escalate to the other channel only, or both simultaneously by default?
- Scope of "essential device status" for the SOS payload — needs a concrete field list, consistent with the NTN payload-discipline principle (no CAN bus data) even though SOS will typically fire over standard connectivity, not NTN.
- Whether SOS events themselves need their own audit/notification to the account owner after the fact (e.g. "an SOS was triggered and contacts were notified") separate from the contacts' own alerts.

## Forward Pointers

- Law Enforcement Device Share (backlog) — a further sharing variant, but time-limited and user-initiated rather than emergency-triggered, restricted to telemetry + device identity only. Templated on the support impersonation workflow (ADR-0006) rather than this one, since it does have a consent/initiation step.
