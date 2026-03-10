#!/usr/bin/env bash
set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Available modules with Dockerfiles
AVAILABLE_MODULES=("backend" "device-data-persist" "device-simulator" "ingress-classifier" "utils")

# Default values
IMAGE_TAG="${IMAGE_TAG:-latest}"
IMAGE_REGISTRY="${IMAGE_REGISTRY:-quarkus}"
BUILD_ARGS=""
CONTAINER_TOOL="${CONTAINER_TOOL:-podman}"

# Function to print usage
usage() {
    echo "Usage: $0 [OPTIONS] [MODULES...]"
    echo ""
    echo "Build container images for one or more Quarkus modules"
    echo ""
    echo "Options:"
    echo "  -t, --tag TAG          Image tag (default: latest)"
    echo "  -r, --registry NAME    Image registry/prefix (default: quarkus)"
    echo "  -c, --container TOOL   Container tool: podman or docker (default: podman)"
    echo "  -a, --all              Build all modules"
    echo "  -s, --skip-build       Skip Maven build (use existing target/)"
    echo "  -h, --help             Show this help message"
    echo ""
    echo "Available modules:"
    for module in "${AVAILABLE_MODULES[@]}"; do
        echo "  - $module"
    done
    echo ""
    echo "Examples:"
    echo "  $0 backend                                    # Build backend module"
    echo "  $0 backend device-simulator                   # Build specific modules"
    echo "  $0 --all                                      # Build all modules"
    echo "  $0 -t v1.0.0 -r myregistry backend           # Custom tag and registry"
    echo "  $0 --skip-build backend                       # Skip Maven, build image only"
    exit 1
}

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

# Function to build a single module
build_module() {
    local module=$1
    local dockerfile="$module/src/main/docker/Dockerfile.jvm"
    local image_name="${IMAGE_REGISTRY}/${module}:${IMAGE_TAG}"

    echo -e "${GREEN}Building module: $module${NC}"

    # Check if Dockerfile exists
    if [[ ! -f "$dockerfile" ]]; then
        echo -e "${RED}Error: Dockerfile not found at $dockerfile${NC}"
        return 1
    fi

    # Build Maven package if not skipped
    if [[ "$SKIP_BUILD" != "true" ]]; then
        echo -e "${YELLOW}Building Maven package for $module...${NC}"
        ./mvnw clean package -pl "$module" -am -DskipTests
    else
        echo -e "${YELLOW}Skipping Maven build for $module${NC}"
    fi

    # Check if target directory exists
    if [[ ! -d "$module/target/quarkus-app" ]]; then
        echo -e "${RED}Error: $module/target/quarkus-app not found. Run Maven build first.${NC}"
        return 1
    fi

    # Build container image
    echo -e "${YELLOW}Building container image with $CONTAINER_TOOL: $image_name${NC}"
    $CONTAINER_TOOL build -f "$dockerfile" -t "$image_name" "$module/" $BUILD_ARGS

    echo -e "${GREEN}Successfully built: $image_name${NC}"
    echo ""
}

# Parse arguments
MODULES=()
BUILD_ALL=false
SKIP_BUILD=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--tag)
            IMAGE_TAG="$2"
            shift 2
            ;;
        -r|--registry)
            IMAGE_REGISTRY="$2"
            shift 2
            ;;
        -c|--container)
            CONTAINER_TOOL="$2"
            shift 2
            ;;
        -a|--all)
            BUILD_ALL=true
            shift
            ;;
        -s|--skip-build)
            SKIP_BUILD=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        -*)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            ;;
        *)
            MODULES+=("$1")
            shift
            ;;
    esac
done

# Determine which modules to build
if [[ "$BUILD_ALL" == "true" ]]; then
    MODULES=("${AVAILABLE_MODULES[@]}")
elif [[ ${#MODULES[@]} -eq 0 ]]; then
    echo -e "${RED}Error: No modules specified${NC}"
    usage
fi

# Validate all modules before building
for module in "${MODULES[@]}"; do
    if ! is_valid_module "$module"; then
        echo -e "${RED}Error: Invalid module '$module'${NC}"
        usage
    fi
done

# Validate container tool
if ! command -v "$CONTAINER_TOOL" &> /dev/null; then
    echo -e "${RED}Error: $CONTAINER_TOOL is not installed or not in PATH${NC}"
    exit 1
fi

# Build each module
echo -e "${GREEN}Starting build process...${NC}"
echo -e "Container tool: $CONTAINER_TOOL"
echo -e "Registry: $IMAGE_REGISTRY"
echo -e "Tag: $IMAGE_TAG"
echo -e "Modules: ${MODULES[*]}"
echo ""

FAILED_MODULES=()
SUCCESS_MODULES=()

for module in "${MODULES[@]}"; do
    if build_module "$module"; then
        SUCCESS_MODULES+=("$module")
    else
        FAILED_MODULES+=("$module")
    fi
done

# Print summary
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Build Summary${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "Successful builds: ${#SUCCESS_MODULES[@]}"
for module in "${SUCCESS_MODULES[@]}"; do
    echo -e "  ${GREEN}✓${NC} $module"
done

if [[ ${#FAILED_MODULES[@]} -gt 0 ]]; then
    echo -e "Failed builds: ${#FAILED_MODULES[@]}"
    for module in "${FAILED_MODULES[@]}"; do
        echo -e "  ${RED}✗${NC} $module"
    done
    exit 1
fi

echo -e "${GREEN}All builds completed successfully!${NC}"
