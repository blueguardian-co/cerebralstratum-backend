# device-registrar

Bridges MQTT device traffic to Kafka. Subscribes to `location`, `status`, and `canbus` topics on the MQTT broker and forwards each message as a typed event onto the corresponding Kafka topic for downstream consumption by the primary backend and notification-dispatcher.

## Prerequisites

- Podman (for running the MQTT broker)
- Java 21+
- Maven

## Development Setup

`device-registrar` consumes from MQTT and produces to Kafka. Kafka is managed automatically by Quarkus devservices. MQTT is not — you need to start the broker manually before running the service.

### 1. Start the MQTT broker

From the `device-simulator` directory (the broker config lives there):

```shell script
cd device-simulator && ./start-mqtt.sh
```

This starts an Eclipse Mosquitto container on `localhost:1883`.

### 2. Run in dev mode

```shell script
make dev-registrar
```

Or with automatic secret injection (1Password CLI required):

```shell script
make hack-registrar
```

The Quarkus Dev UI is available at <http://localhost:6444/q/dev/>.

### 3. Stop the MQTT broker

```shell script
cd device-simulator && ./stop-mqtt.sh
```

## Message Flow

```
Device Fleet
    └── MQTT (localhost:1883)
            └── device-registrar
                    └── Kafka (devservice on localhost:9092)
                            ├── Primary Backend
                            └── Notification Dispatcher
```

