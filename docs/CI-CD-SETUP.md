# Configuração CI/CD - Sinatra Mini

## 📋 Resumo Executivo

Pipeline de CI/CD configurado com GitHub Actions seguindo **Gitflow** para automação completa do ciclo de desenvolvimento.

**Status**: ✅ Operacional  
**Data**: 21/01/2026  
**PR de Teste**: [#45](https://github.com/evslopes/sinatra-mini/pull/45)

---

## 🏗️ Arquitetura do Pipeline

```
┌─────────────┐
│   Develop   │──────┐
│  (Feature)  │      │
└─────────────┘      │
                     ▼
              ┌─────────────┐      ┌──────────────┐
              │   CI Build  │──────│   All Tests  │
              │   & Test    │      │   Pass ✓     │
              └─────────────┘      └──────────────┘
                     │
                     ▼
              ┌─────────────┐
              │  Code       │
              │  Quality    │
              └─────────────┘
                     │
                     ▼
              ┌─────────────┐      ┌──────────────┐
              │   Merge to  │──────│  Deploy to   │
              │   develop   │      │   Staging    │
              └─────────────┘      └──────────────┘
                     │
                     ▼
              ┌─────────────┐      ┌──────────────┐
              │   Release   │──────│  Deploy to   │
              │   to main   │      │  Production  │
              └─────────────┘      └──────────────┘
```

---

## 🔧 Workflows Configurados

### 1. CI Workflow (`.github/workflows/ci.yml`)

**Triggers**:
- Push em: `main`, `develop`, `release/*`, `hotfix/*`
- Pull Request para: `main`, `develop`, `release/*`

**Jobs**:

#### 1.1. Build & Test
- ✅ Checkout do código
- ✅ Setup JDK 17 (Temurin)
- ✅ Cache de dependências Maven
- ✅ Compilação: `./mvnw clean compile`
- ✅ Testes unitários: `./mvnw test`
- ✅ Testes de integração: `./mvnw verify -P integration-tests`
- ✅ Code coverage: JaCoCo
- ✅ Upload para Codecov
- ✅ Publicação de resultados de testes
- ✅ Archive de reports em caso de falha

**Matrix Strategy**: Testa em Java 17 (expansível para múltiplas versões)

#### 1.2. Code Quality Analysis
- ✅ SonarCloud Scan
- ✅ Métricas de qualidade
- ✅ Detecção de code smells
- ✅ Análise de duplicação

**Dependência**: Requer job `build-and-test` completo

#### 1.3. Security Scan
- ✅ OWASP Dependency Check
- ✅ Scan de vulnerabilidades CVE
- ✅ Report HTML gerado
- ✅ Artifact uploadado (retenção: 7 dias)

**Dependência**: Requer job `build-and-test` completo

#### 1.4. Build Docker Image (Validation)
- ✅ Setup Docker Buildx
- ✅ Build da imagem Docker
- ✅ Cache otimizado (GitHub Actions cache)
- ✅ **Sem push** (apenas validação)

**Condicional**: Apenas em Pull Requests

---

### 2. CD Workflow (`.github/workflows/cd.yml`)

**Triggers**:
- Push em: `main`, `develop`
- Tags: `v*.*.*`
- Manual: `workflow_dispatch`

**Jobs**:

#### 2.1. Build & Push Docker Image
- ✅ Build do JAR: `./mvnw clean package -DskipTests`
- ✅ Login no GitHub Container Registry (GHCR)
- ✅ Metadata extraction (versioning automático)
- ✅ Build multi-arch: `linux/amd64`, `linux/arm64`
- ✅ Push para `ghcr.io/evslopes/sinatra-mini`
- ✅ Cache otimizado

**Tags geradas**:
- `develop` → `ghcr.io/.../sinatra-mini:develop`
- `main` → `ghcr.io/.../sinatra-mini:main`
- `v1.1.0` → `ghcr.io/.../sinatra-mini:1.1.0`, `:1.1`, `:latest`

#### 2.2. Deploy to Staging
- ✅ Acionado em push para `develop`
- ✅ Environment: `staging`
- ✅ Smoke tests
- ⏳ **Placeholder** - implementar deploy real

**URL**: https://staging.sinatra.com

#### 2.3. Deploy to Production
- ✅ Acionado em push para `main`
- ✅ Environment: `production` (aprovação manual)
- ✅ Smoke tests
- ✅ Notificação de deploy
- ⏳ **Placeholder** - implementar deploy real

**URL**: https://api.sinatra.com

#### 2.4. Create GitHub Release
- ✅ Acionado em tags `v*.*.*`
- ✅ Geração automática de changelog
- ✅ Comparação com versão anterior
- ✅ Informações da imagem Docker
- ✅ Link para full changelog

---

## 🌿 Gitflow Implementado

### Branches Principais

| Branch | Propósito | Deploy | Proteção |
|--------|-----------|--------|----------|
| `main` | Produção | Automático → Production | ✅ PR + Aprovação |
| `develop` | Integração | Automático → Staging | ✅ PR |

### Branches de Suporte

| Tipo | Formato | Base | Merge para |
|------|---------|------|------------|
| Feature | `feature/<issue>-<desc>` | `develop` | `develop` |
| Release | `release/<version>` | `develop` | `main` + `develop` |
| Hotfix | `hotfix/<version>` | `main` | `main` + `develop` |

### Fluxo de Trabalho

```bash
# 1. Nova Feature
git checkout develop
git checkout -b feature/16-postgresql-setup
# ... desenvolvimento ...
gh pr create --base develop

# 2. Release
git checkout develop
git checkout -b release/1.1.0
# ... preparação (bump version, CHANGELOG) ...
gh pr create --base main
# Após merge em main:
git tag -a v1.1.0 -m "Release v1.1.0"
git push origin --tags

# 3. Hotfix
git checkout main
git checkout -b hotfix/1.1.1
# ... correção urgente ...
gh pr create --base main
# Após merge, também merge para develop
```

---

## 📝 Conventional Commits

**Formato**: `<type>(<scope>): <subject>`

**Types principais**:
- `feat`: Nova funcionalidade
- `fix`: Correção de bug
- `test`: Adição/correção de testes
- `docs`: Documentação
- `chore`: Tarefas de build/CI
- `refactor`: Refatoração
- `perf`: Melhoria de performance

**Exemplos**:
```bash
git commit -m "feat(cache): add Redis L2 cache (#20)"
git commit -m "fix(csv): resolve encoding issue (#45)"
git commit -m "test(pedido): add integration test for reports"
```

---

## 🔒 Branch Protection (Recomendado)

### Para `main`:
- ✅ Require pull request reviews (mínimo 1)
- ✅ Require status checks to pass (CI completo)
- ✅ Require branches to be up to date
- ✅ Require deployments to succeed (production)
- ✅ Do not allow bypassing

### Para `develop`:
- ✅ Require pull request reviews (mínimo 1)
- ✅ Require status checks to pass (CI completo)
- ⚠️ Allow admins to bypass (emergências)

---

## 🧪 Testes Executados

### Unit Tests (53 testes)
- Entity annotations
- Service logic
- Controller error handling
- Validation rules

### Integration Tests
- CRUD operations
- CSV export
- Performance benchmarks
- Schema validation
- Business reports (daily sales, weekly average, etc.)

### Code Coverage
- Target: > 80%
- Tool: JaCoCo
- Upload: Codecov
- Trend tracking

### Code Quality
- Tool: SonarCloud
- Métricas: bugs, vulnerabilities, code smells, duplicação
- Quality Gate enforcement

### Security Scan
- Tool: OWASP Dependency Check
- CVE detection
- Vulnerability reporting

---

## 📦 Artifacts Gerados

### Por Build:
- JAR executable: `target/sinatra-mini-1.0.0.jar`
- Test reports: `target/surefire-reports/`, `target/failsafe-reports/`
- Coverage report: `target/site/jacoco/`
- Dependency check: `target/dependency-check-report.html`

### Docker Images:
- Registry: `ghcr.io/evslopes/sinatra-mini`
- Platforms: `linux/amd64`, `linux/arm64`
- Tags: `develop`, `main`, versões semânticas

---

## 🚀 Como Usar

### Desenvolvimento de Nova Feature

```bash
# 1. Criar branch
git checkout develop
git pull origin develop
git checkout -b feature/20-redis-cache

# 2. Desenvolver
# ... código ...

# 3. Commitar (Conventional Commits)
git add .
git commit -m "feat(cache): implement Redis L2 cache (#20)"

# 4. Push e criar PR
git push origin feature/20-redis-cache
gh pr create --base develop --fill

# 5. Aguardar CI passar
# 6. Merge após aprovação
```

### Preparar Release

```bash
# 1. Criar branch de release
git checkout develop
git checkout -b release/1.1.0

# 2. Bump version
# Editar pom.xml: <version>1.1.0</version>
# Atualizar CHANGELOG.md

# 3. Commit e push
git commit -am "chore: bump version to 1.1.0"
git push origin release/1.1.0

# 4. PR para main
gh pr create --base main --title "Release v1.1.0"

# 5. Após merge, criar tag
git checkout main
git pull origin main
git tag -a v1.1.0 -m "Release v1.1.0 - PostgreSQL + Redis"
git push origin --tags

# 6. Merge de volta para develop
git checkout develop
git merge --no-ff release/1.1.0
git push origin develop
```

### Deploy Manual (Workflow Dispatch)

```bash
# Via GitHub CLI
gh workflow run cd.yml --ref main

# Ou via UI do GitHub:
# Actions → CD - Deploy → Run workflow → Select branch
```

---

## 🔍 Monitoramento do Pipeline

### Status do CI
```bash
# Ver status dos checks do PR
gh pr checks <PR-NUMBER>

# Ver runs do workflow
gh run list --workflow=ci.yml

# Ver logs de um run específico
gh run view <RUN-ID> --log
```

### Status do CD
```bash
# Ver deployments
gh api repos/:owner/:repo/deployments

# Ver status de um deployment
gh api repos/:owner/:repo/deployments/<ID>/statuses
```

---

## 📊 Métricas Coletadas

### Build
- ✅ Tempo de compilação
- ✅ Tamanho do JAR
- ✅ Dependências transitivas

### Testes
- ✅ Total de testes
- ✅ Taxa de sucesso
- ✅ Tempo de execução
- ✅ Code coverage (%)

### Code Quality
- ✅ Bugs
- ✅ Vulnerabilities
- ✅ Code smells
- ✅ Duplicação (%)
- ✅ Technical debt

### Security
- ✅ CVEs detectados
- ✅ Severity (Critical/High/Medium/Low)
- ✅ Versões afetadas

---

## 🔄 Próximas Melhorias

### Curto Prazo
- [ ] Configurar SonarCloud (secrets.SONAR_TOKEN)
- [ ] Implementar deploy real para Staging
- [ ] Implementar deploy real para Production
- [ ] Configurar notificações Slack/Teams
- [ ] Adicionar health checks pós-deploy

### Médio Prazo
- [ ] Performance testing (JMeter/Gatling)
- [ ] E2E testing (Cypress/Selenium)
- [ ] Canary deployments
- [ ] Blue-green deployments
- [ ] Automatic rollback

### Longo Prazo
- [ ] GitOps (ArgoCD/Flux)
- [ ] Infrastructure as Code (Terraform)
- [ ] Kubernetes deployment
- [ ] Multi-region deployment
- [ ] Chaos engineering (Chaos Monkey)

---

## 📚 Documentação Relacionada

- [GITFLOW.md](GITFLOW.md) - Guia completo do Gitflow
- [ROADMAP-ARQUITETURA.md](../documentacao/evolucao-arquitetura/ROADMAP-ARQUITETURA.md) - Roadmap de evolução
- [QA-TEST-REPORT.md](QA-TEST-REPORT.md) - Baseline de qualidade

---

## 🆘 Troubleshooting

### CI falha em "Build and Test"
```bash
# Rodar localmente para debug
./mvnw clean verify

# Verificar logs do workflow
gh run view <RUN-ID> --log
```

### Docker build falha
```bash
# Testar build local
docker build -t sinatra-mini:test .

# Verificar Dockerfile
cat Dockerfile
```

### Deploy falha
```bash
# Verificar secrets configurados
gh secret list

# Verificar environments
gh api repos/:owner/:repo/environments
```

### Workflow não dispara
```bash
# Verificar sintaxe YAML
yamllint .github/workflows/ci.yml

# Verificar eventos de trigger
gh run list --workflow=ci.yml
```

---

**Responsável**: Agente DevOps  
**Última Atualização**: 21/01/2026  
**Próximo Review**: Q2 2026 (Epic 1 - PostgreSQL)
