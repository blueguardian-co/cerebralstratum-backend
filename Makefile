.PHONY: build \
        dev-backend dev-registrar dev-dispatcher dev-simulator \
        hack-backend hack-registrar hack-dispatcher hack-simulator hack-all \
        test test-backend test-registrar test-dispatcher test-simulator

# Build all modules (skip tests)
build:
	./mvnw install -DskipTests

# --- Dev mode (run each in a separate terminal; env vars must be pre-exported) ---
# For automatic secret injection and MQTT lifecycle management use the hack-* targets below.

# Main API service — Postgres, Keycloak, and Kafka devservices spin up automatically
dev-backend:
	./mvnw quarkus:dev -pl backend -am

# MQTT → Kafka bridge — start MQTT broker first (see hack-registrar)
dev-registrar:
	./mvnw quarkus:dev -pl device-registrar -am

# Kafka consumer + notification fan-out — Kafka, Postgres, and Redis devservices spin up automatically
dev-dispatcher:
	./mvnw quarkus:dev -pl notification-dispatcher -am

# Device simulator — start MQTT broker first (see hack-simulator)
dev-simulator:
	./mvnw quarkus:dev -pl device-simulator -am

# --- hack_modules.sh shortcuts (loads 1Password secrets + manages MQTT lifecycle) ---

hack-backend:
	./hack_modules.sh backend

hack-registrar:
	./hack_modules.sh device-registrar

hack-dispatcher:
	./hack_modules.sh notification-dispatcher

hack-simulator:
	./hack_modules.sh device-simulator

# Start all services
hack-all:
	./hack_modules.sh backend device-registrar notification-dispatcher device-simulator

# --- Tests ---

# Run tests for all modules
test:
	./mvnw test

test-backend:
	./mvnw test -pl backend -am

test-registrar:
	./mvnw test -pl device-registrar -am

test-dispatcher:
	./mvnw test -pl notification-dispatcher -am

test-simulator:
	./mvnw test -pl device-simulator -am
