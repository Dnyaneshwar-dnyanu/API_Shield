# Stage 1: Build JAR using Maven
FROM eclipse-adoptium:21-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml ./
COPY common ./common
COPY user-service ./user-service

# Build user-service
RUN apk add --no-cache maven && \
    mvn clean package -pl common,user-service -DskipTests

# Stage 2: Minimal Distroless / JRE Runtime
FROM eclipse-adoptium:21-jre-alpine
WORKDIR /app

RUN addgroup -S apishield && adduser -S apishield -G apishield
USER apishield

COPY --from=builder /build/user-service/target/user-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-XX:+EnableDynamicAgentLoading", "-jar", "app.jar"]
