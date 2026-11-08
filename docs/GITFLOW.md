# Gitflow - Sinatra Mini

## 📋 Visão Geral

Este projeto utiliza **Gitflow** como estratégia de branching para gerenciar o desenvolvimento e releases de forma organizada.

## 🌿 Estrutura de Branches

### Branches Principais (Permanentes)

#### `main`
- **Propósito**: Código em produção
- **Estabilidade**: ✅ Sempre estável e deployável
- **Deploy**: Automático para ambiente de **PRODUÇÃO**
- **Proteção**: Merge apenas via Pull Request aprovado
- **Testes**: CI completo + aprovação manual

#### `develop`
- **Propósito**: Integração de novas features
- **Estabilidade**: 🟡 Deve passar em todos os testes
- **Deploy**: Automático para ambiente de **STAGING**
- **Proteção**: Merge via Pull Request com revisão
- **Testes**: CI completo

---

### Branches de Suporte (Temporárias)

#### `feature/*`
**Formato**: `feature/<issue-number>-<description>`  
**Exemplos**:
- `feature/16-postgresql-setup`
- `feature/20-redis-cache`
- `feature/35-sharding-routing`

**Criação**:
```bash
git checkout develop
git pull origin develop
git checkout -b feature/16-postgresql-setup
```

**Merge**:
```bash
# Via Pull Request para develop
gh pr create --base develop --title "[Story #16] PostgreSQL Setup" --body "..."
```

**Regras**:
- ✅ Sempre derivar de `develop`
- ✅ Merge apenas para `develop` via PR
- ✅ Deletar após merge
- ✅ Commits devem referenciar issue (`git commit -m "feat: add PostgreSQL config (#16)"`)

---

#### `release/*`
**Formato**: `release/<version>`  
**Exemplos**:
- `release/1.1.0`
- `release/1.2.0`

**Criação** (quando `develop` está pronto para release):
```bash
git checkout develop
git pull origin develop
git checkout -b release/1.1.0
```

**Atividades na branch**:
- 🐛 Apenas bug fixes
- 📝 Atualizar CHANGELOG.md
- 🔢 Bumpar versão no pom.xml
- 🧪 Testes finais de homologação

**Merge** (quando validado):
```bash
# 1. Merge para main (produção)
git checkout main
git merge --no-ff release/1.1.0
git tag -a v1.1.0 -m "Release v1.1.0 - PostgreSQL + Redis"
git push origin main --tags

# 2. Merge de volta para develop (propagar fixes)
git checkout develop
git merge --no-ff release/1.1.0
git push origin develop

# 3. Deletar branch de release
git branch -d release/1.1.0
git push origin --delete release/1.1.0
```

---

#### `hotfix/*`
**Formato**: `hotfix/<version>`  
**Exemplos**:
- `hotfix/1.1.1`
- `hotfix/1.2.1`

**Quando usar**: Bug crítico em **PRODUÇÃO** que não pode esperar próximo release

**Criação**:
```bash
git checkout main
git pull origin main
git checkout -b hotfix/1.1.1
```

**Correção**:
```bash
# Fazer fix crítico
git commit -m "fix: corrigir NullPointerException no CSV export (#45)"

# Bumpar versão PATCH (1.1.0 → 1.1.1)
# Atualizar CHANGELOG.md
```

**Merge**:
```bash
# 1. Merge para main
git checkout main
git merge --no-ff hotfix/1.1.1
git tag -a v1.1.1 -m "Hotfix v1.1.1 - Fix CSV export NPE"
git push origin main --tags

# 2. Merge para develop
git checkout develop
git merge --no-ff hotfix/1.1.1
git push origin develop

# 3. Deletar branch
git branch -d hotfix/1.1.1
```

---

## 🔄 Fluxo de Trabalho

### Novo Feature (Story)

```bash
# 1. Criar branch de feature
git checkout develop
git pull origin develop
git checkout -b feature/16-postgresql-setup

# 2. Desenvolver
git add .
git commit -m "feat: add PostgreSQL datasource config (#16)"
git commit -m "test: add PostgreSQL integration tests (#16)"

# 3. Push e criar PR
git push origin feature/16-postgresql-setup
gh pr create --base develop --fill

# 4. Após aprovação e merge, deletar branch local
git checkout develop
git pull origin develop
git branch -d feature/16-postgresql-setup
```

---

### Preparar Release

```bash
# 1. Criar branch de release quando develop estiver pronto
git checkout develop
git pull origin develop
git checkout -b release/1.1.0

# 2. Finalizar preparação
# - Atualizar pom.xml: <version>1.1.0</version>
# - Atualizar CHANGELOG.md
# - Rodar testes finais
./mvnw clean verify

# 3. Commit de versão
git commit -am "chore: bump version to 1.1.0"
git push origin release/1.1.0

# 4. Criar PR para main (code review final)
gh pr create --base main --title "Release v1.1.0" --body "$(cat CHANGELOG.md)"

# 5. Após merge, criar tag
git checkout main
git pull origin main
git tag -a v1.1.0 -m "Release v1.1.0 - PostgreSQL + Redis"
git push origin --tags

# 6. Merge de volta para develop
git checkout develop
git merge --no-ff release/1.1.0
git push origin develop

# 7. Cleanup
git branch -d release/1.1.0
git push origin --delete release/1.1.0
```

