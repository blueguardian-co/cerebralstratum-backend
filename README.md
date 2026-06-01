# cerebral-stratum-backend

[![Dependabot Updates](https://github.com/blueguardian-co/cerebralstratum-backend/actions/workflows/dependabot/dependabot-updates/badge.svg?branch=main)](https://github.com/blueguardian-co/cerebralstratum-backend/actions/workflows/dependabot/dependabot-updates) [![Build documentation](https://github.com/blueguardian-co/cerebralstratum-backend/actions/workflows/build-docs.yaml.yml/badge.svg?branch=main)](https://github.com/blueguardian-co/cerebralstratum-backend/actions/workflows/build-docs.yaml.yml)

Full documentation is published at [blueguardian.co](https://blueguardian.co/cerebralstratum-backend/overview.html).

## Modules

| Module | Description |
|---|---|
| [`backend`](backend/README.md) | Primary REST API — exposes all frontend endpoints, persists Kafka events to PostgreSQL |
| [`device-registrar`](device-registrar/README.md) | Device whole-of-life management — platform registration, user association, firmware updates, and telemetry ingestion |
| [`notification-dispatcher`](notification-dispatcher/README.md) | Kafka consumer that fans out push notifications via FCM / APNs |
| [`device-simulator`](device-simulator/README.md) | Development tool — simulates devices publishing over MQTT |
| [`utils`](utils/README.md) | Shared library JAR — common DTOs, Kafka deserializers, and utilities |

## Local Development

Prerequisites: Java 21+, Maven, Podman (for MQTT-dependent services), [1Password CLI](https://developer.1password.com/docs/cli/) (for `hack-*` targets).

### Individual services

Each service can be started in a separate terminal. Quarkus devservices handle Kafka, PostgreSQL, Redis, and Keycloak automatically.

```shell script
make dev-backend      # port 6443 — Postgres, Keycloak, Kafka devservices auto-start
make dev-registrar    # port 6444 — requires MQTT broker (see below)
make dev-dispatcher   # port 6445 — Kafka, Postgres, Redis devservices auto-start
make dev-simulator    #           — requires MQTT broker (see below)
```

### Services requiring MQTT

`device-registrar` and `device-simulator` depend on an MQTT broker. Start it before running either service:

```shell script
cd device-simulator && ./start-mqtt.sh
```

### Full stack (with secret injection)

The `hack-*` targets wrap `hack_modules.sh`, which loads the OTel credentials from 1Password and manages the MQTT broker lifecycle automatically:

```shell script
make hack-all         # start all four services
make hack-backend     # backend only
make hack-registrar   # device-registrar + MQTT broker
make hack-simulator   # device-simulator + MQTT broker
make hack-dispatcher  # notification-dispatcher
```

### Tests

```shell script
make test             # all modules
make test-backend
make test-registrar
make test-dispatcher
make test-simulator
```
