# --- Build stage ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies in their own layer — only re-downloaded when pom.xml changes.
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar

# Render (and most PaaS hosts) inject PORT — application.properties already
# reads it via ${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
