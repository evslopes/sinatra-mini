#!/bin/bash
# Script para deploy no Kubernetes

set -e

NAMESPACE="${NAMESPACE:-sinatra-mini}"
CONTEXT="${CONTEXT:-}" # Ex: gke_project_zone_cluster

# Usar contexto específico se fornecido
if [ -n "$CONTEXT" ]; then
    echo "🔧 Switching to context: $CONTEXT"
    kubectl config use-context "$CONTEXT"
fi

echo "🚀 Deploying to Kubernetes namespace: $NAMESPACE"

# Criar namespace se não existir
kubectl apply -f k8s/namespace.yaml

# Aplicar recursos na ordem correta
echo "📦 Creating ConfigMaps and Secrets..."
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml

echo "💾 Creating PersistentVolumeClaims..."
kubectl apply -f k8s/pvc.yaml

echo "🗄️  Deploying PostgreSQL..."
kubectl apply -f k8s/postgres-deployment.yaml

echo "⚡ Deploying Redis..."
kubectl apply -f k8s/redis-deployment.yaml

# Aguardar bancos ficarem prontos
echo "⏳ Waiting for database to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n "$NAMESPACE" --timeout=120s
kubectl wait --for=condition=ready pod -l app=redis -n "$NAMESPACE" --timeout=120s

echo "🌐 Deploying Application..."
kubectl apply -f k8s/app-deployment.yaml

echo "🔌 Creating Ingress..."
kubectl apply -f k8s/ingress.yaml

echo "⏳ Waiting for application to be ready..."
kubectl wait --for=condition=ready pod -l app=sinatra-app -n "$NAMESPACE" --timeout=180s

echo "✅ Deployment completed!"
echo ""
echo "📊 Deployment status:"
kubectl get all -n "$NAMESPACE"

echo ""
echo "🔍 To check logs:"
echo "   kubectl logs -f deployment/sinatra-app -n $NAMESPACE"
echo ""
echo "🌐 To access the application:"
echo "   kubectl port-forward service/sinatra-app-service 8080:80 -n $NAMESPACE"
