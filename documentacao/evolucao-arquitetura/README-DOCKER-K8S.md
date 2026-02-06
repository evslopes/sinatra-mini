# Guia de Deploy - Docker e Kubernetes

Este guia descreve como executar o Sinatra Mini usando Docker e Kubernetes.

## 📋 Índice

1. [Pré-requisitos](#pré-requisitos)
2. [Docker Compose (Desenvolvimento)](#docker-compose)
3. [Kubernetes (Produção)](#kubernetes)
4. [Scripts Auxiliares](#scripts-auxiliares)
5. [Troubleshooting](#troubleshooting)

---

## Pré-requisitos

### Para Docker Compose
- Docker 20.10+
- Docker Compose 2.0+

### Para Kubernetes
- Kubernetes 1.24+
- kubectl configurado
- Cluster Kubernetes (Minikube, Kind, GKE, EKS, AKS, etc.)
- (Opcional) Helm 3+

---

## 🐳 Docker Compose

### Quick Start

```bash
# 1. Configurar variáveis de ambiente
cp .env.example .env
# Editar .env conforme necessário

# 2. Build e iniciar todos os serviços
docker-compose up -d --build

# 3. Verificar logs
docker-compose logs -f app

# 4. Verificar status
docker-compose ps

# 5. Testar aplicação
curl http://localhost:8080/actuator/health
```

### Serviços Inclusos

- **app** (sinatra-mini:latest): Aplicação Spring Boot na porta 8080
- **postgres** (postgres:15-alpine): Banco de dados na porta 5432
- **redis** (redis:7-alpine): Cache distribuído na porta 6379

### Comandos Úteis

```bash
# Ver logs de um serviço específico
docker-compose logs -f app
docker-compose logs -f postgres

# Reiniciar um serviço
docker-compose restart app

# Recriar serviço após alteração de código
docker-compose up -d --build app

# Parar tudo
docker-compose down

# Parar e remover volumes (⚠️ apaga dados)
docker-compose down -v

# Entrar em um container
docker-compose exec app sh
docker-compose exec postgres psql -U sinatra_user -d sinatra_dev
```

### Variáveis de Ambiente (.env)

```env
# Banco de Dados
DB_PASSWORD=seu_password_seguro

# Redis
REDIS_PASSWORD=seu_redis_password
```

---

## ☸️ Kubernetes

### Arquitetura do Deploy

```
┌─────────────────────────────────────────┐
│           Ingress (NGINX)               │
│     https://api.sinatra-mini.com        │
└──────────────┬──────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  Service: sinatra-app-service (ClusterIP)│
│            Port: 80 → 8080               │
└──────────┬───────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────┐
│    Deployment: sinatra-app (3 replicas)  │
│    + HorizontalPodAutoscaler (3-10)      │
└──┬───────────────────────────────────┬───┘
   │                                   │
   ▼                                   ▼
┌──────────────────┐         ┌──────────────────┐
│  postgres:15     │         │    redis:7       │
│  StatefulSet     │         │  StatefulSet     │
│  + PVC (10Gi)    │         │  + PVC (2Gi)     │
└──────────────────┘         └──────────────────┘
```

### Deploy Completo

#### 1. Build da Imagem

```bash
# Opção A: Build local
./scripts/build-docker.sh

# Opção B: Build com registry
export REGISTRY=gcr.io/seu-projeto
export IMAGE_TAG=v1.1.0
./scripts/build-docker.sh

# Push para registry
docker push $REGISTRY/sinatra-mini:$IMAGE_TAG
```

#### 2. Configurar Manifests

Edite [k8s/kustomization.yaml](k8s/kustomization.yaml):

```yaml
images:
- name: sinatra-mini
  newName: gcr.io/seu-projeto/sinatra-mini  # Seu registry
  newTag: v1.1.0  # Versão da imagem
```

Edite [k8s/secrets.yaml](k8s/secrets.yaml) com senhas reais:

```yaml
stringData:
  DB_PASSWORD: "SUA_SENHA_POSTGRES_FORTE"
  REDIS_PASSWORD: "SUA_SENHA_REDIS_FORTE"
```

> ⚠️ **IMPORTANTE**: Em produção, use ferramentas como:
> - [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets)
> - [External Secrets Operator](https://external-secrets.io/)
> - [HashiCorp Vault](https://www.vaultproject.io/)

#### 3. Deploy

```bash
# Usando script
./scripts/deploy-k8s.sh

# Ou manualmente
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/pvc.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/redis-deployment.yaml
kubectl apply -f k8s/app-deployment.yaml
kubectl apply -f k8s/ingress.yaml

# Usando Kustomize
kubectl apply -k k8s/
```

#### 4. Verificar Deploy

```bash
# Ver todos os recursos
kubectl get all -n sinatra-mini

# Ver pods
kubectl get pods -n sinatra-mini -w

# Ver logs da aplicação
kubectl logs -f deployment/sinatra-app -n sinatra-mini

# Describe pod (troubleshooting)
kubectl describe pod <pod-name> -n sinatra-mini

# Ver eventos
kubectl get events -n sinatra-mini --sort-by='.lastTimestamp'
```

### Acesso Local (Port Forward)

```bash
# Aplicação
kubectl port-forward service/sinatra-app-service 8080:80 -n sinatra-mini
# Acessar: http://localhost:8080

# PostgreSQL (debug)
kubectl port-forward service/postgres-service 5432:5432 -n sinatra-mini
# Conectar: psql -h localhost -U sinatra_user -d sinatra_prod

# Redis (debug)
kubectl port-forward service/redis-service 6379:6379 -n sinatra-mini
# Conectar: redis-cli -h localhost -p 6379 -a <password>
```

### Escalonamento

#### Manual

```bash
# Escalar deployment
kubectl scale deployment sinatra-app --replicas=5 -n sinatra-mini

# Verificar réplicas
kubectl get deployment sinatra-app -n sinatra-mini
```

#### Automático (HPA)

O HorizontalPodAutoscaler está configurado em [k8s/app-deployment.yaml](k8s/app-deployment.yaml):

- **Min replicas**: 3
- **Max replicas**: 10
- **CPU threshold**: 70%
- **Memory threshold**: 80%

```bash
# Ver status do HPA
kubectl get hpa -n sinatra-mini

# Ver detalhes
kubectl describe hpa sinatra-app-hpa -n sinatra-mini
```

### Atualizações (Rolling Update)

```bash
# Atualizar imagem
kubectl set image deployment/sinatra-app \
  sinatra-app=gcr.io/seu-projeto/sinatra-mini:v1.2.0 \
  -n sinatra-mini

# Verificar rollout
kubectl rollout status deployment/sinatra-app -n sinatra-mini

# Histórico de rollouts
kubectl rollout history deployment/sinatra-app -n sinatra-mini

# Rollback
kubectl rollout undo deployment/sinatra-app -n sinatra-mini
```

### Conectar aos Bancos

#### PostgreSQL

```bash
# Via port-forward
kubectl port-forward service/postgres-service 5432:5432 -n sinatra-mini
psql -h localhost -U sinatra_user -d sinatra_prod

# Direto no pod
kubectl exec -it deployment/postgres -n sinatra-mini -- \
  psql -U sinatra_user -d sinatra_prod
```

#### Redis

```bash
# Via port-forward
kubectl port-forward service/redis-service 6379:6379 -n sinatra-mini
redis-cli -h localhost -p 6379 -a <password>

# Direto no pod
kubectl exec -it deployment/redis -n sinatra-mini -- \
  redis-cli -a <password>
```

### Cleanup

```bash
# Usando script
./scripts/cleanup-k8s.sh

# Ou manualmente
kubectl delete -k k8s/

# Deletar namespace (remove tudo)
kubectl delete namespace sinatra-mini
```

---

## 🛠️ Scripts Auxiliares

### build-docker.sh

Build da imagem Docker com suporte a registry.

```bash
# Build local
./scripts/build-docker.sh

# Build com registry
IMAGE_NAME=sinatra-mini \
IMAGE_TAG=v1.1.0 \
REGISTRY=gcr.io/seu-projeto \
./scripts/build-docker.sh
```

### deploy-k8s.sh

Deploy completo no Kubernetes com validação de health.

```bash
# Deploy padrão (namespace: sinatra-mini)
./scripts/deploy-k8s.sh

# Deploy com contexto específico
CONTEXT=gke_project_zone_cluster \
NAMESPACE=sinatra-prod \
./scripts/deploy-k8s.sh
```

### cleanup-k8s.sh

Remove recursos do Kubernetes com confirmação.

```bash
./scripts/cleanup-k8s.sh
```

---

## 🔍 Troubleshooting

### Pod não inicia

```bash
# Ver logs do pod
kubectl logs <pod-name> -n sinatra-mini

# Ver eventos
kubectl describe pod <pod-name> -n sinatra-mini

# Ver eventos do namespace
kubectl get events -n sinatra-mini --sort-by='.lastTimestamp'
```

### Erro ImagePullBackOff

```bash
# Verificar se a imagem existe
docker images | grep sinatra-mini

# Verificar configuração de registry no kustomization.yaml
cat k8s/kustomization.yaml

# Criar secret para registry privado (se necessário)
kubectl create secret docker-registry regcred \
  --docker-server=gcr.io \
  --docker-username=_json_key \
  --docker-password="$(cat key.json)" \
  -n sinatra-mini

# Adicionar secret no deployment
kubectl patch serviceaccount default \
  -p '{"imagePullSecrets": [{"name": "regcred"}]}' \
  -n sinatra-mini
```

### CrashLoopBackOff

```bash
# Ver logs do container que está crashando
kubectl logs <pod-name> -n sinatra-mini --previous

# Verificar health checks
kubectl describe pod <pod-name> -n sinatra-mini | grep -A 10 Liveness

# Ajustar probes se necessário (aumentar initialDelaySeconds)
```

### Banco de dados não conecta

```bash
# Verificar se postgres está rodando
kubectl get pods -l app=postgres -n sinatra-mini

# Verificar logs do postgres
kubectl logs deployment/postgres -n sinatra-mini

# Testar conexão do pod da app
kubectl exec -it deployment/sinatra-app -n sinatra-mini -- \
  sh -c 'apt-get update && apt-get install -y postgresql-client && \
  psql -h postgres-service -U sinatra_user -d sinatra_prod'
```

### Performance Issues

```bash
# Ver uso de recursos
kubectl top pods -n sinatra-mini
kubectl top nodes

# Ver métricas do HPA
kubectl get hpa -n sinatra-mini

# Aumentar recursos se necessário (editar deployment)
kubectl edit deployment sinatra-app -n sinatra-mini
```

### Ingress não funciona

```bash
# Verificar se NGINX Ingress Controller está instalado
kubectl get pods -n ingress-nginx

# Instalar se necessário
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml

# Ver logs do ingress
kubectl logs -n sinatra-mini -l app=sinatra-app

# Verificar ingress
kubectl describe ingress sinatra-app-ingress -n sinatra-mini
```

---

## 📊 Monitoramento

### Health Checks

```bash
# Via port-forward
kubectl port-forward service/sinatra-app-service 8080:80 -n sinatra-mini
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

### Métricas

```bash
# Prometheus endpoint
curl http://localhost:8080/actuator/prometheus
```

---

## 🔐 Segurança

### Secrets Management

**NÃO commitar secrets reais no Git!**

Opções para produção:

1. **Sealed Secrets**:
   ```bash
   kubeseal --format=yaml < k8s/secrets.yaml > k8s/sealed-secrets.yaml
   ```

2. **External Secrets**:
   ```bash
   kubectl apply -f k8s/external-secret.yaml
   ```

3. **Vault**:
   ```bash
   kubectl apply -f k8s/vault-secret.yaml
   ```

### Network Policies

Criar network policies para isolar pods:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: app-network-policy
  namespace: sinatra-mini
spec:
  podSelector:
    matchLabels:
      app: sinatra-app
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: nginx-ingress
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    - podSelector:
        matchLabels:
          app: redis
```

---

## 📚 Referências

- [Docker Documentation](https://docs.docker.com/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [Kustomize](https://kustomize.io/)
