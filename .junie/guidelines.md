### Cerebral Stratum Backend — Project‑specific Development Guidelines

#### Build and Configuration
- Toolchain
  - Java: Use JDK 21. Although the root `pom.xml` declares `maven.compiler.release` 17, the application modules (e.g., `backend`, `utils`) set `maven.compiler.release` to 21. A Java 21 toolchain avoids mixed‑release build issues.
  - Maven: 3.9.x or newer is recommended.
  - Quarkus: 3.22.2 across modules, managed via the platform BOM and `quarkus-maven-plugin`.

- Multi‑module layout (root `pom.xml` packaging=pom)
  - Modules: `backend`, `ingress-classifier`, `device-data-persist`, `device-simulator`, `utils`.
  - Dependency alignment is handled via Quarkus BOM import in parent and modules.

- Profiles and important properties
  - `native` profile in root and modules:
    - Sets `quarkus.native.enabled=true` and `skipITs=false` to allow integration tests during native build flows.
  - Default property `skipITs=true` prevents the Failsafe plugin from running integration tests unless explicitly overridden.

- Dev Services and runtime config (focus: `backend/src/main/resources/application.yml`)
  - Postgres Dev Service
    - Enabled by default for the datasource; configured DB image: `quay.io/enterprisedb/postgresql:17-postgis-multiarch-ubi9`.
    - Port pinned to `5432` with init script `devservices/init.sql`.
  - Liquibase
    - `%dev`: `migrate-at-start: true` with `devservices/changeLog.yaml`.
    - `%prod`: `migrate-at-start: true` with `db/changeLog.yaml`.
  - Keycloak
    - Quarkus Keycloak Dev Service is explicitly disabled: `quarkus.keycloak.devservices.enabled: false`.
    - In `%dev`, OIDC points at `http://localhost:8000/realms/${keycloak.realm}` with a static client secret. If you don’t run a local Keycloak at `:8000`, you will need to either:
      - Provide a Keycloak instance matching these URLs/realm/credentials, or
      - Temporarily disable enforcement (policy enforcer is already `enable: false` by default) and stub out OIDC‑dependent calls during development.
  - Kafka Dev Service
    - Enabled for `quarkus.kafka.devservices`, port pinned to `9092`.
  - HTTP
    - Default port `6443` in base config, `%dev` binds to `localhost` and enables permissive CORS.

#### Build and Run
- Full build (all modules):
  - `mvn -U -T1C clean verify`  (leverages Surefire for unit tests; Failsafe ITs are skipped by default)
- Build a single module:
  - `mvn -pl utils -am clean verify`
    - `-pl` selects module; `-am` builds required dependencies.
- Run `backend` in dev mode (hot reload and Dev Services):
  - `mvn -pl backend -am quarkus:dev`
  - Notes:
    - Postgres and Kafka Dev Services will spin up automatically.
    - Keycloak Dev Service is disabled; ensure `%dev` OIDC settings are valid for your environment or disable endpoints requiring auth when iterating locally.

#### Testing
- Test framework and plugins
  - Unit tests: JUnit 5 via `maven-surefire-plugin` (3.5.3). No special annotations required other than standard JUnit Jupiter.
  - Integration tests: `maven-failsafe-plugin` (3.5.3). The convention is `*IT.java` (or `*ITCase.java`). By default, ITs are skipped with `skipITs=true` unless you override.

- Running tests
  - All modules (unit tests): `mvn test`
  - All modules (unit + ITs): `mvn verify -DskipITs=false`
  - Single module unit tests: `mvn -pl utils test`
  - Single module including ITs: `mvn -pl backend verify -DskipITs=false`
  - Single test class by FQN (when using Surefire): `mvn -pl utils -Dtest=co.blueguardian.cerebralstratum.utils.YourTest test`
  - Single test method: `mvn -pl utils -Dtest=YourTest#yourMethod test`

