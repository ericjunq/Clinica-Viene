# syntax=docker/dockerfile:1
# Imagem do backend — build multi-stage (D-050). Imagens com versão fixada (nunca "latest").
# Os testes NÃO rodam aqui (precisam de Docker para o Testcontainers): rodam no CI (D-046).

# ---- build ----
FROM eclipse-temurin:25.0.5.1_2-jdk-alpine AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml lombok.config ./
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp package -DskipTests

# ---- runtime ----
FROM eclipse-temurin:25.0.5.1_2-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
# Nunca rodar como root.
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
