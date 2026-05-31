# notification-dispatcher

Consumes device and system events from Kafka and fans out push notifications to end users and operators via FCM (Android + Web) and APNs (iOS). Writes notification records and per-recipient delivery rows to the appropriate tenant schema in PostgreSQL.

## Prerequisites

- Java 21+
- Maven

## Development Setup

Kafka, PostgreSQL, and Redis devservices start automatically in dev mode — no manual setup required.

### Run in dev mode

```shell script
make dev-dispatcher
```

Or with automatic secret injection (1Password CLI required):

```shell script
make hack-dispatcher
```

The Quarkus Dev UI is available at <http://localhost:6445/q/dev/>.

## Message Flow

```
Kafka
    ├── location topic  ──┐
    └── status topic   ──┴── notification-dispatcher → PostgreSQL (notifications + recipients)
                                                     → FCM / APNs (push dispatch)
```

