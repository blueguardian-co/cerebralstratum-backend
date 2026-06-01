#!/usr/bin/env bash
set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Runnable services (library modules such as utils are excluded)
AVAILABLE_MODULES=("backend" "device-registrar" "notification-dispatcher" "device-simulator")

# Array to store background process PIDs and their module names
declare -A PIDS
MODULES_STARTED=()
MQTT_STARTED=false

# Start the shared MQTT broker (idempotent within a session)
mqtt_start() {
    if [[ "$MQTT_STARTED" == "true" ]]; then
        return 0
    fi
    echo -e "${BLUE}Starting MQTT broker...${NC}"
    podman run -d \
        --name cerebralstratum-mqtt \
        -p 1883:1883 \
        docker.io/library/eclipse-mosquitto:latest \
        sh -c 'printf "listener 1883\nallow_anonymous true\n" > /tmp/mqtt.conf && mosquitto -c /tmp/mqtt.conf' \
        || echo -e "${YELLOW}Warning: MQTT broker start failed (may already be running)${NC}"
    MQTT_STARTED=true
}

# Cleanup function to kill all background processes
cleanup() {
    echo -e "\n${YELLOW}Shutting down services...${NC}"

    if [[ "$MQTT_STARTED" == "true" ]]; then
        echo -e "${BLUE}Stopping MQTT broker...${NC}"
        podman container stop cerebralstratum-mqtt 2>/dev/null && \
            podman container rm cerebralstratum-mqtt 2>/dev/null || \
            echo -e "${YELLOW}MQTT broker cleanup failed or already stopped${NC}"
    fi

    # Kill all background processes
    for module in "${!PIDS[@]}"; do
        pid="${PIDS[$module]}"
        if kill -0 "$pid" 2>/dev/null; then
            echo -e "${BLUE}Stopping $module (PID $pid)${NC}"
            kill "$pid" 2>/dev/null || true
        fi
    done
    wait 2>/dev/null || true
    echo -e "${GREEN}All services stopped${NC}"
    exit 0
}

# Register cleanup function for SIGINT (Ctrl+C) and SIGTERM
trap cleanup INT TERM EXIT

# Function to check if module is valid
is_valid_module() {
    local module=$1
    for valid_module in "${AVAILABLE_MODULES[@]}"; do
        if [[ "$valid_module" == "$module" ]]; then
            return 0
        fi
    done
    return 1
}

# Parse arguments
MODULES=()
while [[ $# -gt 0 ]]; do
    case $1 in
        *)
            MODULES+=("$1")
            shift
            ;;
    esac
done

# Validate all modules before running
for module in "${MODULES[@]}"; do
    if ! is_valid_module "$module"; then
        echo -e "${RED}Error: Invalid module '$module'${NC}"
        echo -e "Available modules: ${AVAILABLE_MODULES[*]}"
        exit 1
    fi
done

# Export secrets
echo -e "${BLUE}Loading secrets...${NC}"
export QUARKUS_OTEL_EXPORTER_OTLP_HEADERS
QUARKUS_OTEL_EXPORTER_OTLP_HEADERS=$(/opt/homebrew/bin/op item get --account 3LZPARPJEFBELCRVCYC7CY7N2M --vault l6qbxfd27ppoeo65ndkrbxlx6i 4sbrziyztyqbvjzyzo4ys5cjua --fields=label=password --reveal)
echo -e "${GREEN}Secrets loaded${NC}"

# Set HOSTNAME if not already set
export HOSTNAME="${HOSTNAME:-$(hostname)}"
echo -e "${BLUE}Using hostname: ${HOSTNAME}${NC}"

# Start each module in background with module-specific setup
for module in "${MODULES[@]}"; do
    # Module-specific pre-start actions
    case "$module" in
        device-registrar|device-simulator)
            mqtt_start
            ;;
    esac

    echo -e "${BLUE}Starting ${module} in dev mode...${NC}"
    (cd "${module}" && ./mvnw quarkus:dev) &
    pid=$!
    PIDS["$module"]=$pid
    MODULES_STARTED+=("$module")
    echo -e "${GREEN}Started ${module} with PID ${pid}${NC}"

    # Module-specific post-start actions
    case "$module" in
        backend)
            echo -e "${BLUE}Opening backend in browser...${NC}"
            sleep 30  # Give the server a moment to start
            open "http://localhost:6443" 2>/dev/null || echo -e "${YELLOW}Could not open browser automatically${NC}"
            ;;
    esac
done

echo -e "\n${YELLOW}All services started. Press Ctrl+C to stop all services.${NC}\n"

# Wait for all background processes
wait