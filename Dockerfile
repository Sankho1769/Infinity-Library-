# Multi-Stage Dockerfile for Infinity Library Backend
# Architecture: Senior Backend Modular Monolith
# Stack: Eclipse Temurin JDK 25 LTS, Spring Boot 4.2.x

# Stage 1: Build Application
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

# Cache Maven dependencies layer
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy project source code and compile fat executable JAR
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal Production Runtime
FROM eclipse-temurin:25-jre
LABEL maintainer="Infinity Library Engineering Team" \
      version="1.0.0" \
      description="Production Modular Monolith Library Management Backend"

# Install curl for Actuator health checks and create unprivileged service user
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -r -g 10001 spring && \
    useradd -r -u 10001 -g spring -m -s /bin/bash spring

WORKDIR /app

# Copy artifact from builder stage
COPY --from=builder /workspace/target/infinity-library-1.0.0.jar app.jar

# Enforce secure ownership
RUN chown -R spring:spring /app

USER spring:spring

# Production JVM tuning for container runtime
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
