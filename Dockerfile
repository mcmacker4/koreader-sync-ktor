FROM eclipse-temurin:21-jdk AS build

COPY gradle/ /project/gradle/
COPY src/ /project/src/
COPY build.gradle.kts settings.gradle.kts gradlew /project/

WORKDIR /project

RUN ["./gradlew", "--no-daemon", "buildFatJar"]

FROM eclipse-temurin:24-jre

COPY --from=build /project/build/libs/koreader-sync-ktor-all.jar /app/koreader/

WORKDIR /app
ENTRYPOINT ["java", "-cp", "/app/drivers/*:/app/koreader/koreader-sync-ktor-all.jar", "es.hgg.koreader.sync.ApplicationKt"]

