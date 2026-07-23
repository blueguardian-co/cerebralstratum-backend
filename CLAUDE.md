# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

Prerequisites: JDK 21, Maven 3.9+, Podman (for MQTT-dependent services), 1Password CLI (`op`) for `hack-*` targets.

```shell
# Build all modules (skip tests)
make build                        # ./mvnw install -DskipTests

# Build a single module and its dependencies
./mvnw -pl backend -am clean verify

# Dev mode — each service in a separate terminal
make dev-backend                  # port 6443; Postgres/Keycloak/Kafka Dev Services auto-start
make dev-registrar                # port 6444; start MQTT broker first (see below)
make dev-dispatcher               # port 6445; Kafka/Postgres/Redis Dev Services auto-start
make dev-simulator                # start MQTT broker first

# Start MQTT broker (required before registrar or simulator)
cd device-simulator && ./start-mqtt.sh

# Full stack with automatic 1Password secret injection and MQTT lifecycle
make hack-all
make hack-backend | hack-registrar | hack-dispatcher | hack-simulator
```

## Tests

```shell
make test                         # all modules, unit tests only
make test-backend                 # backend unit tests

# All modules unit + integration tests
./mvnw verify -DskipITs=false

# Single module
./mvnw -pl backend test
./mvnw -pl backend verify -DskipITs=false

# Single test class or method
./mvnw -pl utils -Dtest=co.blueguardian.cerebralstratum.utils.ExampleTest test
./mvnw -pl utils -Dtest=ExampleTest#methodName test
```

Integration tests use the `*IT.java` naming convention and are skipped by default (`skipITs=true`). They start real Dev Service containers. The `native` profile sets `skipITs=false` automatically.

## Module Overview

| Module | Status | Purpose |
|--------|--------|---------|
| `backend` | Active | REST API + gRPC streaming (port 6443 / 9000) |
| `utils` | Active | Shared DTOs, Kafka deserializers, UUIDv5 |
| `device-registrar` | Stub | MQTT → Kafka bridge, device lifecycle (port 6444) |
| `notification-dispatcher` | Active | Kafka consumer → Redis dedupe/rate-limit → Postgres delivery record (port 6445); FCM/APNs push not yet implemented |
| `device-simulator` | Active | Dev tool — simulates MQTT device publishing |

All modules inherit from the root `pom.xml` which pins Quarkus 3.35.4 and Java 21.

## Knowledge Base

`Writerside/topics/` is the source of truth for this platform's architecture — consult it before reasoning about cross-service design, edge/device connectivity, or the rationale behind a decision. `Writerside/topics/Architecture.md` gives the high-level service breakdown; `Writerside/topics/ADRs/` holds merged Architecture Decision Records with full context/decision/consequences for individual decisions (see the ADR lifecycle below for how content gets there). The summaries below are a quick reference — the Writerside docs are where the "why" lives, and take precedence if they ever disagree with this file.

## Architecture

### Backend — Hexagonal Layout

```
controllers/      ← JAX-RS resources + domain records + request DTOs, grouped by domain
  devices/        ← DevicesResource, DeviceStreamService (gRPC), Device, CreateDeviceRequest …
  locations/
  organisations/
  users/
  groups/
repositories/     ← Repository interface + EntityManager implementation + JPA entity, per domain
  devices/        ← DeviceRepository (interface), EntityManagerDeviceRepository, DeviceEntity
  locations/
  organisations/
  users/
utils/            ← PermissionCheckers (@PermissionChecker beans)
adapters/         ← (reserved, currently empty)
ports/            ← (reserved, currently empty)
libs/             ← (reserved, currently empty)
```

The domain record (e.g., `Device`) and its request types (`CreateDeviceRequest`, `UpdateDeviceRequest`) live alongside the resource in `controllers/<domain>/`. JPA entities live exclusively in `repositories/<domain>/`.

### Real-Time Streaming

