# --- Stage 1: build ---
FROM gradle:8.14.3-jdk21-alpine AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle

RUN gradle dependencies --no-daemon || return 0

COPY . .

RUN gradle clean bootJar --no-daemon

# --- Stage 2: runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create storage directory with proper permissions
RUN mkdir -p /app/storage && chmod 755 /app/storage

# Copy application JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# Create non-root user for security
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D -s /bin/sh appuser && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# Memory and disk constraints (as per assignment requirements)
ENV JAVA_OPTS="-Xmx1g -Xms512m"
ENV FILE_STORAGE_MAX_SIZE="200MB"
ENV DISK_SPACE_CHECK_ENABLED="true"
ENV DISK_SPACE_THRESHOLD="90"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]