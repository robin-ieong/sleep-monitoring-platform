package com.example.sleep.recordings;

import com.example.sleep.recordings.application.CompleteRecordingUploadService;
import com.example.sleep.recordings.application.CreateRecordingUploadService;
import com.example.sleep.recordings.application.MarkRecordingStoredService;
import com.example.sleep.recordings.application.PresignedRecordingUploadPort;
import com.example.sleep.recordings.application.ProcessRecordingAnalysisJobService;
import com.example.sleep.recordings.application.RecordingAnalysisJobProcessor;
import com.example.sleep.recordings.application.RecordingAnalysisQueue;
import com.example.sleep.recordings.application.RecordingAnalysisResultRepository;
import com.example.sleep.recordings.application.RecordingObjectVerifier;
import com.example.sleep.recordings.application.RecordingRepository;
import com.example.sleep.recordings.application.RequestRecordingAnalysisService;
import com.example.sleep.recordings.application.RegisterRecordingService;
import com.example.sleep.recordings.infrastructure.FakePresignedRecordingUploadPort;
import com.example.sleep.recordings.infrastructure.FakeRecordingAnalysisQueue;
import com.example.sleep.recordings.infrastructure.FakeRecordingObjectVerifier;
import com.example.sleep.recordings.infrastructure.InMemoryRecordingAnalysisResultRepository;
import com.example.sleep.recordings.infrastructure.InMemoryRecordingRepository;
import com.example.sleep.recordings.infrastructure.JdbcRecordingAnalysisResultRepository;
import com.example.sleep.recordings.infrastructure.JdbcRecordingRepository;
import com.example.sleep.recordings.infrastructure.S3PresignedRecordingUploadPort;
import com.example.sleep.recordings.infrastructure.S3RecordingObjectVerifier;
import com.example.sleep.recordings.infrastructure.SqsRecordingAnalysisJobPoller;
import com.example.sleep.recordings.infrastructure.SqsRecordingAnalysisQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

@Configuration
public class RecordingConfiguration {

    @Bean
    @Profile("!local")
    RecordingRepository recordingRepository() {
        return new InMemoryRecordingRepository();
    }

    @Bean
    @Profile("local")
    RecordingRepository jdbcRecordingRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRecordingRepository(jdbcTemplate);
    }

    @Bean
    @Profile("!local")
    RecordingAnalysisResultRepository recordingAnalysisResultRepository() {
        return new InMemoryRecordingAnalysisResultRepository();
    }

    @Bean
    @Profile("local")
    RecordingAnalysisResultRepository jdbcRecordingAnalysisResultRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRecordingAnalysisResultRepository(jdbcTemplate);
    }

    @Bean
    RegisterRecordingService registerRecordingService(RecordingRepository repository, Clock clock) {
        return new RegisterRecordingService(repository, clock);
    }

    @Bean
    MarkRecordingStoredService markRecordingStoredService(RecordingRepository repository, Clock clock) {
        return new MarkRecordingStoredService(repository, clock);
    }

    @Bean
    @Profile("!local")
    PresignedRecordingUploadPort presignedRecordingUploadPort(Clock clock) {
        return new FakePresignedRecordingUploadPort(clock);
    }

    @Bean
    @Profile("local")
    PresignedRecordingUploadPort s3PresignedRecordingUploadPort(
            S3Presigner presigner,
            Clock clock,
            @Value("${app.storage.recordings-bucket}") String bucketName,
            @Value("${app.storage.presigned-upload-expiry}") Duration expiry
    ) {
        return new S3PresignedRecordingUploadPort(presigner, bucketName, expiry, clock);
    }

    @Bean
    CreateRecordingUploadService createRecordingUploadService(
            RecordingRepository repository,
            PresignedRecordingUploadPort uploadPort,
            Clock clock
    ) {
        return new CreateRecordingUploadService(repository, uploadPort, clock);
    }

    @Bean
    @Profile("!local")
    RecordingObjectVerifier recordingObjectVerifier() {
        return new FakeRecordingObjectVerifier();
    }

    @Bean
    @Profile("local")
    RecordingObjectVerifier s3RecordingObjectVerifier(S3Client s3Client) {
        return new S3RecordingObjectVerifier(s3Client);
    }

    @Bean
    CompleteRecordingUploadService completeRecordingUploadService(
            RecordingRepository repository,
            RecordingObjectVerifier verifier,
            Clock clock
    ) {
        return new CompleteRecordingUploadService(repository, verifier, clock);
    }

    @Bean
    @Profile("!local")
    RecordingAnalysisQueue recordingAnalysisQueue() {
        return new FakeRecordingAnalysisQueue();
    }

    @Bean
    @Profile("local")
    RecordingAnalysisQueue sqsRecordingAnalysisQueue(
            SqsClient sqsClient,
            @Value("${app.queue.recording-analysis-queue-url}") String queueUrl
    ) {
        return new SqsRecordingAnalysisQueue(sqsClient, queueUrl);
    }

    @Bean
    RequestRecordingAnalysisService requestRecordingAnalysisService(
            RecordingRepository repository,
            RecordingAnalysisQueue queue,
            Clock clock
    ) {
        return new RequestRecordingAnalysisService(repository, queue, clock);
    }

    @Bean
    ProcessRecordingAnalysisJobService processRecordingAnalysisJobService(
            RecordingRepository repository,
            RecordingAnalysisResultRepository resultRepository,
            Clock clock
    ) {
        return new ProcessRecordingAnalysisJobService(repository, resultRepository, clock);
    }

    @Bean
    @Profile("local")
    SqsRecordingAnalysisJobPoller sqsRecordingAnalysisJobPoller(
            SqsClient sqsClient,
            RecordingAnalysisJobProcessor processor,
            @Value("${app.queue.recording-analysis-queue-url}") String queueUrl
    ) {
        return new SqsRecordingAnalysisJobPoller(sqsClient, queueUrl, processor);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @Profile("local")
    S3Client s3Client(
            @Value("${app.aws.region}") String region,
            @Value("${app.aws.endpoint}") URI endpoint
    ) {
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(endpoint)
                .credentialsProvider(localStackCredentials())
                .serviceConfiguration(s3Configuration())
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    @Bean
    @Profile("local")
    S3Presigner s3Presigner(
            @Value("${app.aws.region}") String region,
            @Value("${app.aws.endpoint}") URI endpoint
    ) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .endpointOverride(endpoint)
                .credentialsProvider(localStackCredentials())
                .serviceConfiguration(s3Configuration())
                .build();
    }

    @Bean
    @Profile("local")
    SqsClient sqsClient(
            @Value("${app.aws.region}") String region,
            @Value("${app.aws.endpoint}") URI endpoint
    ) {
        return SqsClient.builder()
                .region(Region.of(region))
                .endpointOverride(endpoint)
                .credentialsProvider(localStackCredentials())
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    private static StaticCredentialsProvider localStackCredentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
    }

    private static S3Configuration s3Configuration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
    }
}
