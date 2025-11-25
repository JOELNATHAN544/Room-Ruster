# Stage 1 - Build the JAR (discarded after build)
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy only pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn -q dependency:resolve

# Copy source code
COPY src/ ./src/

# Build the application - only keep the JAR
RUN mvn -q -DskipTests package && \
    ls -la target/room-ruster-0.1.0.jar

# Stage 2 - Minimal runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy ONLY the final JAR from builder stage
COPY --from=builder /build/target/room-ruster-0.1.0.jar .

# Create state directory
RUN mkdir -p /app/state && \
    chmod 755 /app/state

# Mount volume for persistent state
VOLUME /app/state

# Set environment variable
ENV DISCORD_WEBHOOK_URL=""

# Default command: send to Discord
ENTRYPOINT ["java", "-jar", "room-ruster-0.1.0.jar"]
CMD ["--send"]
