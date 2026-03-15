#!/usr/bin/env bash
set -euo pipefail

# Start Mosquitto MQTT broker for CerebralStratum device simulator
echo "Starting MQTT broker..."

podman run -d \
  --name cerebralstratum-mqtt \
  -p 1883:1883 \
  -v "$(pwd)/mosquitto.conf:/mosquitto/config/mosquitto.conf:Z" \
  docker.io/library/eclipse-mosquitto:latest

if [ $? -eq 0 ]; then
  echo "✓ MQTT broker started on localhost:1883"
else
  echo "✗ Failed to start MQTT broker"
  exit 1
fi
