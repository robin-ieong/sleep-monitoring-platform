package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingAnalysisResult;
import com.example.sleep.recordings.RecordingAnalysisResultStatus;
import com.example.sleep.recordings.RecordingId;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcRecordingAnalysisResultRepositoryTest {

    private JdbcRecordingAnalysisResultRepository repository;
    private JdbcRecordingRepository recordingRepository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcRecordingAnalysisResultRepository(jdbcTemplate);
        recordingRepository = new JdbcRecordingRepository(jdbcTemplate);
    }

    @Test
    void savesAndFindsAnalysisResult() {
        RecordingAnalysisResult result = RecordingAnalysisResult.placeholderCompleted(
                new RecordingId("rec-123"),
                Instant.parse("2026-05-29T16:00:00Z")
        );
        recordingRepository.save(recording());

        repository.save(result);

        Optional<RecordingAnalysisResult> found = repository.findByRecordingId(new RecordingId("rec-123"));

        assertThat(found).hasValueSatisfying(saved -> {
            assertThat(saved.recordingId()).isEqualTo(new RecordingId("rec-123"));
            assertThat(saved.status()).isEqualTo(RecordingAnalysisResultStatus.PLACEHOLDER_COMPLETED);
            assertThat(saved.completedAt()).isEqualTo(Instant.parse("2026-05-29T16:00:00Z"));
            assertThat(saved.summary()).isEqualTo("Analysis job completed; audio analysis is not implemented yet.");
        });
    }

    @Test
    void updatesExistingAnalysisResultForRecording() {
        recordingRepository.save(recording());
        repository.save(RecordingAnalysisResult.placeholderCompleted(
                new RecordingId("rec-123"),
                Instant.parse("2026-05-29T16:00:00Z")
        ));

        RecordingAnalysisResult updated = RecordingAnalysisResult.rehydrate(
                new RecordingId("rec-123"),
                RecordingAnalysisResultStatus.PLACEHOLDER_COMPLETED,
                Instant.parse("2026-05-29T16:05:00Z"),
                "Updated placeholder summary."
        );

        repository.save(updated);

        assertThat(repository.findByRecordingId(new RecordingId("rec-123"))).contains(updated);
    }

    @Test
    void returnsEmptyWhenResultDoesNotExist() {
        assertThat(repository.findByRecordingId(new RecordingId("missing"))).isEmpty();
    }

    private static Recording recording() {
        return Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.webm",
                "audio/webm",
                Instant.parse("2026-05-29T15:55:00Z")
        );
    }
}
