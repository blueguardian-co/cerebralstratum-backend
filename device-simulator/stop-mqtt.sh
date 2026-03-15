#!/usr/bin/env bash
set -euo pipefail

# Stop Mosquitto MQTT broker for CerebralStratum device simulator
echo "Stopping MQTT broker..."

podman container stop cerebralstratum-mqtt
podman container rm cerebralstratum-mqtt

if [ $? -eq 0 ]; then
  echo "✓ MQTT broker stopped"
else
  echo "✗ Failed to stop MQTT broker"
  exit 1
fi
