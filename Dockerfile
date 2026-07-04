# Build stage
FROM maven:3.8-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/portfolio-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port and configure execution entrypoint
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
