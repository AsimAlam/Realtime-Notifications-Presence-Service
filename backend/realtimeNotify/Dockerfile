FROM maven:3.9.3-eclipse-temurin-17 AS build
LABEL authors="asima"
WORKDIR /app
COPY pom.xml mvnw ./
COPY src ./src
RUN mvn -B -DskipTests package


FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/realtime-notify-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]