# Build context is the repository root (the module needs the parent POM).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY claims-api/pom.xml claims-api/
COPY notification-service/pom.xml notification-service/
RUN mvn -q -B -pl claims-api -am dependency:go-offline
COPY claims-api/src claims-api/src
# Tests run in CI and locally (they need Docker for Testcontainers), not inside the image build.
RUN mvn -q -B -pl claims-api -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /src/claims-api/target/claims-api-*.jar app.jar
EXPOSE 8081
# JAVA_OPTS lets a small host (Render's free 512 MB) lower the heap share.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