`DeviceEventBroadcaster` is the single point of Kafka consumption for device telemetry — one `@Incoming` consumer per topic (`device.location`, `device.status`, `device.canbus`), fanned out in-process via `BroadcastProcessor` and filtered per-device (`locationUpdatesFor`/`statusUpdatesFor`/`canBusUpdatesFor`). Two transports consume it (ADR-0010):
- `DeviceServerSentEvents` — SSE, the actual web client transport (gRPC is unreachable from browsers without `grpc-web` + a proxy).
- `DeviceStreamService` — `@GrpcService`, reserved for future mobile/KMP clients. Must be `@Singleton` and extend `DeviceStreamServiceGrpc.DeviceStreamServiceImplBase` (not just implement `AsyncService`) or Quarkus silently never binds it — see ADR-0010 for this and the merged-HTTP/gRPC-server gotcha. Fixed and verified against `device-simulator`, but no real client is integrated against it yet.

Proto file: `backend/src/main/proto/device_stream.proto`  
Generated classes land in `co.blueguardian.cerebralstratum.backend.grpc`.

**Gotcha:** any REST/SSE-exposed DTO with a JTS `Point` field needs `@JsonSerialize(using = PointSerializer.class)` / `@JsonDeserialize(using = PointDeserializer.class)` (from `utils.model`) directly on the field. `quarkus-rest-jackson`'s build-time serializer codegen can't see the runtime-registered `GeometryJacksonModule` and falls back to its own reflection-based serializer, which recurses forever over `Point`'s self-referential getters (`getEnvelope()`, `getCentroid()`, ...) — a `StackOverflowError` in production, not a compile-time or dev-mode-only failure. See CSPROD-183.

### Security Model

- OIDC via Keycloak; JWT roles claim path `realm_access/roles`.
- `@RolesAllowed("admins")` for platform-wide admin operations.
- `@PermissionsAllowed("member-of-device-group")` resolved by `PermissionCheckers` — checks JWT `groups` claim for `/{device_uuid}`, `/{device_uuid}/view-only`, or `/{device_uuid}/modify`.
- Device registration is quota-gated: `UserEntity.subscription_entitlement` vs `subscription_used`.
- In `%dev`, Keycloak Dev Service starts automatically from `devservices/realm.json`.
- In `%prod`, Keycloak policy enforcer is active; only `/q/*`, `/swagger-ui/*`, and `/openapi` are unprotected.

### Database

- PostgreSQL + PostGIS; Hibernate Spatial for geometry columns.
- Schema: `cerebralstratum`. Dev Services pin Postgres to port `5432`.
- Liquibase changelogs:
  - `%dev`: `devservices/changeLog.yaml` (includes `db/changeLogs/` + `devservices/changeLogs/` with test data)
  - `%prod`: `db/changeLog.yaml`
- `UserEntity` ID = Keycloak user UUID (no separate sequence).

### utils Module

