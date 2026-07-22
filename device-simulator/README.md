# device-simulator

Simulates IoT devices sending location and status messages via MQTT for development and testing of the CEREBRAL STRATUM backend services.

## Prerequisites

- Podman (for running the MQTT broker via `hack_modules.sh`)
- 1Password CLI (`op`) with access to the CEREBRAL STRATUM vault
- Java 21+
- Maven

## Development Setup

The device simulator publishes location/status/canbus messages to an MQTT broker, the
same way a real device would. It also runs a small dev-only bridge that subscribes to
those same MQTT topics and republishes onto the Kafka topics backend's
`DeviceStreamService` consumes (`device.location`, `device.status`, `device.canbus`),
keyed by device UUID.

This bridge stands in for Eclipse Hono, the edge middleware that will own MQTT-to-Kafka
bridging (and device onboarding/association, handled separately by `device-registrar`)
in the real deployment per the ADRs. Until Hono is wired up, running the simulator is
enough to get realistic device data flowing all the way to the backend's gRPC stream
and database for local testing.

The MQTT broker lifecycle is managed automatically by `hack_modules.sh` — no manual broker steps are required.

### Run in dev mode

```shell script
make hack-simulator
```

This starts an ephemeral Eclipse Mosquitto container on `localhost:1883`, injects secrets from 1Password, and launches the simulator in Quarkus dev mode.

To run alongside the backend (sharing the Kafka Dev Service):

```shell script
make hack-all
```

The Quarkus Dev UI is available at <http://localhost:8080/q/dev/>.

## Message Flow

```
device-simulator
    ├── MQTT (localhost:1883) — publishes location/status/canbus, as a real device would
    └── DeviceMessageBridge (dev-only, stands in for Eclipse Hono)
            └── subscribes to the same MQTT topics
                    └── Kafka: device.location / device.status / device.canbus
                            └── backend DeviceStreamService -> gRPC stream + DB
```

The simulator publishes to the following MQTT topics:
- `location` — device location updates
- `status` — device status updates
- `canbus` — CAN bus frames
