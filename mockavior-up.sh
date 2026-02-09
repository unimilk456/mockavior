#!/usr/bin/env bash
set -euo pipefail

MOCKAVIOR_VERSION="latest"
COMPOSE_FILE="docker-compose.prebuilt.yml"
CONFIG_DIR="./config"
CONTRACT_FILE="$CONFIG_DIR/mockapi.yml"

echo "🧩 Mockavior bootstrap starting..."

# --- checks -------------------------------------------------------

command -v docker >/dev/null 2>&1 || {
  echo "❌ Docker is not installed"
  exit 1
}

command -v docker-compose >/dev/null 2>&1 || {
  docker compose version >/dev/null 2>&1 || {
    echo "❌ Docker Compose is not available"
    exit 1
  }
}

# --- config dir ---------------------------------------------------

if [ ! -d "$CONFIG_DIR" ]; then
  echo "📁 Creating config directory $CONFIG_DIR"
  mkdir -p "$CONFIG_DIR"
fi

# --- contract -----------------------------------------------------

if [ ! -f "$CONTRACT_FILE" ]; then
  echo "📄 Contract not found, creating minimal mockapi.yml"

  cat > "$CONTRACT_FILE" <<EOF
version: 1

settings:
  mode: STRICT
  defaultStatus: 404

endpoints:
  - id: health
    request:
      method: GET
      path: /health
    response:
      type: mock
      status: 200
      body: "OK"
EOF
else
  echo "📄 Contract already exists: $CONTRACT_FILE"
fi

# --- compose file -------------------------------------------------

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "⬇️  Downloading docker-compose.prebuilt.yml"
  curl -fsSL \
    https://raw.githubusercontent.com/unimilk456/mockavior/main/docker-compose.prebuilt.yml \
    -o "$COMPOSE_FILE"
fi

# --- run ----------------------------------------------------------

echo "🚀 Starting Mockavior stack"
docker compose -f "$COMPOSE_FILE" up
