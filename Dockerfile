# Multi-stage Dockerfile for Spring Boot application

#######################
# Build stage
#######################
FROM registry.cn-hangzhou.aliyuncs.com/library/eclipse-temurin:21-jdk-slim AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x ./mvnw

# Copy application source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

#######################
# Runtime stage
#######################
FROM registry.cn-hangzhou.aliyuncs.com/library/eclipse-temurin:21-jre-slim

# Set the working directory
WORKDIR /app

# Create a non-root user and group
RUN groupadd -r bgai && useradd -r -g bgai bgai

# Create a data directory and make it writable
RUN mkdir -p /app/data /app/logs && chown -R bgai:bgai /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Switch to non-root user for security
USER bgai

# Expose ports
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=80"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 