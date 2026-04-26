FROM amazoncorretto:25-alpine-jdk AS build

ARG SERVICE
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY game-engine-service ./game-engine-service
COPY game-session-service ./game-session-service

RUN ./gradlew ":${SERVICE}:bootJar" --no-daemon

FROM amazoncorretto:25-alpine

ARG SERVICE
WORKDIR /app

COPY --from=build /workspace/${SERVICE}/build/libs/${SERVICE}-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
