# Sleep Monitoring & Snore Analysis Platform

This document captures the initial architecture and technology decisions for a learning project focused on modern Java, Spring Boot, AWS, and system design.

Important project rules:

- Do not immediately generate the full project.
- Do not make major technical decisions without confirmation.
- Do not implement core snoring or apnea detection logic automatically.
- Keep the project small at first but expandable.
- Favor production-like, scalable architecture when choosing core boundaries, while still implementing in small confirmed steps.
- Generate boilerplate only after explicit confirmation: **"Proceed with scaffold."**

## 1. Project Structure

### Monolith

One Spring Boot application containing API, database access, upload handling, and processing logic.

Pros:

- Simplest to build and debug.
- Fastest learning loop.
- Easier local development.
- Fewer deployment moving parts.

Cons:

- Can become messy if package boundaries are weak.
- Harder to scale parts independently later.
- Less exposure to distributed-system concerns.

Use here:

- One app handles upload requests, stores metadata, writes audio to storage, and runs background processing.

### Modular Monolith

One deployable application, but internally split into clear modules such as:

- `users`
- `recordings`
- `storage`
- `processing`
- `analysis`
- `notifications`

Pros:

- Keeps the learning project manageable.
- Teaches system design boundaries without microservice complexity.
- Easier to split into services later.
- Good fit for Spring Boot.

Cons:

- Requires discipline around package/module boundaries.
- Still scales as one deployable unit.

Use here:

- API and background processing can live in one app initially, but separated by domain modules.

### Microservices

Separate deployable services, for example:

- Upload API service
- Processing worker service
- User/auth service
- Analysis service

Pros:

- Realistic for large distributed systems.
- Independent scaling and deployment.
- Strong service boundaries.

Cons:

- Too much operational overhead early.
- Requires service discovery, distributed tracing, deployment orchestration, network failure handling, versioning, etc.
- Can distract from learning Java/Spring/AWS fundamentals.

Use here:

- Eventually useful if audio processing becomes expensive and needs dedicated worker scaling.

### Recommendation

Start with a **modular monolith**.

It gives clean architecture practice without premature distributed complexity. Later, the processing module can become a separate worker service if needed.

Recommended choice: **Modular monolith**

## 2. Backend Framework

### Spring MVC

Traditional Spring web stack using servlet-based request handling.

Pros:

- Most common Spring Boot approach.
- Easier to learn.
- Huge ecosystem support.
- Works well with PostgreSQL, S3 SDKs, Spring Security, validation, OpenAPI, etc.
- Simpler debugging and testing.

Cons:

- Historically one request used one thread.
- High-concurrency workloads could need more threads, though Java virtual threads reduce this concern.

Use here:

- File upload endpoint receives audio metadata/file.
- Controller calls application service.
- Background processing is handled asynchronously through a queue or worker.

### Spring WebFlux

Reactive, non-blocking framework based on Reactor.

Pros:

- Good for very high concurrency.
- Efficient for streaming and non-blocking IO.
- Useful when most dependencies are reactive.

Cons:

- Steeper learning curve.
- Debugging is harder.
- Reactive code spreads through the application.
- Not ideal if database/storage libraries are mostly blocking.
- Adds mental overhead while learning Spring.

Use here:

- Could support streaming uploads and async APIs, but probably overkill initially.

### Blocking IO with Virtual Threads

Spring MVC can use Java virtual threads to handle blocking workloads more efficiently.

Pros:

- Simple imperative code.
- Better scalability than traditional platform threads for IO-heavy workloads.
- Fits Java 21 well.
- Avoids reactive complexity.

Cons:

- Not magic for CPU-heavy processing.
- You still need proper async architecture for audio analysis jobs.
- Some libraries may pin carrier threads in edge cases.

Use here:

- API requests remain simple.
- Uploads and database calls can be blocking.
- Audio processing should still happen outside the request path.

### Recommendation

Use **Spring MVC + Java 21 virtual threads**.

This is modern, pragmatic, and easier to learn than WebFlux. For this project, async processing should come from queues/workers, not reactive controllers.

Recommended choice: **Spring MVC with virtual threads**

## 3. Java Version

### Java 17

Pros:

- Long-term support.
- Widely used in production.
- Strong Spring Boot support.
- Supports records and sealed classes.

