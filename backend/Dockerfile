FROM gradle:8.8-jdk17 AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*SNAPSHOT.jar /app/app.jar


ENTRYPOINT ["java", "-jar", "app.jar"]