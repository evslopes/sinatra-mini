#!/bin/bash
# Script para build da imagem Docker

set -e

# Variáveis
IMAGE_NAME="${IMAGE_NAME:-sinatra-mini}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
REGISTRY="${REGISTRY:-}" # Ex: gcr.io/project-id

# Build
echo "🏗️  Building Docker image: $IMAGE_NAME:$IMAGE_TAG"
docker build -t "$IMAGE_NAME:$IMAGE_TAG" .

# Tag com registry se especificado
if [ -n "$REGISTRY" ]; then
    FULL_IMAGE="$REGISTRY/$IMAGE_NAME:$IMAGE_TAG"
    echo "🏷️  Tagging image: $FULL_IMAGE"
    docker tag "$IMAGE_NAME:$IMAGE_TAG" "$FULL_IMAGE"
fi

echo "✅ Build completed successfully!"
echo "   Local image: $IMAGE_NAME:$IMAGE_TAG"
[ -n "$REGISTRY" ] && echo "   Registry image: $FULL_IMAGE"