Cons:

- No stable virtual threads.
- Missing newer language improvements.

Use here:

- Fine if maximum enterprise familiarity is the goal.

### Java 21

Pros:

- Long-term support.
- Virtual threads are stable.
- Records, sealed classes, pattern matching, modern switch improvements.
- Excellent fit for modern Spring Boot.
- Strong learning value.

Cons:

- Slightly newer than some older enterprise stacks.
- You need to be intentional about using newer features appropriately.

Use here:

- Records for DTOs.
- Sealed interfaces for domain events or processing result types.
- Virtual threads for request handling.
- Pattern matching for cleaner branching.

### Newer than Java 21

Pros:

- Latest language/runtime features.
- Good for experimentation.

Cons:

- Less stable for learning production-style backend work.
- Some hosting/build environments may lag.
- Not necessary for this project.

### Recommendation

Use **Java 21**.

It is the best balance of modern features, production relevance, and long-term support.

Recommended choice: **Java 21**

## 4. Database

### PostgreSQL

Pros:

- Excellent relational database.
- Strong data integrity.
- Great for joins, constraints, indexing, JSON columns, timestamps.
- Common with Spring Boot.
- Good learning value.

Cons:

- Requires schema design.
- Horizontal scaling is more involved than DynamoDB.

Use here:

- users
- recordings
- processing jobs
- analysis summaries
- audit timestamps
- ownership relationships

### MySQL

Pros:

- Popular and mature.
- Good Spring Boot support.
- Familiar in many companies.

Cons:

- PostgreSQL generally has richer features.
- Slightly less attractive for complex relational modelling and JSON-heavy metadata.

Use here:

- Also works fine, but less ideal if strong relational modelling practice is a goal.

### DynamoDB

Pros:

- Serverless and highly scalable.
- Good AWS learning value.
- Great for predictable key-value access patterns.
- No server management.

Cons:

- Requires access-pattern-first modelling.
- Harder for ad hoc queries.
- Weaker fit for relational user/recording/job data.
- Local development and testing can be more involved.
- Less beginner-friendly for domain modelling.

Use here:

- Could be useful later for event/status lookup, high-volume derived metrics, or time-series-like summaries.

### Relational vs NoSQL

Relational modelling fits when data has relationships:

- A user owns many recordings.
- A recording has many processing attempts.
- A recording has one or more analysis results.
- Access control depends on ownership.

NoSQL fits when exact lookup patterns are known and massive scale is required.

### Recommendation

Use **PostgreSQL** as the primary database.

Potential later addition:

- DynamoDB for AWS-specific learning or high-volume derived event data.

Recommended choice: **PostgreSQL**

## 5. Database Migrations

### Flyway

Pros:

- Simple.
- SQL-first.
- Easy to understand.
- Great for small and medium projects.
- Works naturally with PostgreSQL.

Cons:

- Less flexible than Liquibase for complex enterprise workflows.
- Rollbacks are more manual.

Use here:

- `V1__create_recordings.sql`
- `V2__create_processing_jobs.sql`

### Liquibase

Pros:

- More powerful.
- Supports XML/YAML/JSON/SQL changelogs.
- Better rollback support.
- Useful in larger enterprise environments.

Cons:

- More complex.
- More ceremony.
- Less ideal for a first Spring Boot learning project.

Use here:

- Valuable later if enterprise migration workflows are a learning goal.

### Recommendation

Use **Flyway**.

It keeps the database lifecycle clear without adding unnecessary complexity.

Recommended choice: **Flyway**

## 6. File / Audio Storage

### Local Filesystem

Pros:

- Very simple.
- No AWS setup.
- Easy to inspect files manually.
- Good for the first milestone.

Cons:

- Not production-like.
- Harder in containerized/cloud environments.
- Needs cleanup and path safety.
- Not horizontally scalable.

Use here:

- Save uploaded audio under a local directory like `data/uploads`.

### S3-Compatible Storage

Pros:

- Production-style object storage.
- Good AWS learning.
- Decouples file storage from the app server.
- Scales well.
- Natural fit for audio files.

Cons:

- More setup.
- Requires bucket/key design.
- Requires IAM/security understanding.
- Local testing needs mocks or LocalStack.

Use here:

- Store audio objects by key like `recordings/{userId}/{recordingId}/original.wav`.

