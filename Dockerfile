#######################
# Build stage
#######################
FROM eclipse-temurin:21.0.2_13-jdk-jammy AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x ./mvnw

COPY src src

RUN ./mvnw clean package -DskipTests

#######################
# Runtime stage
#######################
FROM eclipse-temurin:21.0.2_13-jre-jammy

WORKDIR /app

RUN groupadd -r bgai && useradd -r -g bgai bgai
RUN mkdir -p /app/data /app/logs && chown -R bgai:bgai /app

COPY --from=build /app/target/*.jar app.jar

USER bgai

EXPOSE 8086

ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=80"

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -q --spider http://localhost:8086/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]