package com.example.sleep.recordings;

import com.example.sleep.recordings.application.CompleteRecordingUploadService;
import com.example.sleep.recordings.application.CreateRecordingUploadService;
import com.example.sleep.recordings.application.MarkRecordingStoredService;
import com.example.sleep.recordings.application.PresignedRecordingUploadPort;
import com.example.sleep.recordings.application.RecordingObjectVerifier;
import com.example.sleep.recordings.application.RecordingRepository;
import com.example.sleep.recordings.application.RegisterRecordingService;
import com.example.sleep.recordings.infrastructure.FakePresignedRecordingUploadPort;
import com.example.sleep.recordings.infrastructure.FakeRecordingObjectVerifier;
import com.example.sleep.recordings.infrastructure.InMemoryRecordingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RecordingConfiguration {

    @Bean
    RecordingRepository recordingRepository() {
        return new InMemoryRecordingRepository();
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
    PresignedRecordingUploadPort presignedRecordingUploadPort(Clock clock) {
        return new FakePresignedRecordingUploadPort(clock);
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
    RecordingObjectVerifier recordingObjectVerifier() {
        return new FakeRecordingObjectVerifier();
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
    Clock clock() {
        return Clock.systemUTC();
    }
}