### LocalStack

Pros:

- Simulates AWS services locally.
- Great for learning S3 and SQS without cloud cost.
- Works well with Docker Compose.

Cons:

- Adds local infrastructure complexity.
- Not always identical to real AWS behavior.

Use here:

- Local S3 bucket.
- Local SQS queue.
- Later maybe SNS, Lambda, etc.

### Real S3

Pros:

- Real AWS behavior.
- Valuable AWS learning.
- Production-like.

Cons:

- Costs money.
- Requires IAM, bucket policies, lifecycle config.
- Mistakes can expose data if security is weak.

Use here:

- Use after local upload flow is stable.

### Upload Path

#### Backend-Mediated Upload

The client uploads audio bytes to the backend, and the backend writes them to storage.

Pros:

- Simpler first implementation.
- Backend fully controls the write path.
- Easy to unit test behind a storage port.

Cons:

- API servers handle large audio payloads.
- Less scalable if the app becomes popular.
- More bandwidth and timeout pressure on the backend.

#### Direct-to-S3 Presigned Upload

The backend creates recording metadata, chooses the S3 object key, returns a temporary presigned upload URL, and the client uploads audio directly to S3. The backend then verifies the object exists before marking the recording as stored.

Pros:

- More production-like.
- Scales better because large audio bytes bypass API servers.
- Keeps file transfer on S3, which is designed for it.
- Backend still owns metadata, ownership, object keys, and lifecycle status.

Cons:

- More moving parts.
- Requires presigned URL generation.
- Requires client-side S3 upload handling.
- Requires backend verification before trusting upload completion.

### Recommendation

Use **S3-compatible storage via LocalStack** and design the real upload flow around **direct-to-S3 presigned uploads**.

The important design point is to keep storage behind application boundaries so the app can evolve in small steps while still targeting a scalable production-like architecture.

Recommended choice: **Presigned S3 upload flow + LocalStack S3 for local development**

## 7. Message Queue

### SQS

Pros:

- Simple managed queue.
- Great AWS learning.
- Built-in retries through visibility timeout.
- Supports DLQ.
- Good for async job processing.
- Low operational burden.

Cons:

- Not ideal for complex event streaming.
- Ordering only with FIFO queues, with throughput tradeoffs.
- Message size limits.

Use here:

- Upload creates a recording row and sends a `RecordingUploaded` message.
- Worker receives message and processes audio.
- Failed jobs retry.
- Poison messages go to DLQ.

### Kafka

Pros:

- Excellent event streaming platform.
- High throughput.
- Durable event log.
- Great for many consumers and replaying streams.

Cons:

- Heavy operationally.
- More complex mental model.
- Overkill for initial async audio jobs.

Use here:

- Useful later for analytics pipelines or multiple downstream consumers.

### RabbitMQ

Pros:

- Mature broker.
- Flexible routing.
- Good local development.
- Easier than Kafka in many cases.

Cons:

- More operational burden than SQS in AWS.
- Less directly aligned with AWS certification learning.

Use here:

- Fine if deploying outside AWS.

### Async Needs

This project needs:

- job dispatch
- retries
- failure isolation
- DLQ
- idempotent processing
- status updates
- no strict ordering initially

### Recommendation

Use **SQS**.

For local development, use **LocalStack SQS**. Later deploy to real AWS SQS.

Recommended choice: **SQS with DLQ**

## 8. Caching

### No Cache Initially

Pros:

- Simplest.
- Avoids stale-data bugs.
- Forces good database modelling first.

Cons:

- Less exposure to Redis/caching early.
- Some repeated queries may be slower later.

Use here:

- Completely acceptable for milestone one.

### Application Cache

Pros:

- Simple with Spring Cache.
- No extra infrastructure.
- Good for static/reference data.

Cons:

- Per-instance only.
- Not useful across multiple app instances.
- Can hide consistency issues.

Use here:

- Maybe cache config-like values later.

### Redis

Pros:

- Shared distributed cache.
- Useful for rate limits, sessions, temporary job progress, locks.
- Common backend skill.

Cons:

- Extra infrastructure.
- Easy to overuse.
- Not needed until there are performance or coordination needs.

Use here:

- Later for rate limiting upload endpoints.
- Possibly cache recent analysis summaries.
- Possibly store short-lived processing progress.

