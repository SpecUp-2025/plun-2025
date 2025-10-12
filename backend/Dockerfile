FROM gradle:8.8-jdk17 AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]