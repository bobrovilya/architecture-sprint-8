FROM maven:3.9.9-amazoncorretto-17-al2023 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-oracle
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]