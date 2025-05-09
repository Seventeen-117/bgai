# Multi-stage Dockerfile for Spring Boot application

#######################
# Build stage
#######################
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make the mvnw script executable
RUN chmod +x ./mvnw

# Download dependencies - this layer will be cached unless pom.xml changes
RUN ./mvnw dependency:go-offline -B

# Copy application source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests

#######################
# Runtime stage
#######################
FROM eclipse-temurin:21-jre-alpine

# Install additional dependencies needed for image processing and OCR
RUN apk add --no-cache \
    # Required for PDF processing and OCR
    tesseract-ocr \
    tesseract-ocr-data-chi_sim \
    tesseract-ocr-data-eng \
    # Required for video processing
    ffmpeg \
    # Required for image processing
    libpng \
    libjpeg-turbo \
    tiff

# Create a non-root user
RUN addgroup -S bgai && adduser -S bgai -G bgai

# Set the working directory
WORKDIR /app

# Create a data directory and make it writable
RUN mkdir -p /app/data && chown -R bgai:bgai /app

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