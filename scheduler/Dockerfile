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

RUN chmod +x gradlew && ./gradlew :scheduler:dependencies --no-daemon || true

COPY scheduler/src scheduler/src

RUN ./gradlew :scheduler:bootJar --no-daemon

FROM eclipse-temurin:26-jre AS runtime

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
USER spring

COPY --from=build /workspace/scheduler/build/libs/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]