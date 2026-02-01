#!/usr/bin/env bash
set -euo pipefail

BASE_URL="https://raw.githubusercontent.com/<ORG>/mockavior/main"

echo "⬇️  Downloading Mockavior bootstrap"

curl -fsSL "$BASE_URL/mockavior-up.sh" -o mockavior-up.sh
chmod +x mockavior-up.sh

echo "🚀 Running bootstrap"
./mockavior-up.sh
