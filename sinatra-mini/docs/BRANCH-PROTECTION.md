# Branch Protection Rules - Sinatra Mini

## ✅ Status: Configurado

As regras de proteção foram aplicadas com sucesso nas branches `main` e `develop`.

**Data**: 21/01/2026

---

## 🛡️ Branch: `main` (Production)

### Configurações Aplicadas

| Regra | Status | Descrição |
|-------|--------|-----------|
| **Pull Request Reviews** | ✅ Ativo | Mínimo 1 aprovação obrigatória |
| **Dismiss Stale Reviews** | ✅ Ativo | Aprovações anteriores são invalidadas em novos commits |
| **Enforce Admins** | ✅ Ativo | Regras aplicam-se inclusive para administradores |
| **Allow Force Pushes** | ❌ Bloqueado | Force push não permitido |
| **Allow Deletions** | ❌ Bloqueado | Deletar branch não permitido |
| **Required Status Checks** | ⏳ Pendente | A ser configurado após primeiro workflow executar |

### Proteções Ativas

```json
{
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 1
  },
  "enforce_admins": {
    "enabled": true
  },
  "allow_force_pushes": {
    "enabled": false
  },
  "allow_deletions": {
    "enabled": false
  }
}
```

### O que está protegido

- ✅ Ninguém pode fazer push direto (nem admins)
- ✅ Todo merge requer Pull Request
- ✅ Todo PR requer pelo menos 1 aprovação
- ✅ Aprovações antigas são invalidadas em novos commits
- ✅ Force push bloqueado
- ✅ Deletar branch bloqueado

### Como fazer deploy em produção

```bash
# 1. Criar PR de release para main
git checkout develop
git checkout -b release/1.1.0
gh pr create --base main --title "Release v1.1.0"

# 2. Aguardar aprovação (mínimo 1 reviewer)
# 3. Aguardar CI passar (quando status checks configurados)
# 4. Merge do PR (squash ou merge commit)
# 5. Tag automática via CD workflow
```

---

## 🛡️ Branch: `develop` (Staging/Integration)

### Configurações Aplicadas

| Regra | Status | Descrição |
|-------|--------|-----------|
| **Pull Request Reviews** | ✅ Ativo | Mínimo 1 aprovação obrigatória |
| **Dismiss Stale Reviews** | ✅ Ativo | Aprovações anteriores são invalidadas em novos commits |
| **Enforce Admins** | ⚠️ Desabilitado | Admins podem fazer bypass em emergências |
| **Allow Force Pushes** | ❌ Bloqueado | Force push não permitido |
| **Allow Deletions** | ❌ Bloqueado | Deletar branch não permitido |
| **Required Status Checks** | ⏳ Pendente | A ser configurado após primeiro workflow executar |

### Proteções Ativas

```json
{
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 1
  },
  "enforce_admins": {
    "enabled": false
  },
  "allow_force_pushes": {
    "enabled": false
  },
  "allow_deletions": {
    "enabled": false
  }
}
```

### O que está protegido

- ✅ Push direto requer PR
- ✅ Todo PR requer pelo menos 1 aprovação
- ✅ Aprovações antigas são invalidadas em novos commits
- ⚠️ Admins podem fazer bypass (para emergências)
- ✅ Force push bloqueado (exceto admins)
- ✅ Deletar branch bloqueado (exceto admins)

### Como integrar features

```bash
# 1. Criar PR de feature para develop
git checkout develop
git checkout -b feature/16-postgresql-setup
# ... desenvolvimento ...
gh pr create --base develop --fill

# 2. Aguardar aprovação (mínimo 1 reviewer)
# 3. Aguardar CI passar (quando status checks configurados)
# 4. Merge do PR
# 5. Deploy automático para staging via CD workflow
```

---

## 🔄 Próximos Passos: Status Checks

Após o primeiro workflow CI executar com sucesso, configure status checks obrigatórios:

### Para `main`:

