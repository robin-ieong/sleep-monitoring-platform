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

This repository currently contains the foundation application scaffold, local development infrastructure, recording lifecycle domain, recording persistence, storage/upload ports, thin HTTP endpoints, LocalStack S3 integration for presigned upload URLs and object verification, a LocalStack SQS boundary for analysis job requests, and a small worker-side service for processing one queued analysis message body.

No SQS polling loop, separate worker runtime, or detection logic has been implemented yet.

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

Successful responses return `200 OK` and the updated recording metadata. Blank or missing storage fields return `400 Bad Request` with field-level validation errors. This endpoint records that storage has happened; it is mainly kept as a low-level lifecycle boundary while the preferred direct-to-S3 flow evolves.

## Recording Storage Boundary

The application layer now has a `RecordingStorage` port and a `StoreRecordingContentService`. This service stores content through the port, then marks the recording as stored using the returned storage reference.

The preferred real upload path is the presigned upload flow below. This older backend-byte-storage boundary is still present but is not wired to an HTTP file-upload endpoint.

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

With the default profile, this flow uses fake local adapters. With the `local` profile, the app uses the AWS SDK against LocalStack S3 to generate a real presigned PUT URL.

Complete an upload after the client has uploaded the object:

```http
POST /recordings/rec-123/upload-complete
Content-Type: application/json

{
  "bucketName": "sleep-recordings",
  "objectKey": "recordings/user-456/rec-123/audio.m4a"
}
```

Successful responses return `200 OK` and the updated recording metadata with status `STORED`. The backend checks a `RecordingObjectVerifier` port before marking the recording stored. With the `local` profile, this uses LocalStack S3 `HeadObject`; with the default profile, it uses a fake verifier.

Request analysis for a stored recording:

```http
POST /recordings/rec-123/analysis-requests
```

Successful responses return `202 Accepted` and the updated recording metadata with status `ANALYSIS_REQUESTED`. The application persists the lifecycle transition and enqueues a small message for future analysis work. With the `local` profile, this uses LocalStack SQS; with the default profile, it uses a fake queue adapter.

## Analysis Worker Boundary

The application layer now has a `ProcessRecordingAnalysisJobService` that can process one queued analysis message body:

```json
{"recordingId":"rec-123","status":"ANALYSIS_REQUESTED"}
```

For now, this worker-side boundary loads the recording, verifies the existing lifecycle state through the domain model, marks analysis as completed, and saves the updated metadata. This is placeholder worker behavior only. There is no SQS polling loop, separate worker runtime, analysis result model, or snoring/apnea/silence detection logic yet.

## Recording Package Structure

The `recordings` module is split by responsibility:

- `recordings`: domain model and value objects.
- `recordings.application`: application services, commands, and repository port.
- `recordings.web`: Spring MVC controllers and HTTP DTOs.
- `recordings.infrastructure`: adapter implementations for in-memory/fake defaults plus JDBC, S3, and SQS integrations used by the `local` profile.

With the default profile, the app uses in-memory/fake adapters for lightweight startup and tests. With the `local` profile, recording metadata is persisted in PostgreSQL, S3 upload behavior uses LocalStack S3, and analysis requests are queued in LocalStack SQS.

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

The Compose file pins LocalStack to `localstack/localstack:3.8.1` because `latest` resolved to a 2026 image that required a LocalStack license token.

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
