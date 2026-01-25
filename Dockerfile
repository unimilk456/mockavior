# syntax=docker/dockerfile:1.6

### -------- Build stage --------
FROM gradle:8.7-jdk21 AS build
WORKDIR /app

COPY . .

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle bootJar

### -------- Runtime stage --------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -r -u 1001 mockavior
USER mockavior

COPY --from=build /app/build/libs/*.jar app.jar

ENV MOCKAVIOR_CONTRACT_PATH=/app/config/mockapi.yml
ENV JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