```bash
# Atualizar proteção com status checks
cat > /tmp/main-protection-checks.json << 'EOF'
{
  "required_status_checks": {
    "strict": true,
    "checks": [
      {"context": "build-and-test"},
      {"context": "code-quality"},
      {"context": "security-scan"}
    ]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 1
  },
  "restrictions": null
}
EOF

gh api --method PUT repos/evslopes/sinatra-mini/branches/main/protection \
  --input /tmp/main-protection-checks.json
```

### Para `develop`:

```bash
# Atualizar proteção com status checks
cat > /tmp/develop-protection-checks.json << 'EOF'
{
  "required_status_checks": {
    "strict": true,
    "checks": [
      {"context": "build-and-test"}
    ]
  },
  "enforce_admins": false,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 1
  },
  "restrictions": null
}
EOF

gh api --method PUT repos/evslopes/sinatra-mini/branches/develop/protection \
  --input /tmp/develop-protection-checks.json
```

**Nota**: Os contextos dos checks (`build-and-test`, `code-quality`, etc.) devem corresponder exatamente aos nomes dos jobs no workflow CI.

---

## 📋 Validação

### Verificar configurações via CLI

```bash
# Ver proteção de main
gh api repos/evslopes/sinatra-mini/branches/main/protection

# Ver proteção de develop
gh api repos/evslopes/sinatra-mini/branches/develop/protection

# Listar todas as branches protegidas
gh api repos/evslopes/sinatra-mini/branches --paginate | grep -A 5 '"protected": true'
```

### Verificar via GitHub UI

1. Acesse: https://github.com/evslopes/sinatra-mini/settings/branches
2. Veja seções:
   - **Branch protection rules**
   - **main** → Edit
   - **develop** → Edit

---

## 🧪 Testar Proteções

### Teste 1: Push direto bloqueado

```bash
# Deve FALHAR
git checkout main
echo "test" >> README.md
git commit -am "test: direct push"
git push origin main
# Erro esperado: "remote: error: GH006: Protected branch update failed"
```

### Teste 2: PR sem aprovação bloqueado

```bash
# Criar PR
git checkout develop
git checkout -b test/branch-protection
echo "test" >> README.md
git commit -am "test: branch protection"
git push origin test/branch-protection
gh pr create --base develop --fill

# Tentar merge sem aprovação
gh pr merge --auto
# Erro esperado: "Pull request requires 1 approving review"
```

### Teste 3: Aprovação funciona

```bash
# Aprovar PR (por outro usuário)
gh pr review <PR-NUMBER> --approve

# Merge permitido após aprovação
gh pr merge --squash
```

---

## 📊 Comparação: main vs develop

| Característica | `main` | `develop` |
|----------------|--------|-----------|
| PR obrigatório | ✅ Sim | ✅ Sim |
| Aprovações mínimas | 1 | 1 |
| Dismiss stale reviews | ✅ Sim | ✅ Sim |
| Enforce para admins | ✅ Sim | ❌ Não |
| Status checks | ⏳ Pendente | ⏳ Pendente |
| Force push | ❌ Não | ❌ Não (exceto admins) |
| Delete branch | ❌ Não | ❌ Não (exceto admins) |

---

## 🔒 Segurança

### Proteções Implementadas

- ✅ **Code Review Obrigatório**: Todo código passa por revisão
- ✅ **Histórico Preservado**: Force push bloqueado
- ✅ **Estabilidade**: Push direto bloqueado
- ✅ **Rastreabilidade**: Commits via PR são rastreáveis
- ⏳ **CI/CD Validation**: Status checks (pendente configuração)

### Emergências

Em caso de urgência extrema, admins podem:
- **Em `develop`**: Fazer bypass das regras
- **Em `main`**: Temporariamente desabilitar proteção via UI

**Processo de emergência**:
1. Documentar motivo em issue
2. Desabilitar proteção temporariamente
3. Fazer correção urgente
4. Re-habilitar proteção imediatamente
5. Criar PR retroativo com a mudança

---

## 📚 Referências

- [GitHub Branch Protection](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- [GITFLOW.md](GITFLOW.md) - Guia do Gitflow
- [CI-CD-SETUP.md](CI-CD-SETUP.md) - Documentação do pipeline

---

**Responsável**: Agente DevOps  
**Última Atualização**: 21/01/2026
