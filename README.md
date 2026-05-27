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

This repository currently contains the foundation application scaffold, local development infrastructure, recording lifecycle domain, in-memory recording boundaries, storage/upload ports, and thin HTTP endpoints for registration, retrieval, and marking recordings as stored.

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

Successful responses return `201 Created`, a `Location` header such as `/recordings/rec-123`, and the registered metadata. The endpoint currently uses the in-memory repository, so data is not persisted across application restarts. Blank or missing request fields return `400 Bad Request` with field-level validation errors.

Retrieve registered recording metadata:

```http
GET /recordings/rec-123
```

Successful responses return `200 OK` and the registered metadata. Missing recordings return `404 Not Found`.

Mark a recording as stored after its object has been written to storage:

```http
PATCH /recordings/rec-123/storage
Content-Type: application/json

{
  "bucketName": "sleep-recordings",
  "objectKey": "recordings/rec-123/audio.m4a"
}
```

Successful responses return `200 OK` and the updated recording metadata. Blank or missing storage fields return `400 Bad Request` with field-level validation errors. This endpoint records that storage has happened; it does not upload files or talk to S3 yet.

## Recording Storage Boundary

The application layer now has a `RecordingStorage` port and a `StoreRecordingContentService`. This service stores content through the port, then marks the recording as stored using the returned storage reference.

There is still no S3 adapter, upload endpoint, or file-transfer workflow. The port exists so a future storage implementation can be added behind the application boundary.

## Presigned Upload Boundary

The preferred production-like upload direction is direct-to-S3 presigned uploads. The application layer now has:

- `CreateRecordingUploadService`
- `PresignedRecordingUploadPort`
- `PresignedRecordingUpload`

This boundary creates recording metadata, asks a port for temporary upload instructions, and returns the recording plus upload target. There is a thin HTTP endpoint for this flow:

```http
POST /recording-uploads
Content-Type: application/json

{
  "id": "rec-123",
  "ownerId": "user-456",
  "originalFilename": "night-audio.m4a",
  "contentType": "audio/mp4"
}
```

Successful responses return `201 Created`, a `Location` header such as `/recordings/rec-123`, the created recording metadata, and an `upload` object containing the temporary upload instructions.

The current upload URL provider is fake/local. There is still no AWS SDK integration or real S3 signing yet.

Complete an upload after the client has uploaded the object:

```http
POST /recordings/rec-123/upload-complete
Content-Type: application/json

{
  "bucketName": "sleep-recordings",
  "objectKey": "recordings/user-456/rec-123/audio.m4a"
}
```

Successful responses return `200 OK` and the updated recording metadata with status `STORED`. The backend checks a `RecordingObjectVerifier` port before marking the recording stored. The current verifier is fake/local and always reports that the object exists.

## Recording Package Structure

The `recordings` module is split by responsibility:

- `recordings`: domain model and value objects.
- `recordings.application`: application services, commands, and repository port.
- `recordings.web`: Spring MVC controllers and HTTP DTOs.
- `recordings.infrastructure`: current in-memory repository implementation.

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
