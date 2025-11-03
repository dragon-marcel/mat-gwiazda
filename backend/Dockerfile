# Multi-stage Dockerfile for backend
# - Build stage: uses Eclipse Temurin 17 JDK + Gradle wrapper to build a fat JAR (bootJar)
# - Runtime stage: lightweight Eclipse Temurin 17 JRE, runs as non-root user

############################
# Builder stage
############################
FROM eclipse-temurin:17-jdk AS builder

# Create app directory
WORKDIR /workspace

# Copy Gradle wrapper and configuration first to leverage Docker cache
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle/ ./gradle/

# Make gradlew executable
RUN chmod +x ./gradlew

# Copy the rest of the project (sources & resources)
COPY . .

# Build the application (skip tests for speed; remove -x test if you want tests run)
RUN ./gradlew clean bootJar -x test --no-daemon

############################
# Runtime stage
############################
FROM eclipse-temurin:17-jre

# Create a non-root user to run the app
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser || \
    (groupadd -r appgroup && useradd -r -g appgroup -d /app -s /sbin/nologin appuser)

WORKDIR /app

# Copy jar produced by the builder stage
COPY --from=builder /workspace/build/libs/*.jar ./app.jar

# Ensure the app jar is owned by the non-root user
RUN chown appuser:appgroup /app/app.jar

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Environment variable placeholders (you will pass values with `docker run -e`)
ENV OPENROUTER_API_KEY=""
ENV JWT_SECRET=""
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Run the application
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
