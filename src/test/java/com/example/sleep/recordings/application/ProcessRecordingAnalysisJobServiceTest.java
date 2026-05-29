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

class ProcessRecordingAnalysisJobServiceTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-05-28T09:00:00Z");
    private static final Instant STORED_AT = Instant.parse("2026-05-28T09:05:00Z");
    private static final Instant ANALYSIS_REQUESTED_AT = Instant.parse("2026-05-28T09:06:00Z");
    private static final Instant ANALYSIS_COMPLETED_AT = Instant.parse("2026-05-28T09:10:00Z");
    private static final StorageObjectReference STORAGE_OBJECT =
            new StorageObjectReference("sleep-recordings", "recordings/user-456/rec-123/audio");

    private final InMemoryRecordingRepository repository = new InMemoryRecordingRepository();
    private final ProcessRecordingAnalysisJobService service = new ProcessRecordingAnalysisJobService(
            repository,
            Clock.fixed(ANALYSIS_COMPLETED_AT, ZoneOffset.UTC)
    );

    @Test
    void completesAnalysisForQueuedRecording() {
        repository.save(analysisRequestedRecording());

        Recording completed = service.process("""
                {"recordingId":"rec-123","status":"ANALYSIS_REQUESTED"}
                """);

        assertThat(completed.status()).isEqualTo(RecordingStatus.ANALYSIS_COMPLETED);
        assertThat(completed.analysisCompletedAt()).contains(ANALYSIS_COMPLETED_AT);
        assertThat(repository.findById(new RecordingId("rec-123"))).contains(completed);
    }

    @Test
    void rejectsMessageForMissingRecording() {
        assertThatThrownBy(() -> service.process("""
                {"recordingId":"missing","status":"ANALYSIS_REQUESTED"}
                """))
                .isInstanceOf(RecordingNotFoundException.class)
                .hasMessage("Recording missing was not found");
    }

    @Test
    void rejectsMessageWhenRecordingIsNotAwaitingAnalysis() {
        repository.save(storedRecording());

        assertThatThrownBy(() -> service.process("""
                {"recordingId":"rec-123","status":"ANALYSIS_REQUESTED"}
                """))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recording rec-123 must have analysis requested before analysis can be completed");
    }

    @Test
    void rejectsUnexpectedMessageStatus() {
        assertThatThrownBy(() -> service.process("""
                {"recordingId":"rec-123","status":"STORED"}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("analysis job status must be ANALYSIS_REQUESTED");
    }

    @Test
    void rejectsMalformedMessage() {
        assertThatThrownBy(() -> service.process("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("analysis job message must include recordingId and status");
    }

    private static Recording analysisRequestedRecording() {
        return storedRecording().requestAnalysis(ANALYSIS_REQUESTED_AT);
    }

    private static Recording storedRecording() {
        return Recording.register(
                        new RecordingId("rec-123"),
                        "user-456",
                        "night-audio.webm",
                        "audio/webm",
                        REGISTERED_AT
                )
                .markStored(STORAGE_OBJECT, STORED_AT);
    }
}
