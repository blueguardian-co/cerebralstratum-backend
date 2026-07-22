# ADR-0010: Real-Time Device Data Transport — SSE for Web, gRPC Reserved for Mobile/KMP

| Field              | Value                                                                                                    |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Status**         | Proposed                                                                                                  |
| **Date**           | 2026-07-22                                                                                                |
| **Author**         | Alex Henshaw (drafted with Claude Code, refined with Claude Sonnet 5, based on implementation findings)   |
| **Migrated from**  | [CSPROD-A-32](https://youtrack.blueguardian.co/articles/CSPROD-A-32)                                     |

---

## 1. Context

The primary backend's `DeviceStreamService` streams live device telemetry (location, status, CAN bus frames) from Kafka (`device.location`, `device.status`, `device.canbus`) to connected clients. The original implementation used WebSockets/SSE; this was later moved to gRPC (`StreamDeviceUpdates`, a server-streaming RPC defined in `device_stream.proto`) for scalability.

Until this session, that gRPC path had **never been exercised end-to-end with real data or a real client** — nothing had ever produced to the Kafka topics it consumes, and no client (browser or native) had ever successfully connected to it. Standing up `device-simulator` (to generate realistic device telemetry for testing) surfaced this gap directly: once real data was flowing, the frontend showed all devices as offline and never received live GPS pings, because the gRPC stream was never actually reachable.

### 1.1 Findings from attempting to exercise gRPC

Investigating why `StreamDeviceUpdates` wasn't delivering data surfaced several concrete problems:

1. **Browser incompatibility.** Browsers cannot speak gRPC's HTTP/2 framing natively. Reaching a gRPC service from a web client requires `grpc-web` plus a translating proxy (typically Envoy) — additional infrastructure and an extra hop, working against the "compatibility and stability" priority at this stage of the project.
2. **Merged HTTP/gRPC server mode is broken for this stack.** With `quarkus.grpc.server.use-separate-server=false` (gRPC multiplexed onto the main HTTP port), requests to `StreamDeviceUpdates` returned a raw HTTP 500 with an HTML error body instead of a gRPC response. This matches a known Quarkus issue where `quarkus-keycloak-authorization` interacts badly with the merged gRPC/HTTP server ([quarkusio/quarkus#34085](https://github.com/quarkusio/quarkus/issues/34085) — the specific blocking-thread symptom was fixed in 3.2.1.Final, but the combination remains fragile enough to produce this failure on our version).
3. **Separate server mode starts, but registers zero services.** Switching to `quarkus.grpc.server.use-separate-server=true` got the gRPC server binding correctly on port 9000, but `grpcurl` reflection reported zero registered services, and a direct proto-based call returned `UNIMPLEMENTED: Method not found` for `DeviceStreamService/StreamDeviceUpdates` — despite the class being correctly annotated `@GrpcService` and implementing the generated service interface. **Not conclusively root-caused**: investigation was confounded by an unrelated local environment issue (the repo living under iCloud Drive sync, independently confirmed to nondeterministically duplicate generated source files mid-build, corrupting compilation without always throwing a hard error). A clean-environment retest (repo moved out of iCloud sync) is needed before concluding whether this is a genuine `quarkus-grpc` bug worth filing upstream.

## 2. Decision

Use **Server-Sent Events (SSE)** for backend → web client live telemetry push, and **reserve gRPC for future native/KMP mobile and desktop clients**, where `grpc-kotlin` has first-class support and the browser transport problem doesn't apply.

Both SSE and gRPC consumers read independently from the same Kafka topics (separate consumer groups) — this is a fan-out of one event stream to two transport surfaces for two different client populations, not a duplicate ingestion/write path. This is consistent with Principle #1: neither transport writes to PostgreSQL; both are read-side projections of the same Kafka stream.

`DeviceStreamService`/`device_stream.proto` is **not deleted** — the gRPC contract is retained for the future mobile/KMP path, but must be treated as unverified/dormant until the zero-services-registered issue is resolved and it has been exercised against a real client.

**Consumer group naming (confirmed):** each transport gets its own Kafka consumer group, service-scoped and transport-suffixed:

- `cerebral-stratum.device-stream.sse`
- `cerebral-stratum.device-stream.grpc`

This pattern is intended to generalize — a future third consumer of these topics gets `cerebral-stratum.device-stream.<transport>` rather than a one-off naming decision.

### 2.1 Implementation requirements (this ADR's scope of work)

`DeviceServerSentEvents` already exists with correct REST endpoint shapes and `@PermissionsAllowed("member-of-device-group")` checks, but is non-functional scaffolding. To make it work:

| # | Fix | Current state |
|---|---|---|
| 1 | Channel wiring | Points at placeholder channels (`location`/`status`/`canbus`) instead of the real Kafka topics (`device.location`/`device.status`/`device.canbus`) |
| 2 | Payload key/value mismatch | Expects `device_id` embedded in the Kafka *value*; the real value types (`GetLocationRequest`, `Status`, `CANBus`) carry `device_id` only in the Kafka *key* |
| 3 | Per-device filtering | `status`/`canbus` broadcast methods don't filter by the `{device_uuid}` path parameter at all — only `location` does. Every subscriber currently receives every device's data regardless of what they subscribed to |
| 4 | Consumer group isolation | Configure `cerebral-stratum.device-stream.sse` explicitly for the SSE consumer (see naming decision above). Left to default, it would share the app's default group ID with the gRPC consumer and silently compete for partitions instead of both receiving every message |

## 3. Alternatives Considered

| Alternative | Reason considered | Reason rejected |
|---|---|---|
| WebSockets | Bidirectional support | Overkill for one-way telemetry push; adds connection-lifecycle/reconnect handling SSE already provides natively in-browser (`EventSource`) |
| Keep gRPC for web via `grpc-web` + Envoy | Single unified transport for all client types | Adds a proxy dependency and operational complexity working against the "compatibility and stability" priority right now — deferred, not permanently ruled out |

## 4. Consequences

### Positive

- SSE is natively supported by every browser (`EventSource`), with built-in reconnect — no proxy or translation layer required.
- Matches the actual data-flow shape needed (one-way server→client push); avoids WebSocket's bidirectional complexity where nothing needs to flow client→server.
- This is a repair/wiring job on existing scaffolding, not new architecture — `DeviceServerSentEvents` already has the right endpoint shapes and security checks.
- Keeps gRPC available for the future mobile/KMP path without forcing a single transport choice platform-wide.
- Consumer group naming convention is settled up front, so the gRPC consumer group (`cerebral-stratum.device-stream.grpc`) can be wired in later without revisiting the scheme.

### Trade-offs & limitations

- Two live-data transports to maintain (SSE for web, gRPC for future mobile) instead of one — accepted as deliberate given the different client capabilities involved.
- SSE has no client→server streaming direction; a future need for live client-to-backend streaming (e.g. commands) will need a separate mechanism.
- The gRPC path's registration issue is unresolved and must be fixed and verified against a real client before the mobile/KMP path can depend on it.

## 5. Open Items

- Root cause of the gRPC zero-services-registered issue is not conclusively isolated — confounded by the iCloud-sync build corruption this session. Needs a clean-environment retest (repo now being moved out of iCloud Drive sync) before deciding whether to file upstream against `quarkusio/quarkus`.
- `DeviceServerSentEvents` fixes (§2.1, items 1–3) need to be implemented and verified end-to-end against simulated device data.
- Whether/when `grpc-web` + Envoy might be revisited for a unified web+mobile transport is left open, not decided against permanently.

## 6. Forward Pointers

- Depends on `device-simulator`'s dev-only MQTT→Kafka bridge (or, longer-term, Eclipse Hono) producing to `device.location`/`device.status`/`device.canbus` — confirmed working this session.
- Relates to the still-open gap that nothing currently persists incoming Kafka telemetry to PostgreSQL (`DeviceEntity.status`, `locations` table) — out of scope for this ADR, but the reason the frontend shows devices as offline independently of the transport issue addressed here.
- When the gRPC path is eventually wired up for mobile/KMP, its consumer group (`cerebral-stratum.device-stream.grpc`) should be configured at that time, per the naming decision in §2.
