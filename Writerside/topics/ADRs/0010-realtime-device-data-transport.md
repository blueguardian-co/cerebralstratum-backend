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
3. **Separate server mode starts, but registers zero services.** Switching to `quarkus.grpc.server.use-separate-server=true` got the gRPC server binding correctly on port 9000, but `grpcurl` reflection reported zero registered services, and a direct proto-based call returned `UNIMPLEMENTED: Method not found` for `DeviceStreamService/StreamDeviceUpdates` — despite the class being correctly annotated `@GrpcService` and implementing the generated service interface. At the time this was written, investigation was confounded by an unrelated local environment issue (the repo living under iCloud Drive sync, independently confirmed to nondeterministically duplicate generated source files mid-build, corrupting compilation without always throwing a hard error) and not conclusively root-caused.

   **Resolved (2026-07-22, clean-environment retest):** after moving the repo out of iCloud Drive sync, the failure reproduced identically — ruling out iCloud corruption as the cause. Root cause was isolated to two things, both within our own code, neither a Quarkus bug:
   - `DeviceStreamService` was annotated `@ApplicationScoped`; Quarkus's `GrpcServerProcessor` requires `@GrpcService` beans to be `@Singleton` and throws a hard `IllegalStateException` at build/startup time otherwise (`use-separate-server=true` surfaces this as a startup crash; `use-separate-server=false` apparently swallows it into the flakier "zero services" / HTTP 500 symptom instead).
   - The class implemented the plain generated `DeviceStreamServiceGrpc.AsyncService` interface rather than extending `DeviceStreamServiceGrpc.DeviceStreamServiceImplBase`. The latter is what actually implements `io.grpc.BindableService` and gets picked up by Quarkus's build-time service registration — implementing the bare interface compiles fine and passes CDI validation, but is silently never bound to the server (no error, just `UNIMPLEMENTED` at call time).

   Fixing both (`@Singleton` + `extends DeviceStreamServiceImplBase`) in `DeviceStreamService.java` made `grpcurl -plaintext localhost:9000 list` report `devicestream.DeviceStreamService`, and a live `StreamDeviceUpdates` call against the running `device-simulator` correctly streamed real `CurrentLocation`/`CANBusMessage` updates for the seeded test device. Not worth filing upstream — this was a misconfiguration on our side, not a `quarkus-grpc` defect.

## 2. Decision

Use **Server-Sent Events (SSE)** for backend → web client live telemetry push, and **reserve gRPC for future native/KMP mobile and desktop clients**, where `grpc-kotlin` has first-class support and the browser transport problem doesn't apply.

Both SSE and gRPC consumers read from the same Kafka topics and fan out to two transport surfaces for two different client populations — this is not a duplicate ingestion/write path. This is consistent with Principle #1: neither transport writes to PostgreSQL; both are read-side projections of the same Kafka stream. (As originally planned, this fan-out was to happen via separate per-transport consumer groups; what got built instead shares one consumer per topic — see the flagged deviation below.)

`DeviceStreamService`/`device_stream.proto` is **not deleted** — the gRPC contract is retained for the future mobile/KMP path. As of 2026-07-22 it has been fixed (see §1.1) and exercised end-to-end against `device-simulator`, but still has no real client (browser or mobile/KMP) integrated against it — that integration work remains unstarted.

**Consumer group naming (as originally planned):** each transport gets its own Kafka consumer group, service-scoped and transport-suffixed:

- `cerebral-stratum.device-stream.sse`
- `cerebral-stratum.device-stream.grpc`

This pattern was intended to generalize — a future third consumer of these topics gets `cerebral-stratum.device-stream.<transport>` rather than a one-off naming decision.

> **⚠ Flagging for decision — implementation deviated from this plan.** What actually got built is a single `DeviceEventBroadcaster` bean holding one `@Incoming` Kafka consumer per topic (implicit group id = `quarkus.application.name`, i.e. `cerebral-stratum-backend`), fanned out in-process via `BroadcastProcessor` to *both* the SSE resource and the gRPC service. There is one Kafka consumer per topic for the whole backend process, not two — the per-transport consumer group naming above was never wired in. This was a deliberate simplification made during implementation (verified working end-to-end against `device-simulator` on 2026-07-22): it avoids reading each topic twice for what is otherwise identical fan-out, and there's no correctness reason to duplicate the Kafka reads. It does mean the two consumer group names above are currently unused, and the "future third consumer" generalization this section describes doesn't apply to the transports already built. Leaving this flagged rather than silently resolving it: worth a conscious call on whether to (a) keep the shared-broadcaster design and demote/remove the per-transport group naming from this ADR, or (b) split it back out to independent consumer groups per transport for some isolation reason (e.g. independent backpressure/restart behavior per transport) not yet identified.

### 2.1 Implementation requirements (this ADR's scope of work)

`DeviceServerSentEvents` already exists with correct REST endpoint shapes and `@PermissionsAllowed("member-of-device-group")` checks, but is non-functional scaffolding. To make it work:

