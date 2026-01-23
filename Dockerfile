# Multi-stage Dockerfile para Sinatra Mini
# Stage 1: Build com Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copiar arquivos de dependências primeiro (cache layer)
COPY pom.xml .

# Download de dependências (cached se pom.xml não mudar)
RUN mvn dependency:go-offline -B

# Copiar código fonte
COPY src ./src

# Build da aplicação (skip tests para agilizar - testes rodam em CI/CD)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime otimizado
FROM eclipse-temurin:17-jre-alpine

# Metadados
LABEL maintainer="sinatra-mini-team"
LABEL version="1.1.0"
LABEL description="Sinatra Mini - SaaS de Gestão de Vendas"

# Criar usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring

# Instalar dumb-init para gerenciar processos (PID 1)
RUN apk add --no-cache dumb-init

WORKDIR /app

# Copiar JAR do stage de build
COPY --from=builder /app/target/*.jar app.jar

# Alterar ownership para usuário spring
RUN chown -R spring:spring /app

# Mudar para usuário não-root
USER spring:spring

# Expor porta padrão Spring Boot
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Usar dumb-init como PID 1 (gerencia sinais corretamente)
ENTRYPOINT ["dumb-init", "--"]

# JVM otimizada para containers
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-XX:InitialRAMPercentage=50.0", \
     "-XX:+UseG1GC", \
     "-XX:+HeapDumpOnOutOfMemoryError", \
     "-XX:HeapDumpPath=/tmp/heap-dump.hprof", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-jar", "app.jar"]