---

### Hotfix Urgente

```bash
# 1. Criar branch de hotfix
git checkout main
git pull origin main
git checkout -b hotfix/1.1.1

# 2. Corrigir bug
git commit -m "fix: resolver memory leak no cache (#67)"

# 3. Bumpar versão PATCH
# pom.xml: 1.1.0 → 1.1.1
git commit -am "chore: bump version to 1.1.1"

# 4. Push
git push origin hotfix/1.1.1

# 5. Merge para main via PR (urgente, mas com revisão)
gh pr create --base main --title "Hotfix v1.1.1" --label "hotfix,priority:critical"

# 6. Após merge, tag
git checkout main
git pull origin main
git tag -a v1.1.1 -m "Hotfix v1.1.1"
git push origin --tags

# 7. Propagar para develop
git checkout develop
git merge --no-ff hotfix/1.1.1
git push origin develop
```

---

## 🛡️ Proteção de Branches

### Configurações Recomendadas (GitHub)

**Branch `main`**:
- ✅ Require pull request reviews (mínimo 1 aprovação)
- ✅ Require status checks (CI deve passar)
- ✅ Require branches to be up to date
- ✅ Require signed commits
- ✅ Do not allow bypassing (nem admins)
- ✅ Require deployments to succeed (environment: production)

**Branch `develop`**:
- ✅ Require pull request reviews (mínimo 1 aprovação)
- ✅ Require status checks (CI deve passar)
- ✅ Require branches to be up to date
- ⚠️ Allow admins to bypass (emergências)

---

## 🏷️ Convenção de Commits

Seguir [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

**Types**:
- `feat`: Nova funcionalidade (Story)
- `fix`: Correção de bug
- `docs`: Documentação
- `style`: Formatação (não muda lógica)
- `refactor`: Refatoração
- `perf`: Melhoria de performance
- `test`: Adição/correção de testes
- `chore`: Tarefas de build, CI, etc.

**Exemplos**:
```bash
git commit -m "feat(cache): add Redis L2 cache with 10min TTL (#20)"
git commit -m "fix(csv): resolve encoding issue for special characters (#45)"
git commit -m "docs(roadmap): update Epic 1 timeline"
git commit -m "test(pedido): add integration test for daily sales report"
git commit -m "chore(deps): upgrade Spring Boot to 3.2.12"
```

---

## 🔢 Versionamento (Semantic Versioning)

**Formato**: `MAJOR.MINOR.PATCH`

- **MAJOR** (1.x.x → 2.x.x): Breaking changes
  - Ex: v1.0.0 → v2.0.0 (Sharding - mudança de arquitetura)
  
- **MINOR** (x.1.x → x.2.x): Novas features (backward compatible)
  - Ex: v1.0.0 → v1.1.0 (PostgreSQL + Redis)
  
- **PATCH** (x.x.1 → x.x.2): Bug fixes
  - Ex: v1.1.0 → v1.1.1 (Hotfix para NPE)

---

## 📦 Deploy Automático

| Branch | Ambiente | Trigger | Aprovação |
|--------|----------|---------|-----------|
| `feature/*` | - | - | - |
| `develop` | Staging | Push automático | Não requerida |
| `release/*` | - | - | - |
| `main` | Production | Push automático | **Manual** (GitHub Environments) |
| `hotfix/*` | - | - | - |

---

## 🧪 Testes por Branch

### `feature/*`
```bash
# Rodar localmente antes de push
./mvnw clean verify
```

### `develop` / `release/*` / `hotfix/*`
```bash
# CI roda automaticamente:
- Unit tests
- Integration tests
- Code coverage (JaCoCo)
- Code quality (SonarQube)
- Security scan (OWASP)
```

### `main`
```bash
# Além do CI, também:
- Smoke tests em produção
- Performance benchmarks
- Health checks
```

---

## 📚 Referências

- [Gitflow Original (Vincent Driessen)](https://nvie.com/posts/a-successful-git-branching-model/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Semantic Versioning](https://semver.org/)
- [GitHub Flow vs Gitflow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow)

---

## 🆘 Comandos Úteis

```bash
# Ver branches locais
git branch

# Ver branches remotas
git branch -r

# Deletar branch local
git branch -d feature/16-postgresql-setup

# Deletar branch remota
git push origin --delete feature/16-postgresql-setup

# Listar tags
git tag -l

# Ver diff entre branches
git diff develop...feature/16-postgresql-setup

# Ver histórico gráfico
git log --graph --oneline --all --decorate

# Verificar qual branch você está
git status
```

---

**Próximo Passo**: Criar branch `develop` e fazer primeiro commit
