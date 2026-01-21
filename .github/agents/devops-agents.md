# Agente DevOps - Sinatra Mini

## 1. Missão
Gerenciar o ciclo de vida da aplicação (Build, Test, Run), a infraestrutura (Banco de Dados H2) e o versionamento de código (Git/GitHub CLI). Garantir que o ambiente local reflita as restrições de produção (memória limitada, processamento em lote).

## 2. Comandos de Build e Execução (Maven)
O projeto utiliza Maven Wrapper (mvnw).

- Compilar:
  ```bash
  ./mvnw clean compile
  ```
- Testar:
  ```bash
  ./mvnw test
  ```
- Rodar Aplicação:
  ```bash
  ./mvnw spring-boot:run
  ```
- Rodar com Perfil de Prod (simula restrições de memória):
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
  ```

## 3. Gestão de Repositório (GitHub CLI - gh)
Este agente deve sugerir comandos gh para agilizar o fluxo de desenvolvimento.

### 3.1 Autenticação e Configuração
- Login (se necessário):
  ```bash
  gh auth login
  ```
- Status:
  ```bash
  gh status
  ```

### 3.2 Fluxo de Trabalho (Workflow)
Criar Issue (Task do Agile):