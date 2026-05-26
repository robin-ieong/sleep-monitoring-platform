# Sleep Monitoring & Snore Analysis Platform

Learning project for modern Java, Spring Boot, AWS, and system design.

The platform will accept sleep audio recordings, store them, process them asynchronously, and later support manually implemented snoring/silence analysis logic.

## Current Scaffold Choices

- App name: `sleep-monitoring-platform`
- Base package: `com.example.sleep`
- Build tool: Maven
- Java version: 21
- Backend framework: Spring MVC
- Spring Boot version: 4.0.6
- Initial architecture: modular monolith
- Storage direction: LocalStack S3
- Queue direction: LocalStack SQS

## Current Scope

This repository currently contains the foundation application scaffold, local development infrastructure, recording lifecycle domain, in-memory recording registration boundary, and a thin HTTP registration endpoint.

No upload handling, storage adapter, SQS worker, database schema, or detection logic has been implemented yet.

## Recording Registration Endpoint

Register recording metadata:

```http
POST /recordings
Content-Type: application/json

{
  "id": "rec-123",
  "ownerId": "user-456",
  "originalFilename": "night-audio.m4a",
  "contentType": "audio/mp4"
}
```

Successful responses return `201 Created`, a `Location` header such as `/recordings/rec-123`, and the registered metadata. The endpoint currently uses the in-memory repository, so data is not persisted across application restarts.

## Project Rules

- Do not generate the whole product at once.
- Do not immediately generate the full project.
- Do not make major technical decisions without confirmation.
- Confirm each major step before proceeding.
- Do not implement core snoring or apnea detection logic automatically.
- Keep the system small at first but expandable.
- Generate boilerplate only after explicit confirmation: **"Proceed with scaffold."**
- Build application behavior with TDD: write or update unit tests before implementation.
- Maintain full unit test coverage for implemented business/application behavior.
- Add integration or functional tests later, or alongside unit tests, when they are sensible for the change.

## Documentation

- Architecture decisions: `architecture-decisions.md`
- Running project log: `project-log.md`

## Opening in IntelliJ IDEA

Open this directory as a Maven project:

```text
/home/ieongr/sleep_project
```

In IntelliJ:

1. Open the project directory.
2. Let IntelliJ import the Maven `pom.xml`.
3. Configure the Project SDK as Java 21.
4. If prompted, enable Maven auto-import.

This environment has Java 21 available and the Maven Wrapper is present.

Verified tooling:

- `java`: OpenJDK 21
- `javac`: OpenJDK 21
- Maven Wrapper: Apache Maven 3.9.12

In this environment, run the wrapper with Maven's user home inside the project:

```bash
MAVEN_USER_HOME=.m2 ./mvnw -Dmaven.repo.local=.m2/repository test
```

## Local Development Infrastructure

Start the local service dependencies with Docker Compose:

```bash
docker compose up -d
```

This starts:

- LocalStack on `http://localhost:4566`
- PostgreSQL on `localhost:5432`

LocalStack is configured for:

- S3 bucket: `sleep-recordings`
- SQS queue: `sleep-recording-analysis`
- AWS region: `eu-west-2`

PostgreSQL is configured for:

- Database: `sleep_monitoring`
- Username: `sleep_app`
- Password: `sleep_app_password`

Run the Spring app with the `local` profile when you want it to use these local settings:

```bash
SPRING_PROFILES_ACTIVE=local MAVEN_USER_HOME=.m2 ./mvnw -Dmaven.repo.local=.m2/repository spring-boot:run
```

Stop the local services with:

```bash
docker compose down
```
