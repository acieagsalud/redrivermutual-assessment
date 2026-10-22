# Build context is the repository root (the module needs the parent POM).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY claims-api/pom.xml claims-api/
COPY claims-api/pom.xml claims-api/
RUN mvn -q -B -pl notification-service -am dependency:go-offline
COPY notification-service/src notification-service/src
# Tests run in CI and locally (they need Docker for Testcontainers), not inside the image build.
RUN mvn -q -B -pl notification-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /src/notification-service/target/notification-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
