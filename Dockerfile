# Build the Spring Boot application with Maven.
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

# Resolve dependencies separately so Docker can cache them between source changes.
COPY backend/pom.xml ./pom.xml
RUN mvn -B -DskipTests dependency:go-offline

COPY backend/src ./src
RUN mvn -B -DskipTests clean package

# Run with a small Java 17 runtime image and a non-root user.
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
RUN addgroup -S keyvault && adduser -S keyvault -G keyvault

COPY --from=build --chown=keyvault:keyvault /workspace/target/keyvault-1.0.0.jar ./app.jar

USER keyvault
EXPOSE 10000

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
