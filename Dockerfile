# Multi-stage Dockerfile for building and running the Spring Boot app

# Build stage: use Maven + JDK to build the fat jar
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /workspace/app

# Copy Maven wrapper and settings first to leverage layer caching
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw

# Copy source and build
COPY src ./src
RUN ./mvnw -B -DskipTests package

# Run stage: small JRE image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /workspace/app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]