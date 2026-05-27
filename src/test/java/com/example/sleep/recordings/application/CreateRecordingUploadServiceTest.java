package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.RecordingStatus;
import com.example.sleep.recordings.StorageObjectReference;
import com.example.sleep.recordings.infrastructure.InMemoryRecordingRepository;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateRecordingUploadServiceTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-05-27T11:35:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-05-27T11:50:00Z");
    private static final PresignedRecordingUpload UPLOAD = new PresignedRecordingUpload(
            URI.create("https://example.com/upload/rec-123"),
            "PUT",
            new StorageObjectReference("sleep-recordings", "recordings/user-456/rec-123/audio.m4a"),
            EXPIRES_AT
    );

    private final InMemoryRecordingRepository repository = new InMemoryRecordingRepository();
    private final PresignedRecordingUploadFake uploadPort = new PresignedRecordingUploadFake(UPLOAD);
    private final CreateRecordingUploadService service = new CreateRecordingUploadService(
            repository,
            uploadPort,
            Clock.fixed(REGISTERED_AT, ZoneOffset.UTC)
    );

    @Test
    void createsRecordingMetadataAndUploadInstructions() {
        CreateRecordingUploadResult result = service.createUpload(new CreateRecordingUploadCommand(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4"
        ));

        assertThat(result.recording().id()).isEqualTo(new RecordingId("rec-123"));
        assertThat(result.recording().ownerId()).isEqualTo("user-456");
        assertThat(result.recording().status()).isEqualTo(RecordingStatus.AWAITING_UPLOAD);
        assertThat(result.recording().registeredAt()).isEqualTo(REGISTERED_AT);
        assertThat(result.upload()).isEqualTo(UPLOAD);
        assertThat(repository.findById(new RecordingId("rec-123"))).contains(result.recording());
        assertThat(uploadPort.recording).isSameAs(result.recording());
    }

    @Test
    void rejectsDuplicateRecordingIdWithoutCreatingUploadInstructions() {
        service.createUpload(new CreateRecordingUploadCommand(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4"
        ));
        uploadPort.reset();

        assertThatThrownBy(() -> service.createUpload(new CreateRecordingUploadCommand(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4"
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recording rec-123 already exists");

        assertThat(uploadPort.wasCalled()).isFalse();
    }

    @Test
    void commandRejectsBlankOriginalFilename() {
        assertThatThrownBy(() -> new CreateRecordingUploadCommand(
                new RecordingId("rec-123"),
                "user-456",
                " ",
                "audio/mp4"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("originalFilename must not be blank");
    }

    private static final class PresignedRecordingUploadFake implements PresignedRecordingUploadPort {

        private final PresignedRecordingUpload upload;
        private Recording recording;

        private PresignedRecordingUploadFake(PresignedRecordingUpload upload) {
            this.upload = upload;
        }

        @Override
        public PresignedRecordingUpload createUploadFor(Recording recording) {
            this.recording = recording;
            return upload;
        }

        private boolean wasCalled() {
            return recording != null;
        }

        private void reset() {
            recording = null;
        }
    }
}
