package com.example.sleep.recordings;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordingTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-05-26T08:00:00Z");
    private static final Instant STORED_AT = Instant.parse("2026-05-26T08:05:00Z");
    private static final Instant ANALYSIS_REQUESTED_AT = Instant.parse("2026-05-26T08:06:00Z");
    private static final Instant ANALYSIS_COMPLETED_AT = Instant.parse("2026-05-26T08:30:00Z");
    private static final StorageObjectReference STORAGE_OBJECT =
            new StorageObjectReference("sleep-recordings", "recordings/rec-123/audio.m4a");

    @Test
    void registersRecordingMetadataBeforeUploadIsStored() {
        Recording recording = Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4",
                REGISTERED_AT
        );

        assertThat(recording.id()).isEqualTo(new RecordingId("rec-123"));
        assertThat(recording.ownerId()).isEqualTo("user-456");
        assertThat(recording.originalFilename()).isEqualTo("night-audio.m4a");
        assertThat(recording.contentType()).isEqualTo("audio/mp4");
        assertThat(recording.status()).isEqualTo(RecordingStatus.AWAITING_UPLOAD);
        assertThat(recording.registeredAt()).isEqualTo(REGISTERED_AT);
        assertThat(recording.storedAt()).isEmpty();
        assertThat(recording.storageObject()).isEmpty();
    }

    @Test
    void marksAwaitingRecordingAsStored() {
        Recording recording = registeredRecording();

        Recording stored = recording.markStored(STORAGE_OBJECT, STORED_AT);

        assertThat(stored.status()).isEqualTo(RecordingStatus.STORED);
        assertThat(stored.storageObject()).contains(STORAGE_OBJECT);
        assertThat(stored.storedAt()).contains(STORED_AT);
    }

    @Test
    void requestsAnalysisOnlyAfterRecordingIsStored() {
        Recording stored = registeredRecording().markStored(STORAGE_OBJECT, STORED_AT);

        Recording requested = stored.requestAnalysis(ANALYSIS_REQUESTED_AT);

        assertThat(requested.status()).isEqualTo(RecordingStatus.ANALYSIS_REQUESTED);
        assertThat(requested.analysisRequestedAt()).contains(ANALYSIS_REQUESTED_AT);
    }

    @Test
    void completesAnalysisOnlyAfterAnalysisWasRequested() {
        Recording requested = registeredRecording()
                .markStored(STORAGE_OBJECT, STORED_AT)
                .requestAnalysis(ANALYSIS_REQUESTED_AT);

        Recording completed = requested.completeAnalysis(ANALYSIS_COMPLETED_AT);

        assertThat(completed.status()).isEqualTo(RecordingStatus.ANALYSIS_COMPLETED);
        assertThat(completed.analysisCompletedAt()).contains(ANALYSIS_COMPLETED_AT);
    }

    @Test
    void rejectsInvalidLifecycleTransition() {
        Recording recording = registeredRecording();

        assertThatThrownBy(() -> recording.requestAnalysis(ANALYSIS_REQUESTED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recording rec-123 must be STORED before analysis can be requested");
    }

    @Test
    void rejectsBlankMetadata() {
        assertThatThrownBy(() -> Recording.register(
                new RecordingId("rec-123"),
                " ",
                "night-audio.m4a",
                "audio/mp4",
                REGISTERED_AT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ownerId must not be blank");
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