### Recommendation

Use **no cache initially**.

Add Redis later only when there is a clear use case.

Recommended choice: **No cache for milestone one**

## 9. Authentication and Authorization

### Session Auth

Pros:

- Simple for server-rendered apps.
- Secure when done correctly.
- Easy logout/session invalidation.

Cons:

- Less natural for mobile/API clients.
- Requires session storage if scaled horizontally.
- CSRF concerns for browser apps.

Use here:

- Good if building a traditional web app with server-rendered pages.

### JWT

Pros:

- Common for APIs.
- Works well with separate frontend/mobile clients.
- Stateless verification.
- Good learning value.

Cons:

- Token invalidation is harder.
- Security mistakes are common.
- Refresh token flow adds complexity.

Use here:

- Suitable if this is an API-first backend.

### Spring Security Options

Possible stages:

1. No auth, use a fake/dev user ID.
2. Basic auth for learning.
3. JWT resource server.
4. OAuth2/OIDC with Cognito/Auth0/Keycloak.

### Roles and Ownership

For this app, ownership matters more than roles at first.

Examples:

- A user can only see their own recordings.
- A user can only download their own audio.
- Admin can view processing failures.
- Worker can update processing status.

### Recommendation

Start with **simple Spring Security + JWT-style API auth later**, but for the first scaffold a temporary dev user approach is acceptable if the focus is upload/processing.

For AWS learning, a later option is Amazon Cognito + Spring Security OAuth2 Resource Server.

Recommended choice: **Start with simple auth boundary, add JWT/OIDC later**

## 10. API Documentation

### OpenAPI / Swagger

Pros:

- Documents endpoints automatically.
- Gives a browser UI for testing.
- Helps frontend development.
- Makes API contracts explicit.
- Good professional habit.

Cons:

- Requires annotations or careful DTO design.
- Docs can drift if neglected.

Use here:

- Upload recording.
- List recordings.
- Get processing status.
- Get analysis summary.
- Download or pre-sign audio access later.

### Recommendation

Use **springdoc-openapi**.

It is lightweight and useful immediately.

Recommended choice: **OpenAPI with springdoc**

## 11. Error Handling Strategy

### Global Exception Handling

Use `@RestControllerAdvice` to convert exceptions into consistent API responses.

Example response shape:

```json
{
  "code": "RECORDING_NOT_FOUND",
  "message": "Recording was not found",
  "requestId": "..."
}
```

Pros:

- Consistent API.
- Easier frontend handling.
- Cleaner controllers.

### Typed Domain Exceptions

Examples:

- `RecordingNotFoundException`
- `UnauthorizedRecordingAccessException`
- `InvalidAudioFormatException`
- `ProcessingJobAlreadyStartedException`

Pros:

- Makes domain failures explicit.
- Easier to map to HTTP statuses.

Cons:

- Too many exception classes can become noisy.

### Validation Errors

Use Bean Validation:

- `@NotNull`
- `@Size`
- `@Positive`
- custom validators later

Return structured field errors.

### Retryable vs Non-Retryable Errors

Important for async processing.

Retryable:

- temporary S3 failure
- database timeout
- transient queue issue

Non-retryable:

- unsupported audio format
- corrupted file
- missing recording
- unauthorized access

### Recommendation

Use:

- `@RestControllerAdvice`
- typed domain exceptions
- structured validation errors
- explicit processing failure categories

Recommended choice: **Centralized error handling with typed domain exceptions**

## 12. Observability

### Structured Logging

Pros:

- Essential.
- Simple.
- Works locally and in cloud.
- Makes debugging async flows much easier.

Use here:

- `recordingId`
- `userId`
- `jobId`
- processing status
- storage key
- request ID

### Spring Actuator

Pros:

- Health checks.
- Metrics endpoints.
- Useful for deployment.
- Easy to add.

Use here:

- `/actuator/health`
- readiness/liveness checks later

### Micrometer

Pros:

- Standard metrics facade in Spring.
- Works with Prometheus, CloudWatch, etc.

Use here:

- uploads count
- processing duration
- failed jobs
- queue lag later

### OpenTelemetry

Pros:

- Distributed tracing.
- Valuable once there are multiple services or AWS components.

Cons:

