# Contributing

If you'd like to contribute to the project, please read the information below.

## Local Development Setup

Prerequisites: Java 21+, Maven, Podman (for MQTT-dependent services).

### Starting individual services

Each service runs in its own terminal. Quarkus devservices handle Kafka, PostgreSQL, Redis, and Keycloak automatically.

```shell script
make dev-backend      # Primary API — port 6443
make dev-registrar    # MQTT → Kafka bridge — port 6444 (requires MQTT broker)
make dev-dispatcher   # Notification fan-out — port 6445
make dev-simulator    # Device simulator (requires MQTT broker)
```

### MQTT broker

`device-registrar` and `device-simulator` both depend on an MQTT broker that Quarkus cannot provide as a devservice. Start it before running either:

```shell script
cd device-simulator && ./start-mqtt.sh   # starts Eclipse Mosquitto on localhost:1883
cd device-simulator && ./stop-mqtt.sh    # stop when done
```

### Full stack with secret injection

The `hack-*` Makefile targets wrap `hack_modules.sh`, which loads OTel credentials from 1Password and manages the MQTT broker lifecycle automatically. Requires the [1Password CLI](https://developer.1password.com/docs/cli/).

```shell script
make hack-all         # all four services
make hack-backend     # backend only
make hack-registrar   # device-registrar + MQTT
make hack-dispatcher  # notification-dispatcher
make hack-simulator   # device-simulator + MQTT
```

### Running tests

```shell script
make test             # all modules
make test-backend     # backend only
make test-registrar
make test-dispatcher
make test-simulator
```
