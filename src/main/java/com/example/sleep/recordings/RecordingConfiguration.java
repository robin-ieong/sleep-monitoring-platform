package com.example.sleep.recordings;

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
    Clock clock() {
        return Clock.systemUTC();
    }
}
