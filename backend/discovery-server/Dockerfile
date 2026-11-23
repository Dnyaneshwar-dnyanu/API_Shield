# Stage 1: Build JAR using Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml ./
COPY common ./common
COPY discovery-server ./discovery-server

# Strip multi-module aggregator block so Maven treats this as standalone parent
RUN sed -i '/<modules>/,/<\/modules>/d' pom.xml && \
    mvn install -N -DskipTests && \
    mvn install -f common/pom.xml -DskipTests && \
    mvn package -f discovery-server/pom.xml -DskipTests

# Stage 2: Minimal Distroless / JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S apishield && adduser -S apishield -G apishield
USER apishield

COPY --from=builder /build/discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8761
ENTRYPOINT ["java", "-XX:+EnableDynamicAgentLoading", "-jar", "app.jar"]
