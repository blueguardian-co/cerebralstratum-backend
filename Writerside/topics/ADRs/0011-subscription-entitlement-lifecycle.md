# ADR-0011: Subscription / Entitlement Lifecycle

| Field              | Value                                                                                                    |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Status**         | Proposed                                                                                                  |
| **Date**           | 2026-07-23                                                                                                |
| **Author**         | Alex Henshaw                                                                                              |
| **Migrated from**  | [CSPROD-A-33](https://youtrack.blueguardian.co/articles/CSPROD-A-33)                                     |
| **Relates to**     | `cerebralstratum` ADR-0005 (Data Classification and Control Plane Residency); `cerebralstratum-backend` ADR-0007 (Emergency SOS) |
| **Forward pointers** | `ENTITLEMENT_MODE` env var doc/ADR (community mode purge-responsibility handoff); possible future entry for Law Enforcement Device Share if it ever needs an entitlement carve-out |

---

## Context

The subscription/entitlement lifecycle has been referenced across the Backend Architecture doc, `cerebralstratum` ADR-0005 (Data Classification and Control Plane Residency), and `cerebralstratum-backend` ADR-0007 (Emergency SOS), but has never been formally documented as its own decision record. This creates two problems: (1) there is no single canonical source for how entitlement state transitions and enforcement actually work, and (2) exceptions to entitlement enforcement — like the SOS bypass — are justified ad hoc in the feature ADR that needs the exception, rather than in a place designed to hold the general policy.

Shopify Advanced is the authoritative source of subscription state. The backend caches this state locally for low-latency enforcement decisions, which introduces a staleness problem that needs an explicit answer.

Separately, the platform's data sovereignty principle (`cerebralstratum` ADR-0005) requires that purge jobs execute at the regional instance level — this ADR needs to state how the entitlement lifecycle triggers that regional purge without violating the control-plane-as-routing-only boundary.

## Decision

**Four-state lifecycle**, tracked per tenant subscription:

```
SUBSCRIPTION_ACTIVE
      │
      ▼ (payment failure)
SUBSCRIPTION_PAST_DUE
      │
      ├──▶ SUBSCRIPTION_LAPSED      (grace period expires without resolution)
      └──▶ SUBSCRIPTION_CANCELLED  (explicit cancellation, via Shopify)
      │
      ▼ (7-day grace period elapses)
   [Purged]  — regional-instance-level data purge
      │
      ▼ (account deletion request)
   [Account Deleted]
```

**Staleness tracking:** every cached entitlement record carries a `subscription_synced_at` timestamp — the mechanism for detecting cache staleness against Shopify as source of truth, not a webhook-guaranteed real-time mirror. Enforcement logic must treat a stale-beyond-threshold cache as a signal to re-sync before making a hard denial, never as silent pass-through.

**Enforcement layer:** entitlement checks are enforced at the API layer, mandatorily — not optional or client-supplementary (server-side authority principle). Critically: **the backend continues ingesting telemetry from devices under `SUBSCRIPTION_LAPSED` or `SUBSCRIPTION_CANCELLED` states.** This preserves data integrity and avoids silent data loss during the grace window — what's restricted is *access* (dashboards, alerting, API reads), not *ingestion*. Ingestion and access-entitlement are deliberately decoupled.

**Purge execution:** at the 7-day grace period boundary, purge jobs execute within the regional instance holding that tenant's data (per `cerebralstratum` ADR-0005's control-plane-boundary decision). The control plane never performs the purge itself — it may schedule/trigger, but the regional instance is what deletes.

**Named exceptions to enforcement:** this ADR is the canonical location for entitlement-bypass exceptions. The pattern is: any feature that must function regardless of subscription state needs an explicit, named entry here, not a bespoke justification embedded in that feature's own ADR.

| Exception | Scope of bypass | Rationale | Defined in |
|---|---|---|---|
| Emergency SOS | Bypasses entitlement checks entirely — full functionality regardless of subscription state | Life-safety feature; a lapsed payment must never gate an SOS trigger | `cerebralstratum-backend` ADR-0007 (Emergency SOS) — cross-linked here as canonical justification |
| Priority Notifications | Bypasses entitlement checks for high-priority delivery (SSE/gRPC + FCM/APNs) only; standard-priority notifications remain subject to normal entitlement gating at the read/delivery API | Time-sensitive alerts must reach the user regardless of subscription state; routine billing/lifecycle communication (renewal reminders, lapse warnings) is handled separately via Shopify email, not the in-platform notification system | `cerebralstratum-backend` ADR-0012 (Notification Delivery Transport) — cross-linked here as canonical justification |

Future named exceptions should be added as new rows to this table, with the implementing ADR cross-linking back here rather than re-deriving the justification independently.

## Alternatives Considered

- **Real-time Shopify webhook sync as sole source of truth (no local cache).** Rejected — eliminates staleness but introduces a hard external dependency into the request path for every entitlement check, violating the platform's general bias against Shopify/Brightpearl on the critical path.
- **Client-side entitlement enforcement (trust client state).** Rejected outright — directly violates the server-side authority principle; not seriously considered.
- **Immediate hard-cutoff on lapse (no grace period, no continued ingestion).** Rejected — risks silent telemetry loss for tenants mid-payment-resolution (e.g. card re-auth in progress); worse support/goodwill posture for a life-safety-adjacent product. Rejected in favor of grace-period + continued-ingestion.
- **Per-feature bypass justification (status quo before this ADR).** Rejected — exactly the problem this ADR exists to fix: bypass logic scattered across feature ADRs makes it hard to audit which features bypass entitlement and why.

## Consequences

### Positive

- Easier auditing of which features bypass entitlement (single table, single ADR).
- Easier reasoning about tenant data lifecycle end-to-end.
- Easier onboarding of a new engineer to "how does billing state affect the platform."

### Trade-offs & limitations

- Any new bypass exception now requires touching two documents (this ADR's table, plus the feature ADR) rather than one — deliberate friction, not a bug.
- **To revisit:** if the exceptions table grows past a handful of entries, consider whether entitlement bypass should become a first-class enum/flag in the entitlement service rather than a documentation convention.

## Open Items

- Confirm exact `subscription_synced_at` staleness threshold that triggers a forced re-sync (not yet specified numerically).
- Confirm whether `SUBSCRIPTION_PAST_DUE` restricts any access before `LAPSED`/`CANCELLED`, or whether restriction only begins at those two terminal-before-purge states.
- Decide whether Law Enforcement Device Share (`cerebralstratum-backend` ADR-0009) should be a future entry in the exceptions table — it's read-only and narrower in scope than SOS, so it may not need a bypass at all (entitlement-gated is plausibly correct for it). Flag for that feature's own ADR to decide, cross-linking here only if it does need one.
- `ENTITLEMENT_MODE=community` removes the billing subsystem entirely, which changes who is responsible for purge enforcement (becomes the operator's responsibility). Needs an explicit note in whichever doc formalises `ENTITLEMENT_MODE` that this ADR's purge-trigger logic assumes `managed` mode.

## Forward Pointers

- `ENTITLEMENT_MODE` env var doc/ADR — community mode purge-responsibility handoff.
- Possible future entry for Law Enforcement Device Share (`cerebralstratum-backend` ADR-0009) if it ever needs an entitlement carve-out.
- Implementation tracked in [CSPROD-170](https://youtrack.blueguardian.co/issue/CSPROD-170) (Billing & Entitlement Sync service).