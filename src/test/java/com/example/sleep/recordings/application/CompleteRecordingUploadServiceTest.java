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

class CompleteRecordingUploadServiceTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-05-27T12:30:00Z");
    private static final Instant STORED_AT = Instant.parse("2026-05-27T12:35:00Z");
    private static final StorageObjectReference STORAGE_OBJECT =
            new StorageObjectReference("sleep-recordings", "recordings/user-456/rec-123/audio.m4a");

    private final InMemoryRecordingRepository repository = new InMemoryRecordingRepository();
    private final RecordingObjectVerifierFake verifier = new RecordingObjectVerifierFake(true);
    private final CompleteRecordingUploadService service = new CompleteRecordingUploadService(
            repository,
            verifier,
            Clock.fixed(STORED_AT, ZoneOffset.UTC)
    );

    @Test
    void completesUploadWhenObjectExistsAndMarksRecordingStored() {
        repository.save(registeredRecording());

        Recording stored = service.completeUpload(new CompleteRecordingUploadCommand(
                new RecordingId("rec-123"),
                STORAGE_OBJECT
        ));

        assertThat(stored.status()).isEqualTo(RecordingStatus.STORED);
        assertThat(stored.storageObject()).contains(STORAGE_OBJECT);
        assertThat(stored.storedAt()).contains(STORED_AT);
        assertThat(repository.findById(new RecordingId("rec-123"))).contains(stored);
        assertThat(verifier.storageObject).isEqualTo(STORAGE_OBJECT);
    }

    @Test
    void rejectsMissingRecordingWithoutVerifyingObject() {
        assertThatThrownBy(() -> service.completeUpload(new CompleteRecordingUploadCommand(
                new RecordingId("missing-recording"),
                STORAGE_OBJECT
        )))
                .isInstanceOf(RecordingNotFoundException.class)
                .hasMessage("Recording missing-recording was not found");

        assertThat(verifier.wasCalled()).isFalse();
    }

    @Test
    void rejectsUploadWhenObjectDoesNotExist() {
        repository.save(registeredRecording());
        RecordingObjectVerifierFake verifier = new RecordingObjectVerifierFake(false);
        CompleteRecordingUploadService service = new CompleteRecordingUploadService(
                repository,
                verifier,
                Clock.fixed(STORED_AT, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> service.completeUpload(new CompleteRecordingUploadCommand(
                new RecordingId("rec-123"),
                STORAGE_OBJECT
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recording object recordings/user-456/rec-123/audio.m4a does not exist in bucket sleep-recordings");

        assertThat(repository.findById(new RecordingId("rec-123")))
                .hasValueSatisfying(recording -> assertThat(recording.status()).isEqualTo(RecordingStatus.AWAITING_UPLOAD));
    }

    @Test
    void rejectsAlreadyStoredRecordingWithoutVerifyingObject() {
        repository.save(registeredRecording().markStored(STORAGE_OBJECT, STORED_AT));

        assertThatThrownBy(() -> service.completeUpload(new CompleteRecordingUploadCommand(
                new RecordingId("rec-123"),
                STORAGE_OBJECT
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recording rec-123 must be awaiting upload before upload can be completed");

        assertThat(verifier.wasCalled()).isFalse();
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

    private static final class RecordingObjectVerifierFake implements RecordingObjectVerifier {

        private final boolean exists;
        private StorageObjectReference storageObject;

        private RecordingObjectVerifierFake(boolean exists) {
            this.exists = exists;
        }

        @Override
        public boolean exists(StorageObjectReference storageObject) {
            this.storageObject = storageObject;
            return exists;
        }

        private boolean wasCalled() {
            return storageObject != null;
        }
    }
}
