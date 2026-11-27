# syntax=docker/dockerfile:1
# Imagem do backend — build multi-stage (D-050). Imagens com versão fixada (nunca "latest").
# Os testes NÃO rodam aqui (precisam de Docker para o Testcontainers): rodam no CI (D-046).

# ---- build ----
# Imagem oficial do Maven (já traz o Maven): evita baixar a distribuição pelo mvnw a cada build,
# que já falhou com 429 do repositório central.
FROM maven:3.10.2-eclipse-temurin-25-alpine AS build
WORKDIR /build
# Dependências numa camada própria: só é refeita quando o pom.xml muda (e fica no cache do CI),
# evitando baixar tudo do Maven Central a cada build (já falhou com 429 — limite de requisições).
COPY pom.xml lombok.config ./
RUN mvn -B -ntp dependency:go-offline
COPY src/ src/
RUN mvn -B -ntp package -DskipTests

# ---- runtime ----
FROM eclipse-temurin:25.0.5.1_2-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
# Nunca rodar como root.
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
