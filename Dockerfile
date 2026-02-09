FROM maven:3-eclipse-temurin-25 AS build

WORKDIR /artifacts

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package && mv target/backend-*.jar target/backend.jar

FROM eclipse-temurin:25-jammy

COPY --from=build /artifacts/target/backend.jar /app/backend.jar

WORKDIR /app

ENTRYPOINT [ "java", "-jar", "backend.jar" ]
