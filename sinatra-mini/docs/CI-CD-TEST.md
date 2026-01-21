# Pipeline de CI/CD - Teste de Validação

## Status: ✅ CI/CD Configurado

Este arquivo foi criado para testar o pipeline de CI/CD do GitHub Actions.

### Workflows Configurados

#### CI (Integração Contínua)
- ✅ Build com Maven
- ✅ Testes unitários
- ✅ Testes de integração
- ✅ Code coverage (JaCoCo)
- ✅ Code quality (SonarCloud)
- ✅ Security scan (OWASP)
- ✅ Build Docker image

**Triggers**: Push/PR em `main`, `develop`, `release/*`, `hotfix/*`

#### CD (Entrega Contínua)
- ✅ Build & Push Docker image (GHCR)
- ✅ Deploy para Staging (branch `develop`)
- ✅ Deploy para Production (branch `main`)
- ✅ Create GitHub Release (tags `v*.*.*`)

**Triggers**: Push em `main`, `develop`, tags `v*.*.*`, manual

### Branches Configuradas (Gitflow)

- `main` → Production (protegida)
- `develop` → Staging/Integration
- `feature/*` → Desenvolvimento de stories
- `release/*` → Preparação de releases
- `hotfix/*` → Correções urgentes em produção

### Teste do Pipeline

Este PR testa:
1. ✅ Build compilando sem erros
2. ✅ Testes passando (53 testes)
3. ✅ Workflow de CI executando
4. ✅ Branch protection rules
5. ✅ Conventional commits

**Data**: 21/01/2026  
**Responsável**: Agente DevOps