- More setup.
- Not necessary on day one.

Use here:

- Add later when app + worker are split or deployed to AWS.

### CloudWatch vs Prometheus/Grafana

CloudWatch:

- Native AWS.
- Good for AWS learning.
- Easier with ECS/Lambda/AWS services.

Prometheus/Grafana:

- Common cloud-native stack.
- Great dashboards.
- More infrastructure to run.

### Recommendation

Start with:

- structured logs
- Spring Actuator
- Micrometer basics

Add:

- CloudWatch when deploying to AWS
- OpenTelemetry later

Recommended choice: **Structured logging + Actuator + Micrometer first**

## 13. Testing Strategy

### Unit Tests

Pros:

- Fast.
- Good for domain logic.
- Great for business rules implemented manually.

Use here:

- Recording status transitions.
- Processing job state machine.
- Validation of accepted file metadata.

### Integration Tests

Pros:

- Tests Spring wiring, database, repositories, controllers.
- Catches real configuration issues.

Use here:

- Upload metadata flow.
- Recording persistence.
- API error responses.

### Testcontainers

Pros:

- Real PostgreSQL/SQS-like dependencies in tests.
- Much more realistic than mocks.
- Strong professional skill.

Cons:

- Requires Docker.
- Slower than unit tests.

Use here:

- PostgreSQL integration tests.
- Possibly LocalStack container for S3/SQS tests.

### Contract/API Tests

Pros:

- Protects API behavior.
- Useful when frontend exists.

Cons:

- Too early unless API consumers are serious.

Use here:

- Later, once endpoints stabilize.

### Load Tests: k6 or Gatling

Pros:

- Good for upload and async queue throughput testing.
- Teaches system behavior under pressure.

Cons:

- Premature until core flows work.

Use here:

- Later test concurrent uploads and processing queue behavior.

### Recommendation

Start with:

- JUnit unit tests
- Spring Boot integration tests
- Testcontainers for PostgreSQL

Add LocalStack Testcontainers after S3/SQS is introduced.

Recommended choice: **JUnit + Spring integration tests + Testcontainers**

## 14. Docker and Local Development

### No Docker Initially

Pros:

- Simpler if only using app + in-memory/dev setup.
- Less local tooling.

Cons:

- Not realistic for PostgreSQL/S3/SQS.
- Harder to reproduce environment.

Use here:

- Only viable for the very first toy API.

### Docker Compose

Pros:

- Excellent for local dependencies.
- Easy to run Postgres, Redis, LocalStack.
- Mirrors deployment concerns.
- Great learning value.

Cons:

- Requires Docker.
- Compose files need maintenance.

Use here:

- App runs locally from IDE.
- Dependencies run in Docker:
  - Postgres
  - LocalStack
  - maybe Redis later

### Recommendation

Use **Docker Compose for dependencies**, not necessarily for the app at first.

This gives a comfortable Java development loop while still learning realistic infrastructure.

Recommended choice: **Docker Compose for Postgres and LocalStack**

## 15. Deployment Options

### Render / Railway / Fly.io

Pros:

- Easier deployment.
- Lower operational complexity.
- Good for getting something public quickly.

Cons:

- Less AWS learning.
- Less exposure to IAM, ECS, S3, SQS, CloudWatch.
- Platform-specific abstractions.

Use here:

- Good if the main goal is shipping a demo quickly.

### AWS ECS Fargate

Pros:

- Strong AWS learning value.
- Good match for AWS SAA concepts.
- Runs containers without managing servers.
- Integrates with S3, SQS, CloudWatch, IAM, RDS.

Cons:

- More complex.
- Costs can accumulate.
- Requires VPC/networking/IAM understanding.

Use here:

- API service on ECS Fargate.
- Worker service on ECS Fargate.
- RDS PostgreSQL.
- S3 bucket.
- SQS queue + DLQ.
- CloudWatch logs/metrics.

### AWS Lambda

Pros:

- Good for event-driven processing.
- Scales automatically.
- Can process S3/SQS events.

Cons:

- Audio processing may hit timeout/memory constraints.
- Java cold starts.
- More constraints for large files.

Use here:

- Possible later for lightweight metadata extraction, not initial core app.

### Recommendation

For learning AWS and system design: **eventually deploy to ECS Fargate**.

