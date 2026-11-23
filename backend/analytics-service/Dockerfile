# Stage 1: Build JAR using Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml ./
COPY common ./common
COPY analytics-service ./analytics-service

# Strip multi-module aggregator block so Maven treats this as standalone parent
RUN sed -i '/<modules>/,/<\/modules>/d' pom.xml && \
    mvn install -N -DskipTests && \
    mvn install -f common/pom.xml -DskipTests && \
    mvn package -f analytics-service/pom.xml -DskipTests

# Stage 2: Minimal Distroless / JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S apishield && adduser -S apishield -G apishield
USER apishield

COPY --from=builder /build/analytics-service/target/analytics-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8087
ENTRYPOINT ["java", "-XX:+EnableDynamicAgentLoading", "-jar", "app.jar"]
