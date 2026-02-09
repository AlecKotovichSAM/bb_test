FROM maven:3-eclipse-temurin-25 AS build

WORKDIR /artifacts

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package && \
    mv target/backend-*.jar target/backend.jar 2>/dev/null || \
    (JAR_FILE=$(ls target/backend-*.jar | grep -v sources | head -1) && mv "$JAR_FILE" target/backend.jar)

FROM eclipse-temurin:25-jre

RUN groupadd -r appuser && useradd -r -g appuser appuser && \
    apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /artifacts/target/backend.jar /app/backend.jar

WORKDIR /app

# Создаем папку для базы данных и настраиваем права доступа
RUN mkdir -p /app/data && \
    chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Используем переменные окружения для настройки в Docker
ENV SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/bb;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL
# Настройки Flyway для Docker:
# baseline-on-migrate создает baseline для существующей базы без истории миграций
ENV SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
# repair-on-migrate исправляет состояние истории миграций при запуске (исправляет несоответствия)
ENV SPRING_FLYWAY_REPAIR_ON_MIGRATE=true
# Логирование Flyway для диагностики
ENV LOGGING_LEVEL_ORG_FLYWAYDB=DEBUG

ENTRYPOINT [ "java", "-jar", "backend.jar" ]
