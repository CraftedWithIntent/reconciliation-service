FROM eclipse-temurin:17 AS builder

WORKDIR /app

# Copy Gradle wrapper files
COPY gradle ./gradle
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY src ./src

# Build the application directly (gradle will resolve dependencies during build, skipping extra dependency download step)
RUN ./gradlew clean bootJar -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/reconciliation-service-1.0.0.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/reconcile/health || exit 1

# Run the application with JVM options for Spark to access internal modules
ENTRYPOINT ["java", "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED", "-jar", "app.jar"]
