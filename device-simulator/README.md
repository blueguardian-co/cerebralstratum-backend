# device-simulator

Simulates IoT devices sending location and status messages via MQTT for development and testing of the CEREBRAL STRATUM backend services.

## Prerequisites

- Podman (for running the MQTT broker)
- Java 21+
- Maven

## Development Setup

The device simulator publishes messages to an MQTT broker. The `device-registrar` service consumes from MQTT and forwards the messages to Kafka, where the primary backend and notification-dispatcher pick them up.

### 1. Start the MQTT broker

```shell script
./start-mqtt.sh
```

This starts an Eclipse Mosquitto container on `localhost:1883`.

### 2. Run in dev mode

```shell script
make dev-simulator
```

Or with automatic secret injection and MQTT lifecycle management (1Password CLI required):

```shell script
make hack-simulator
```

The Quarkus Dev UI is available at <http://localhost:8080/q/dev/>.

### 3. Stop the MQTT broker

```shell script
./stop-mqtt.sh
```

## Message Flow

```
device-simulator
    └── MQTT (localhost:1883)
            └── device-registrar
                    └── Kafka
                            ├── Primary Backend
                            └── Notification Dispatcher
```

The simulator publishes to three MQTT topics:
- `location` — device location updates
- `status` — device status updates
- `canbus` — CAN bus data

