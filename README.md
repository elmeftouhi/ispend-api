# Expense API

This is the Expense API — a Spring Boot (Java + Maven) application that uses PostgreSQL as its database.

Date: October 22, 2025

## What this README contains

- How to start a PostgreSQL 15 container (the exact `docker run` you requested)
- How to configure the application (`application.yml` / `application.properties`)
- How to build and run the app
- Example JSON payloads for creating a user and authenticating

## Prerequisites

- Java 17+ (or the version used by the project)
- Maven
- Docker

## Start PostgreSQL container

Run the following command to create a new PostgreSQL 15 container (single-line suitable for Windows `cmd.exe` / PowerShell):

```bash
docker run -d --name my-postgres15 -e POSTGRES_DB=miexpense_db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5433:5432 postgres:15
```

Or the multi-line form (Linux/macOS or shells that support backslashes):

```bash
docker run -d \
  --name my-postgres15 \
  -e POSTGRES_DB=miexpense_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
  postgres:15
```

Wait for the container to initialize. To follow logs:

```bash
docker logs -f my-postgres15
```

To stop and remove the container:

```bash
docker stop my-postgres15
docker rm my-postgres15
```

## Configure the application

The project contains `src/main/resources/application.yml` (or you can use `application.properties`). Update the datasource settings so the application connects to the container you started:

Example `application.yml` snippet:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/miexpense_db
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
server:
  port: 8080
```

Adjust `ddl-auto` and any other settings according to your requirements.

## Build and run

Build the project with Maven:

```bash
mvn clean package
```

Run with Maven:

```bash
mvn spring-boot:run
```

Or run the produced jar (after `mvn package`):

```bash
java -jar target/expense-api-0.0.1-SNAPSHOT.jar
```

The application will start on port `8080` by default unless overridden in `application.yml`.

## Example API payloads

Create a new user (POST to the `UserController` endpoint that accepts `UserCreateRequest`):

```json
{
  "firstname": "Jane",
  "lastname": "Doe",
  "email": "jane.doe@example.com",
  "password": "P@ssw0rd!",
  "status": "ACTIVE"
}
```

Authenticate (POST to `/auth/login`):

```json
{
  "email": "jane.doe@example.com",
  "password": "P@ssw0rd!"
}
```

## Useful commands

- Run tests:

```bash
mvn test
```

- Tail application logs (if running locally):

```bash
# If running via java -jar and app prints logs to console, run in a terminal to view logs
# If using a process manager, consult that tool's logs command
```

- Check Docker container status:

```bash
docker ps -a | findstr my-postgres15
```

## Notes / Next steps

- The repository already includes a `docker-compose.yml`. If you prefer docker-compose, open that file and adapt the DB service there.
- If you use a different DB port or credentials, update `src/main/resources/application.yml` accordingly.

---

File created: `README.md` — includes the requested `docker run` command and step-by-step run instructions.

