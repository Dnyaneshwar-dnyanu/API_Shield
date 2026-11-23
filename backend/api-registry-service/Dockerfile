# Stage 1: Build JAR using Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml ./
COPY common ./common
COPY api-registry-service ./api-registry-service

# Strip multi-module aggregator block so Maven treats this as standalone parent
RUN sed -i '/<modules>/,/<\/modules>/d' pom.xml && \
    mvn install -N -DskipTests && \
    mvn install -f common/pom.xml -DskipTests && \
    mvn package -f api-registry-service/pom.xml -DskipTests

# Stage 2: Minimal Distroless / JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S apishield && adduser -S apishield -G apishield
USER apishield

COPY --from=builder /build/api-registry-service/target/api-registry-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-XX:+EnableDynamicAgentLoading", "-jar", "app.jar"]