Shared JAR on the classpath of all service modules:
- `model/Status`, `model/Location` — Kafka message payloads (also stored as JSON columns in Postgres via `@JdbcTypeCode(SqlTypes.JSON)`)
- `model/GeometryJacksonModule`, `model/PointSerializer`, `model/PointDeserializer` — JTS `Point` (de)serialization. Each consuming service registers `GeometryJacksonModule` at runtime via its own `ObjectMapperCustomizer` (there's no shared one) — see the Real-Time Streaming gotcha above for why REST/SSE DTOs also need the field-level annotations, not just this module registration.
- `messaging/CANBusMessage`, `messaging/LocationMessage`, `messaging/StatusMessage` — Kafka message shape classes. Quarkus's zero-config Kafka messaging generates the Jackson (de)serializer per type from these at build time — there are no hand-written deserializer classes.
- `uuid/UUIDv5Generator`

### Kafka Topics

| Channel name (MP config) | Key type | Value type |
|--------------------------|----------|------------|
| `device.location` | `UUID` | `GetLocationRequest` |
| `device.status` | `UUID` | `Status` (from `utils`) |
| `device.canbus` | `UUID` | `CANBus` |

Dev Services pin Kafka to port `9092`.

### Notification Dispatch

`notification-dispatcher` consumes `device.location`/`device.status` (the same topics `DeviceEventBroadcaster` reads — there's no dedicated notification-trigger topic yet) via `NotificationEventConsumer`, and calls `NotificationDispatchService.dispatch(deviceId, eventType, occurredAt)` per event. That method is `@CacheResult(cacheName = "notification-dispatch")` with `deviceId`/`eventType` as `@CacheKey` — the method body (a Postgres write to the `notifications` table) only runs on a Redis cache miss, which is the dedupe/rate-limit mechanism (5 min window, `quarkus.cache.redis."notification-dispatch".expire-after-write`). `occurredAt` is deliberately excluded from the cache key since every event has a distinct timestamp.

Real-time in-app delivery (SSE/gRPC hand-off to `backend`) and FCM/APNs push are decided but not yet implemented — see ADR-0012, tracked in CSPROD-181.

## YouTrack Bridge (ADRs & Implementation Tracking)

YouTrack (`https://youtrack.blueguardian.co`) is the authoritative source for
tasks, lines of effort, and ADR staging. This repo's `Writerside/topics/ADRs/`
directory is the authoritative source for **merged** ADR content. Claude Code is
the bridge between the two — Claude (claude.ai) drafts, Claude Code commits.

### ADR lifecycle

1. **Draft** — An ADR is drafted in a claude.ai session and created as a
   YouTrack **article** under this component's root article (see
   `YOUTRACK_ROOT_ARTICLE` below), tagged `status: draft`.
2. **Migrate** — When asked to "sync ADRs" or "pull ADR <id>", Claude Code:
   - Fetches the article via YouTrack MCP (`get_article`).
   - Writes it to `Writerside/topics/ADRs/0XXX-<slug>.md` in this repo,
     preserving the Context / Decision / Alternatives Considered / Consequences
     / Open Items / Forward Pointers structure verbatim.
   - Registers the new topic under the `ADRs.md` toc-element in `cs-b.tree`.
   - Commits with message `docs(adr): add ADR-0XX <title>`.
   - Reports back the commit SHA and file path.
3. **Close the loop** — Claude (claude.ai) updates the source article: tag
   flips to `status: merged`, and a line is appended noting the repo path and
   commit link. The article remains as a searchable index entry; the
   `Writerside/topics/ADRs/` file is the canonical content going forward — do
   not edit the article further after this point. Corrections happen in-repo
   via normal PR flow.

`YOUTRACK_ROOT_ARTICLE`: [CSPROD-A-10](https://youtrack.blueguardian.co/articles/CSPROD-A-10) — "Backend" (child of Architecture Decision Records, `CSPROD-A-5`).

### Implementation tracking (issues)

Each ADR that requires implementation work gets a matching **Epic** in
YouTrack (same project as the article, `Type: Epic`), linked to the article
by ID in its description. Concrete work is filed as `Task` / `Sub-task`
issues under that Epic.

Fields in use (CSPROD project):
- `Type`: Epic, Story, Task, Sub-task, Bug, New Feature, Bug Fix
- `Subsystem`: backend, frontend, iOS, firmware, infra
- `State`: Backlog, Selected for Development, In Progress, Fixed, Done,
  Open, Duplicate
- `Priority`: Highest, High, Medium, Low, Lowest

**Claude Code's responsibility during implementation work:**
- When starting work on a ticket, move `State` to `In Progress`
  (`update_issue`).
- When a PR lands that implements a ticket, move `State` to `Fixed` (or
  `Done` for Epics once all children are closed) and reference the commit/PR
  in a comment or the issue description.
- Do not close/re-prioritize tickets outside the scope of the current task —
  only touch the ticket(s) explicitly being worked.
- If work reveals the ADR itself needs revision (an Open Item gets resolved,
  a Consequence turns out wrong), flag this back rather than silently
  editing `Writerside/topics/ADRs/*.md` — ADR amendments are a deliberate
  decision-first step, same as original drafting.

This gives a real feedback loop: ticket state in YouTrack reflects actual
implementation progress against the ADR, not just intent.

---

## Configuration Notes

- `QUARKUS_OTEL_EXPORTER_OTLP_HEADERS` — injected by `hack_modules.sh` from 1Password; needed to ship telemetry to Grafana Cloud in `%dev`.
- Prod env vars: `JDBC_URL`, `JDBC_USERNAME`, `JDBC_PASSWORD`, `OIDC_AUTH_SERVER_URL`, `OIDC_SECRET`, `keycloak.username`, `keycloak.password`, `keycloak.realm`.
- `quarkus.keycloak.devservices.enabled: true` by default — set to `false` if you want to supply your own Keycloak at `:8000`.
- Fixed Dev Service ports (5432, 9092) conflict with local services; free them or override in `application.yml`.
