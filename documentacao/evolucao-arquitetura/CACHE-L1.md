# Cache L1 (Caffeine) — Sinatra Mini

Resumo: implementação L1 local usando Caffeine para reduzir latência de consultas de relatório.

O que foi adicionado neste PR-esboço:

- `src/main/java/pro/sinatra/mini/config/CacheConfig.java` — configuração de `CaffeineCacheManager` com TTL 5 minutos, máximo 1000 entradas e `recordStats()` para métricas.

Como revisar / testar:

1. Build e rodar a aplicação com profile `dev` ou `prod`.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

2. Verificar que o `CacheManager` foi carregado (logs) e que `@EnableCaching` está ativo.

3. Marcar métodos de serviço relevantes com `@Cacheable` (ex.: `MaiorIntervaloSemVendasService.calcular(...)`).

4. Teste manual de hit-rate local:

```bash
# Fazer primeira chamada (miss)
curl -s http://localhost:8080/pedidos/1/reports/longest-no-sales
# Repetir chamada várias vezes (hit)
for i in {1..10}; do curl -s http://localhost:8080/pedidos/1/reports/longest-no-sales >/dev/null; done
```

5. Ver métricas via Actuator (Actuator já presente):

```bash
curl http://localhost:8080/actuator/metrics/cache.gets
curl http://localhost:8080/actuator/metrics/cache.hit
```

Notas para revisores:
- Dependência `caffeine` já está no `pom.xml`. Adicionei `CacheConfig` e `@EnableCaching`.
- É necessário adicionar `@Cacheable` nos métodos de `Service` que fazem queries pesadas — este PR é um esboço para revisão; posso aplicar as anotações em um follow-up.
- Recomendo testes de carga para validar hit-rate > 70% antes de marcar como Done.
