package com.example.sleep.recordings;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterRecordingServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-26T10:30:00Z");

    private final InMemoryRecordingRepository repository = new InMemoryRecordingRepository();
    private final RegisterRecordingService service = new RegisterRecordingService(
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void registersRecordingAndStoresItInRepository() {
        Recording recording = service.register(new RegisterRecordingCommand(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4"
        ));

        assertThat(recording.id()).isEqualTo(new RecordingId("rec-123"));
        assertThat(recording.status()).isEqualTo(RecordingStatus.AWAITING_UPLOAD);
        assertThat(recording.registeredAt()).isEqualTo(NOW);
        assertThat(repository.findById(new RecordingId("rec-123"))).contains(recording);
    }

    @Test
    void rejectsDuplicateRecordingId() {
        RegisterRecordingCommand command = new RegisterRecordingCommand(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4"
        );

        service.register(command);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recording rec-123 already exists");
    }

    @Test
    void canStoreUpdatedRecordingLifecycleState() {
        Recording registered = service.register(new RegisterRecordingCommand(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4"
        ));
        Recording stored = registered.markStored(
                new StorageObjectReference("sleep-recordings", "recordings/rec-123/audio.m4a"),
                Instant.parse("2026-05-26T10:35:00Z")
        );

        repository.save(stored);

        assertThat(repository.findById(new RecordingId("rec-123")))
                .hasValueSatisfying(recording -> assertThat(recording.status()).isEqualTo(RecordingStatus.STORED));
    }
}