| # | Fix | Original state | Resolution |
|---|---|---|---|
| 1 | Channel wiring | Points at placeholder channels (`location`/`status`/`canbus`) instead of the real Kafka topics (`device.location`/`device.status`/`device.canbus`) | **Done.** Both SSE and gRPC now read from a shared `DeviceEventBroadcaster` with `@Incoming("device.location")` / `.status` / `.canbus"` — no `application.yml` config needed; Quarkus's zero-config Kafka messaging resolves channel name → topic name → `smallrye-kafka` connector by convention. Verified via backend startup logs (`Configuring the channel 'device.location' to be managed by the connector 'smallrye-kafka'`) and live data flow. |
| 2 | Payload key/value mismatch | Expects `device_id` embedded in the Kafka *value*; the real value types (`GetLocationRequest`, `Status`, `CANBus`) carry `device_id` only in the Kafka *key* | **Done.** `DeviceEventBroadcaster` reads `ConsumerRecord<UUID, T>` and pairs `record.key()` with `record.value()` into `DeviceLocationEvent`/`DeviceStatusEvent`/`DeviceCanBusEvent`. |
| 3 | Per-device filtering | `status`/`canbus` broadcast methods don't filter by the `{device_uuid}` path parameter at all — only `location` does. Every subscriber currently receives every device's data regardless of what they subscribed to | **Done.** All three `DeviceEventBroadcaster` accessor methods (`locationUpdatesFor`/`statusUpdatesFor`/`canBusUpdatesFor`) filter by device UUID before returning the `Multi`, used identically by both `DeviceServerSentEvents` and `DeviceStreamService`. |
| 4 | Consumer group isolation | Configure `cerebral-stratum.device-stream.sse` explicitly for the SSE consumer (see naming decision above). Left to default, it would share the app's default group ID with the gRPC consumer and silently compete for partitions instead of both receiving every message | **Superseded, not done as originally specified** — see the flagged note under §2 above. Since both transports now share one `DeviceEventBroadcaster`/one Kafka consumer per topic instead of two independent consumers, the competing-consumer-groups failure mode this item was guarding against no longer applies, but the named consumer groups were never configured. |

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
- ~~The gRPC path's registration issue is unresolved and must be fixed and verified against a real client before the mobile/KMP path can depend on it.~~ Registration is fixed and verified against `device-simulator` (2026-07-22); a real mobile/KMP client still hasn't been integrated against it.

## 5. Open Items

- ~~Root cause of the gRPC zero-services-registered issue is not conclusively isolated~~ — **Resolved 2026-07-22.** Root-caused to `DeviceStreamService` needing `@Singleton` scope and to extend `DeviceStreamServiceGrpc.DeviceStreamServiceImplBase` (not merely implement the plain `AsyncService` interface). Not an iCloud artifact, not a `quarkus-grpc` bug — see §1.1 finding 3. Fixed in code and verified against live simulator data.
- ~~`DeviceServerSentEvents` fixes (§2.1, items 1–3) need to be implemented and verified end-to-end~~ — **Resolved.** Implemented via the shared `DeviceEventBroadcaster`; verified end-to-end against `device-simulator` output for the seeded test device (`e4bb7b63-6619-589b-98a3-549d0cedc8bc`).
- **New:** the consumer-group-per-transport design in §2 was never implemented — both transports share one consumer per topic instead. Needs a decision: update this ADR to describe the shared-consumer design as the accepted approach, or split it back out. See flagged note under §2.
- Whether/when `grpc-web` + Envoy might be revisited for a unified web+mobile transport is left open, not decided against permanently.

## 6. Forward Pointers

- Depends on `device-simulator`'s dev-only MQTT→Kafka bridge (or, longer-term, Eclipse Hono) producing to `device.location`/`device.status`/`device.canbus` — confirmed working this session, and reconfirmed in the 2026-07-22 clean-environment retest.
- Relates to the still-open gap that nothing currently persists incoming Kafka telemetry to PostgreSQL (`DeviceEntity.status`, `locations` table) — out of scope for this ADR, but the reason the frontend shows devices as offline independently of the transport issue addressed here. Note this is distinct from [CSPROD-176](https://youtrack.blueguardian.co/issue/CSPROD-176) (a schema type mismatch on the `locations` table itself, found during this session's verification) — both need to be resolved before persistence can work correctly.
- When/if the gRPC path's consumer group is split back out from the shared broadcaster for mobile/KMP, its consumer group (`cerebral-stratum.device-stream.grpc`) should be configured at that time, per the naming decision in §2 — contingent on resolving the flagged open item on whether the shared-broadcaster design supersedes that plan.
- [CSPROD-177](https://youtrack.blueguardian.co/issue/CSPROD-177): `device-simulator`'s `DeviceTelemetryPublisher.canbusEmitter` fails to wire up, found during this session's verification — doesn't block the MQTT→Kafka bridge path this ADR depends on, but should be cleaned up or removed if dead code.
