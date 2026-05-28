package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.RecordingStatus;
import com.example.sleep.recordings.StorageObjectReference;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcRecordingRepositoryTest {

    private JdbcRecordingRepository repository;

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

        repository = new JdbcRecordingRepository(new JdbcTemplate(dataSource));
    }

    @Test
    void savesAndFindsAwaitingUploadRecording() {
        Recording recording = Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.webm",
                "audio/webm",
                Instant.parse("2026-05-27T09:00:00Z")
        );

        repository.save(recording);

        Optional<Recording> found = repository.findById(new RecordingId("rec-123"));

        assertThat(found).hasValueSatisfying(saved -> {
            assertThat(saved.id()).isEqualTo(new RecordingId("rec-123"));
            assertThat(saved.ownerId()).isEqualTo("user-456");
            assertThat(saved.originalFilename()).isEqualTo("night-audio.webm");
            assertThat(saved.contentType()).isEqualTo("audio/webm");
            assertThat(saved.status()).isEqualTo(RecordingStatus.AWAITING_UPLOAD);
            assertThat(saved.registeredAt()).isEqualTo(Instant.parse("2026-05-27T09:00:00Z"));
            assertThat(saved.storageObject()).isEmpty();
        });
    }

    @Test
    void savesUpdatedLifecycleStateForExistingRecording() {
        Recording registered = Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.webm",
                "audio/webm",
                Instant.parse("2026-05-27T09:00:00Z")
        );
        Recording stored = registered.markStored(
                new StorageObjectReference("sleep-recordings", "recordings/user-456/rec-123/audio"),
                Instant.parse("2026-05-27T09:05:00Z")
        );

        repository.save(registered);
        repository.save(stored);

        assertThat(repository.findById(new RecordingId("rec-123"))).hasValueSatisfying(saved -> {
            assertThat(saved.status()).isEqualTo(RecordingStatus.STORED);
            assertThat(saved.storedAt()).contains(Instant.parse("2026-05-27T09:05:00Z"));
            assertThat(saved.storageObject()).contains(new StorageObjectReference(
                    "sleep-recordings",
                    "recordings/user-456/rec-123/audio"
            ));
        });
    }

    @Test
    void returnsEmptyWhenRecordingDoesNotExist() {
        assertThat(repository.findById(new RecordingId("missing"))).isEmpty();
        assertThat(repository.existsById(new RecordingId("missing"))).isFalse();
    }

    @Test
    void reportsExistingRecordingById() {
        repository.save(Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.webm",
                "audio/webm",
                Instant.parse("2026-05-27T09:00:00Z")
        ));

        assertThat(repository.existsById(new RecordingId("rec-123"))).isTrue();
    }
}
