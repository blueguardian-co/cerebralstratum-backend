# ADR-0004: `utils` Module as a Shared Library JAR

| Field        | Value                  |
|--------------|------------------------|
| **Status**   | Accepted               |
| **Date**     | 2026-05-31             |
| **Authors**  | Platform Architecture  |
| **Deciders** | Platform Architecture  |

---

## Context

The `utils` module contains shared DTOs (`LocationMessage`, `StatusMessage`), a Jackson deserializer (`LocationMessageDeserializer`), and a UUID utility (`UUIDv5Generator`). Despite this library-grade content, its `pom.xml` was scaffolded as a runnable Quarkus service, pulling in `quarkus-rest`, `quarkus-oidc`, `quarkus-keycloak-admin-rest-client`, `quarkus-smallrye-openapi`, `quarkus-arc`, and `quarkus-opentelemetry` — none of which have any corresponding code in the module.

This created ambiguity: is `utils` a library JAR that other services depend on, or a runnable identity/admin helper service?

---

## Decision

**Option (a): Pure shared-library JAR.**

All content in `utils` is library-grade. No `@Path`, `@QuarkusMain`, or REST resources exist. The `application.yml` is empty. The `backend` service already declares `utils` as a Maven dependency, confirming its role as a consumed library. The service-shaped dependencies were vestigial scaffolding.

### Rejected alternatives

**Option (b): Split into `utils-lib` + small admin service.** Would only be warranted if real running service code (e.g. a Keycloak admin microservice) were co-located here. No such code exists.

**Option (c): Keep as a service and rename.** Misleading given the actual content; also forces Quarkus extension resolution on all consumers unnecessarily.

---

## Consequences

- `utils/pom.xml` declares only `jackson-databind` and `jts-core` as dependencies; version management is inherited from the root `quarkus-bom` import.
- The `quarkus-maven-plugin` build and code-generation goals are disabled in `utils` — the module packages as a plain JAR.
- Docker assets (`src/main/docker/`) are removed; this module will not be containerised.
- All downstream consumers (`backend`, and future services) depend on `utils` as a Maven compile-scoped JAR. No Quarkus extension resolution is forced on consumers by `utils` itself.