For milestone one, keep deployment local. Do AWS deployment only after the upload + queue + status flow works locally.

Recommended future choice: **AWS ECS Fargate + RDS + S3 + SQS**

## 16. CI/CD

### GitHub Actions Basic Pipeline

Pros:

- Free/easy for GitHub projects.
- Common industry skill.
- Runs tests on every push/PR.

Use here:

- Build Java app.
- Run unit tests.
- Run integration tests if Docker is available.
- Check formatting later.

### Docker Image Build

Pros:

- Required for ECS/Fargate.
- Catches packaging issues.
- Makes deployment consistent.

Cons:

- Adds build time and registry setup.

Use here:

- Build image after tests pass.
- Push to GitHub Container Registry or AWS ECR later.

### Deployment Pipeline

Pros:

- Automates releases.
- Good DevOps learning.

Cons:

- Too much early.
- AWS credentials and IAM need care.

Use here:

- Add only after manual deployment works.

### Recommendation

Start with:

- GitHub Actions build/test

Later:

- Docker image build
- Push to ECR
- Deploy ECS service

Recommended choice: **GitHub Actions build/test first**

## Architecture Decision Record Summary

| Area | Recommendation |
| --- | --- |
| Project structure | Modular monolith |
| Backend | Spring MVC |
| IO model | Blocking IO with Java 21 virtual threads |
| Java | Java 21 |
| Database | PostgreSQL |
| Migrations | Flyway |
| Audio storage | Storage abstraction, LocalStack S3 locally |
| Queue | SQS with DLQ, LocalStack locally |
| Cache | No cache initially |
| Auth | Start simple, add JWT/OIDC later |
| API docs | OpenAPI with springdoc |
| Error handling | Global handler + typed domain exceptions |
| Observability | Structured logs + Actuator + Micrometer |
| Testing | JUnit + Spring integration tests + Testcontainers |
| Local dev | Docker Compose for dependencies |
| Deployment | Local first, AWS ECS Fargate later |
| CI/CD | GitHub Actions build/test first |

## First Milestone Plan

Milestone 1 should prove the platform shape without implementing snore detection.

Build only this:

1. Create recording metadata.
2. Upload/store an audio file.
3. Persist recording status in PostgreSQL.
4. Publish an async processing message.
5. Consume the message with a worker component.
6. Simulate processing with a placeholder.
7. Update status to `UPLOADED`, `PROCESSING`, `COMPLETED`, or `FAILED`.
8. Expose APIs to:
   - upload recording
   - list recordings
   - get recording status
   - get placeholder analysis result

No real apnea/snore detection yet.

## Minimal Scaffold Plan

Only after the user says **"Proceed with scaffold."**

```text
sleep-monitoring-platform/
  src/main/java/.../
    SleepMonitoringApplication.java

    recordings/
      api/
      application/
      domain/
      persistence/

    processing/
      application/
      domain/
      messaging/

    storage/
      StorageService.java
      s3/
      local/

    shared/
      errors/
      security/
      observability/

  src/main/resources/
    application.yml
    db/migration/

  src/test/java/.../

  docker-compose.yml
  build.gradle or pom.xml
  README.md
```

Before generating that, confirm:

1. Build tool: **Maven or Gradle**
2. Package name, for example: `com.example.sleep`
3. Whether milestone one uses:
   - Local filesystem first, or
   - LocalStack S3 immediately
4. Whether SQS is included in the first scaffold or added in milestone two

## Business Logic to Implement Manually

To maximize learning, the user should manually implement:

- Recording status transition rules.
- File validation rules.
- Ownership checks.
- Processing job lifecycle.
- Retry/non-retry classification.
- Audio metadata extraction.
- Silence detection.
- Snore event detection.
- Sleep segment aggregation.
- Risk scoring rules.
- Any apnea-risk heuristics.
- Final analysis summary generation.

Codex can review or guide those implementations, but should not generate the core detection logic.

## Initial Choices to Confirm

Before scaffolding, choose:

1. Project structure: modular monolith?
2. Backend: Spring MVC + Java 21 virtual threads?
3. Database: PostgreSQL + Flyway?
4. Storage: local filesystem first or LocalStack S3 immediately?
5. Queue: include LocalStack SQS in milestone one, or defer?
6. Build tool: Maven or Gradle?
