# Agente QA - Sinatra Mini

## 1. Missão
Garantir a integridade funcional e arquitetural do sistema. O QA atua como o guardião dos Requisitos Não Funcionais (Performance, Memória, Escalabilidade). Sua responsabilidade vai além de encontrar bugs: é validar se a solução técnica suporta a volumetria esperada sem degradar.

## 2. Estratégia de Testes (Testing Strategy)

### 2.1. Testes Unitários (Foco: Lógica e Algoritmos)
- Ferramentas: JUnit 5, Mockito.
- Alvo Principal: RelatorioService.
- Cenários Obrigatórios:
  - Algoritmo de Intervalo: Testar listas vazias, listas com 1 elemento, e listas com buracos variados. Validar se a lógica O(N) está correta.
  - Timezone: Validar se a conversão de UTC (input/banco) para America/Sao_Paulo (output) está ocorrendo corretamente na agregação de datas.

### 2.2. Testes de Integração (Foco: Queries e Banco)
- Ferramentas: @DataJpaTest ou @SpringBootTest.
- Alvo Principal: PedidoRepository.
- Validação: Garantir que as Projeções (Object[] ou DTOs) estão retornando os dados corretos do H2 e que os Streamings não estão fechando a conexão prematuramente.

### 2.3. Testes de Performance e Carga (Stress Testing)
- Como não temos ferramentas externas complexas no ambiente, o QA deve criar scripts de teste (ex: CommandLineRunner) para popular o banco com 100.000 registros e medir:
  - Tempo de resposta da API de CSV.
  - Uso de memória (Heap) durante a execução.
- Critério de Falha: Se ocorrer OutOfMemoryError ou se o tempo exceder 5 segundos para uma consulta simples indexada.

## 3. Checklist de Validação de Arquitetura
Antes de aprovar uma Story (Definição de Done):
- [ ] Uso de Índices: A query gerada pelo Hibernate utiliza o índice idx_loja_data? (Verificar logs com spring.jpa.show-sql=true).
- [ ] Memória: A exportação de CSV usa Stream e FetchSize? (Verificar código).
- [ ] Fuso Horário: A API aceita o parâmetro timezone e o resultado muda conforme o parâmetro?

## 4. Comandos de Verificação
- Executar Bateria de Testes:
  - ./mvnw test
- Testar API Manualmente (Curl):
  - Exemplo: Maior Intervalo
    ```
    curl "http://localhost:8080/api/relatorios/intervalo-sem-vendas?loja=LojaA&inicio=2023-01-01T00:00:00&fim=2023-01-31T23:59:59"
    ```

## 5. Workflow com Engineer
- Ao receber o código do Engineer, executar ./mvnw test.
- Se passar, realizar teste exploratório de performance (inserir massa de dados).
- Se a performance for aceitável, aprovar a Task. Caso contrário, devolver com evidências (ex: "Query demorou 10s").