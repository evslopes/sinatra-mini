#!/bin/bash
# Script para remover recursos do Kubernetes

set -e

NAMESPACE="${NAMESPACE:-sinatra-mini}"

echo "🗑️  Removing Kubernetes resources from namespace: $NAMESPACE"

# Remover na ordem reversa
kubectl delete -f k8s/ingress.yaml --ignore-not-found=true
kubectl delete -f k8s/app-deployment.yaml --ignore-not-found=true
kubectl delete -f k8s/redis-deployment.yaml --ignore-not-found=true
kubectl delete -f k8s/postgres-deployment.yaml --ignore-not-found=true
kubectl delete -f k8s/pvc.yaml --ignore-not-found=true
kubectl delete -f k8s/secrets.yaml --ignore-not-found=true
kubectl delete -f k8s/configmap.yaml --ignore-not-found=true

read -p "⚠️  Delete namespace '$NAMESPACE'? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    kubectl delete namespace "$NAMESPACE"
    echo "✅ Namespace deleted"
else
    echo "ℹ️  Namespace preserved"
fi

echo "✅ Cleanup completed!"