- Adding a new unit test (example)
  - Place tests under the module’s `src/test/java` using the same package structure as sources.
  - Example JUnit 5 test:
    ```java
    package co.blueguardian.cerebralstratum.utils;

    import org.junit.jupiter.api.Test;
    import static org.junit.jupiter.api.Assertions.assertEquals;

    class ExampleTest {
        @Test
        void addsNumbers() {
            assertEquals(4, 2 + 2);
        }
    }
    ```
  - Run just this test in the `utils` module:
    ```bash
    mvn -pl utils -Dtest=co.blueguardian.cerebralstratum.utils.ExampleTest test
    ```

- Integration testing notes
  - Because Dev Services are used for the database and Kafka, ITs that hit the Quarkus runtime may start containers automatically and can be time‑consuming.
  - If ITs depend on Keycloak, you must provide a running Keycloak aligned with `%dev` config (or enable Keycloak Dev Service by changing configuration) before running `verify` with `-DskipITs=false`.
  - Native profile (`-Pnative`) sets `skipITs=false` as part of the profile properties. Ensure your environment has GraalVM/Native Image prerequisites if you plan to build/run native ITs.

#### Additional Development Notes
- Code style and language level
  - Prefer Java 21 language features where appropriate in modules that declare 21. Maintain consistent formatting with existing code (2 or 4 space indentation as per module, standard imports ordering).
  - Follow the package structure already present, e.g. `co.blueguardian.cerebralstratum.backend.*`, `co.blueguardian.cerebralstratum.utils.*`.

- Backend `application.yml` quick reference
  - Core toggles:
    - `quarkus.keycloak.devservices.enabled: false` — prevents auto Keycloak; expect to provide your own OIDC endpoint in `%dev` or minimize secured surfaces while developing.
    - `quarkus.datasource.devservices` — pins Postgres image/ports and sets init script. Update if a local Postgres conflicts with port 5432.
  - Messaging topics under `mp.messaging.incoming`: `location`, `status`, `canbus`. If you connect to an external Kafka instead of Dev Services, uncomment and fill the `%dev` Kafka SASL example block.

- Liquibase
  - `%dev` uses `devservices/changeLog.yaml`. There is also a `devservices/changeLogs/test_data_v1.0.0.yaml` and additional initial data wiring. Be cautious when modifying schemas that impact spatial types (Hibernate Spatial is included in `backend`).

- Common pitfalls
  - Mixed Java versions (17 vs 21): ensure you compile and run with JDK 21 to satisfy module requirements.
  - Keycloak availability in dev: endpoints that require OIDC will fail if you do not have a local Keycloak at `:8000` configured to match `%dev`. Consider disabling routes or using mocks during local development, or enabling Keycloak Dev Services explicitly if that fits your workflow.
  - Fixed ports for Dev Services (5432/9092): change or free these ports if you have local services occupying them.

- Useful Maven snippets
  - Speed up rebuilds: `mvn -T1C -DskipITs clean test`
  - Re-run failed tests only: `mvn -pl backend -DtestFailureIgnore=false -DfailIfNoTests=false test`
  - Format dependency tree for a module: `mvn -pl backend -Dincludes=co.blueguardian:* dependency:tree`

#### CI/CD and Docs
- Tekton pipeline definition exists under `.tekton/pipelinerun.yaml` (review for the exact CI expectations and parameters).
- Writerside documentation under `Writerside/topics` contains project overviews and contributing notes; keep these in sync with backend behavior when changing endpoints or security.

#### Verification notes
- The testing instructions above are aligned with the current Maven/Quarkus configuration in the repository. If tests fail to discover/run, double‑check:
  - JDK version is 21.
  - Surefire version (3.5.3) is active and `junit-jupiter` is on the classpath via Quarkus JUnit 5 dependency.
  - For ITs, run with `-DskipITs=false` and ensure dependent services (Keycloak, DB, Kafka) are available or Dev Services are configured appropriately.
