# ADR-0012: Notification Delivery Transport — Kafka Hand-off to Backend, FCM/APNs as a Separate Concern

| Field              | Value                                                                                                    |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Status**         | Proposed                                                                                                  |
| **Date**           | 2026-07-23                                                                                                |
| **Author**         | Alex Henshaw                                                                                              |
| **Migrated from**  | [CSPROD-A-34](https://youtrack.blueguardian.co/articles/CSPROD-A-34)                                     |
| **Relates to**     | ADR-0010 (Real-Time Device Data Transport); ADR-0011 (Subscription/Entitlement Lifecycle); CSPROD-165 (notification-dispatcher bootstrap); CSPROD-88 (Platform notification endpoint) |

---

## Context

CSPROD-165 bootstrapped `notification-dispatcher`'s Kafka → Redis dedupe → Postgres pipeline: device telemetry events become rows in a `notifications` table, which clients can read (and manage read/unread state for) via `backend`'s REST API. That's sufficient for low-urgency, informational notifications, but two delivery mechanisms are still unaddressed:

1. **Real-time in-app delivery** — pushing a notification to an already-connected client immediately, rather than waiting for it to poll/re-fetch.
2. **OS-level push (FCM/APNs)** — reaching a backgrounded or force-quit mobile app, which polling and in-app real-time transports cannot do.

`backend` already has an established real-time transport precedent from ADR-0010: `DeviceEventBroadcaster` (Kafka in, `BroadcastProcessor` fan-out) feeding both `DeviceServerSentEvents` (SSE, web) and `DeviceStreamService` (gRPC, reserved for mobile/KMP). It also owns the only client-facing OIDC/Keycloak auth and connection-management surface today. `notification-dispatcher` is a separate process (own port, own pom) with no client-facing HTTP/gRPC surface and no auth wiring — it has only ever been a Kafka consumer.

The question this ADR answers: does `notification-dispatcher` grow its own client-facing real-time surface, or hand off to `backend`'s existing one? And separately: how do FCM/APNs push and "priority" notifications fit in?

## Decision

**1. Real-time in-app delivery hands off to `backend`, it is not built into `notification-dispatcher`.** After `notification-dispatcher` persists a notification, it publishes a `notification.created` Kafka event (key: recipient user/device UUID; value: notification id, type, priority, summary). `backend` consumes this via a new `NotificationEventBroadcaster`, following the exact `DeviceEventBroadcaster` pattern, fanned out to the SSE/gRPC surfaces clients already connect to. One client connection surface, one auth/entitlement enforcement point — not duplicated in a second service for one hop of latency saved.

**2. FCM/APNs push is a separate mechanism, dispatched directly from `notification-dispatcher`, using a generic wake-signal payload.** It's an outbound call to an external provider, not an inbound client connection, so it doesn't need `backend`'s auth surface — it needs provider credentials and a push-token registry instead. The payload sent to FCM/APNs carries no location, device identity, or notification content — only a generic prompt ("You have a new alert — open TrueWard to view it"). This is not a fallback pending region-specific provider routing: FCM and APNs are each single global endpoints with no native per-region pinning equivalent to Skylo/emnify's carrier-level routing, so the generic payload is the sovereignty control itself, not a stopgap awaiting regional infrastructure.

**3. Priority is data, not a second code path.** The `notification.created` event carries a `priority` field. High-priority notifications get both the SSE/gRPC push (if connected) and FCM/APNs (unconditionally, regardless of connection state); standard-priority notifications get the SSE/gRPC push only, relying on the existing Postgres-backed read/unread state for anything missed.

**4. Entitlement bypass is scoped to high-priority notifications only.** A lapsed or cancelled subscription does not block priority-notification delivery (SSE/gRPC + FCM/APNs) — this is a named exception per ADR-0011, cross-referenced below. Standard-priority notifications remain subject to normal entitlement gating at the read/delivery API, consistent with all other API reads. Subscription lifecycle communication (renewal reminders, lapse/cancellation warnings) is handled entirely by Shopify via email — it is out of scope for the in-platform notification system and does not flow through `notification-dispatcher`.

## Alternatives Considered

- **`notification-dispatcher` hosts its own SSE/gRPC endpoint directly.** Rejected for now — duplicates OIDC/Keycloak wiring, entitlement enforcement, and connection-lifecycle management `backend` already has, and fragments where clients connect to two services instead of one, for the sake of avoiding one Kafka hop.
- **REST polling only, no real-time push.** Rejected as the sole mechanism — fine for low-priority/informational notifications (matches CSPROD-88's "planned outages, what's new" framing) but insufficient for anything time-sensitive.
- **Skip FCM/APNs, rely on SSE/gRPC + app background refresh.** Rejected — SSE/gRPC only reaches actively-connected clients; a backgrounded or force-quit app needs OS-level push to wake it, which is the entire reason FCM/APNs exist.

## Consequences

### Positive

- One client connection/auth surface (`backend`); `notification-dispatcher` stays a narrow background worker (Kafka in, Postgres out, provider push out).
- Priority is a data field, easy to audit and reason about, rather than a parallel code path.
- FCM/APNs generic-payload design closes a transit-layer sovereignty gap of the same shape flagged in platform ADR-0005 (observability egress) — no regionally-bound customer data leaves the region boundary via a global push provider.

### Trade-offs & limitations

- One more Kafka topic and one more hop for real-time delivery (`notification-dispatcher` → Kafka → `backend` → client) versus a direct push from `notification-dispatcher`.
- `backend` and `notification-dispatcher` become coupled through `notification.created` in addition to the existing `device.location`/`device.status` coupling. This is accepted on a blast-radius basis, not an edge-count basis: `notification-dispatcher` going offline degrades to "no new alerts" — the platform continues operating. `backend` going offline takes the platform down. Accepting one more coupling edge on the low-blast-radius side is a reasonable trade against duplicating auth/entitlement surfaces on the critical-path service.
- Push-token storage ownership is unresolved (see Open Items).

## Open Items

- Where do FCM/APNs push tokens live — `backend` (alongside `DeviceEntity`/`UserEntity`, which already own device/user registration) or `notification-dispatcher`? Leaning `backend`, not decided.
- Exact schema of the `notification.created` Kafka event is not yet specified.
- Whether `backend`'s existing `device_stream.proto` gRPC surface is extended with a new message type, or a separate proto/service is used for notifications — leaning toward extending, given ADR-0010 already established that transport, but not decided.
- FCM/APNs credential management, retry policy, and provider-outage handling is unspecified — may warrant its own implementation-level ADR if it grows complex enough.

## Forward Pointers

- Builds on CSPROD-165 (notification-dispatcher bootstrap — Postgres persistence side, done).
- Reuses ADR-0010's `DeviceEventBroadcaster`/SSE/gRPC pattern for the new `NotificationEventBroadcaster`.
- ADR-0011's entitlement-bypass exceptions table gains a new row for Priority Notifications (see cross-reference in ADR-0011).
- CSPROD-88 (Platform notification endpoint) is a narrower platform-announcement SSE ticket that may fold into or be superseded by this design.
