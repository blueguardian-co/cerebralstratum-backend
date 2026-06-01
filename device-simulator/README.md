# device-simulator

Simulates IoT devices sending location and status messages via MQTT for development and testing of the CEREBRAL STRATUM backend services.

## Prerequisites

- Podman (for running the MQTT broker via `hack_modules.sh`)
- 1Password CLI (`op`) with access to the CEREBRAL STRATUM vault
- Java 21+
- Maven

## Development Setup

The device simulator publishes messages to an MQTT broker. The `device-registrar` service consumes from MQTT and forwards the messages to Kafka, where the primary backend and notification-dispatcher pick them up.

The MQTT broker lifecycle is managed automatically by `hack_modules.sh` — no manual broker steps are required.

### Run in dev mode

```shell script
make hack-simulator
```

This starts an ephemeral Eclipse Mosquitto container on `localhost:1883`, injects secrets from 1Password, and launches the simulator in Quarkus dev mode.

To run multiple services together (e.g. simulator + device-registrar sharing one broker):

```shell script
make hack-all
```

The Quarkus Dev UI is available at <http://localhost:8080/q/dev/>.

## Message Flow

```
device-simulator
    └── MQTT (localhost:1883)
            └── device-registrar
                    └── Kafka
                            ├── Primary Backend
                            └── Notification Dispatcher
```

The simulator publishes to the following MQTT topics:
- `location` — device location updates
- `status` — device status updates
