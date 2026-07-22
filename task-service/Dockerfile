FROM eclipse-temurin:26-jdk AS build

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

COPY common/build.gradle common/build.gradle
COPY common/src common/src

COPY auth-service/build.gradle auth-service/
COPY api-gateway/build.gradle api-gateway/
COPY task-service/build.gradle task-service/
COPY email-sender/build.gradle email-sender/
COPY scheduler/build.gradle scheduler/

RUN chmod +x gradlew && ./gradlew :task-service:dependencies --no-daemon || true

COPY task-service/src task-service/src

RUN ./gradlew :task-service:bootJar --no-daemon

FROM eclipse-temurin:26-jre-alpine AS runtime

WORKDIR /app
RUN apk add --no-cache wget
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/task-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]