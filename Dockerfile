# ==============================================================================
# Multi-Stage Dockerfile for Crop Disease Detector Backend
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Application
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Copy Maven wrapper & pom.xml first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Normalize line endings in case cloned on Windows and make wrapper executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Pre-fetch dependencies
RUN ./mvnw dependency:go-offline -B

# Copy project source and package the production executable JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Production Minimal Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install curl for container health checks
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run as non-root user for container security
RUN groupadd -r spring && useradd -r -g spring -s /bin/false spring

# Copy jar from builder stage with correct file permissions
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Switch to non-root user
USER spring:spring

# Expose Spring Boot port
EXPOSE 8080

# Configure container-aware JVM memory settings
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Healthcheck checking Spring Boot actuator health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
