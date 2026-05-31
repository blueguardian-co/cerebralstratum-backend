# backend

Primary REST API service for the CEREBRAL STRATUM platform. Exposes all endpoints consumed by the frontend, persists GPS and device status events from Kafka to PostgreSQL, and serves notification and entitlement data.

## Prerequisites

- Java 21+
- Maven

## Development Setup

PostgreSQL (PostGIS), Keycloak, and Kafka devservices start automatically in dev mode — no manual setup required.

### Run in dev mode

```shell script
make dev-backend
```

Or with automatic secret injection (1Password CLI required):

```shell script
make hack-backend
```

The Quarkus Dev UI is available at <http://localhost:6443/q/dev/>.

