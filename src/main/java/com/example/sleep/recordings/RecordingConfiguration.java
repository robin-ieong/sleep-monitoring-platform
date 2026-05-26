package com.example.sleep.recordings;

import com.example.sleep.recordings.application.MarkRecordingStoredService;
import com.example.sleep.recordings.application.RecordingRepository;
import com.example.sleep.recordings.application.RegisterRecordingService;
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
    Clock clock() {
        return Clock.systemUTC();
    }
}
