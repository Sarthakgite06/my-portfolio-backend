# Build stage
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/portfolio-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port and configure execution entrypoint
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
