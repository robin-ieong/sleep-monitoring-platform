package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.RecordingNotFoundException;
import com.example.sleep.recordings.RecordingStatus;
import com.example.sleep.recordings.StorageObjectReference;
import com.example.sleep.recordings.infrastructure.InMemoryRecordingRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkRecordingStoredServiceTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-05-26T13:20:00Z");
    private static final Instant STORED_AT = Instant.parse("2026-05-26T13:25:00Z");
    private static final StorageObjectReference STORAGE_OBJECT =
            new StorageObjectReference("sleep-recordings", "recordings/rec-123/audio.m4a");

    private final InMemoryRecordingRepository repository = new InMemoryRecordingRepository();
    private final MarkRecordingStoredService service = new MarkRecordingStoredService(
            repository,
            Clock.fixed(STORED_AT, ZoneOffset.UTC)
    );

    @Test
    void marksExistingRecordingAsStoredAndSavesIt() {
        repository.save(registeredRecording());

        Recording stored = service.markStored(new MarkRecordingStoredCommand(
                new RecordingId("rec-123"),
                STORAGE_OBJECT
        ));

        assertThat(stored.status()).isEqualTo(RecordingStatus.STORED);
        assertThat(stored.storageObject()).contains(STORAGE_OBJECT);
        assertThat(stored.storedAt()).contains(STORED_AT);
        assertThat(repository.findById(new RecordingId("rec-123"))).contains(stored);
    }

    @Test
    void rejectsMissingRecording() {
        assertThatThrownBy(() -> service.markStored(new MarkRecordingStoredCommand(
                new RecordingId("missing-recording"),
                STORAGE_OBJECT
        )))
                .isInstanceOf(RecordingNotFoundException.class)
                .hasMessage("Recording missing-recording was not found");
    }

    @Test
    void rejectsAlreadyStoredRecording() {
        Recording stored = registeredRecording().markStored(STORAGE_OBJECT, STORED_AT);
        repository.save(stored);

        assertThatThrownBy(() -> service.markStored(new MarkRecordingStoredCommand(
                new RecordingId("rec-123"),
                STORAGE_OBJECT
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recording rec-123 must be awaiting upload before it can be marked stored");
    }

    private static Recording registeredRecording() {
        return Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4",
                REGISTERED_AT
        );
    }
}
