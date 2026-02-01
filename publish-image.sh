#!/usr/bin/env bash

#./publish-image.sh 3.0.2

set -euo pipefail

# -----------------------------
# Configuration
# -----------------------------
IMAGE_NAME="unisoft123/mockavior"
DEFAULT_TAG="latest"

# Allow tag override: ./publish.sh 0.2.0
TAG="${1:-$DEFAULT_TAG}"

# -----------------------------
# Preconditions
# -----------------------------
command -v docker >/dev/null 2>&1 || { echo "Docker is required but not installed."; exit 1; }
command -v ./gradlew >/dev/null 2>&1 || { echo "Gradle wrapper (./gradlew) not found."; exit 1; }

echo "Publishing Mockavior Docker image"
echo "Image: ${IMAGE_NAME}:${TAG}"
echo

# -----------------------------
# Run tests
# -----------------------------
echo "▶ Running tests..."
./gradlew clean test --no-daemon
echo "✔ Tests passed"
echo

# -----------------------------
# Build application JAR
# -----------------------------
echo "▶ Building application JAR..."
./gradlew bootJar --no-daemon
echo "✔ JAR built"
echo

# -----------------------------
# Build Docker image
# -----------------------------
echo "▶ Building Docker image..."
docker build -t ${IMAGE_NAME}:${TAG} .
echo "✔ Docker image built"
echo

# -----------------------------
# Push to Docker Hub
# -----------------------------
echo "▶ Pushing image to Docker Hub..."
docker push ${IMAGE_NAME}:${TAG}
echo "✔ Image pushed successfully"
echo

# -----------------------------
# Optional: also push 'latest' tag if versioned tag is used
# -----------------------------
if [[ "${TAG}" != "${DEFAULT_TAG}" ]]; then
  echo "▶ Tagging and pushing 'latest'..."
  docker tag ${IMAGE_NAME}:${TAG} ${IMAGE_NAME}:latest
  docker push ${IMAGE_NAME}:latest
  echo "✔ 'latest' tag pushed"
fi

echo
echo "✅ Mockavior image published successfully: ${IMAGE_NAME}:${TAG}"
