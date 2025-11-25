# Multi-stage Dockerfile: build the jar with Maven, run with a slim JRE

FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace
# copy maven files first to leverage docker layer caching
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY src ./src

# package the application (skip tests to speed up image build)
RUN mvn -B -DskipTests package

# Runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app
# copy the jar from the build stage
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

