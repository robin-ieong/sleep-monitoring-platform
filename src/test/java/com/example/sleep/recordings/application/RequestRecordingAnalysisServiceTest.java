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

class RequestRecordingAnalysisServiceTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-05-28T09:00:00Z");
    private static final Instant STORED_AT = Instant.parse("2026-05-28T09:05:00Z");
    private static final Instant ANALYSIS_REQUESTED_AT = Instant.parse("2026-05-28T09:06:00Z");
    private static final StorageObjectReference STORAGE_OBJECT =
            new StorageObjectReference("sleep-recordings", "recordings/user-456/rec-123/audio");

    private final InMemoryRecordingRepository repository = new InMemoryRecordingRepository();
    private final RecordingAnalysisQueueFake queue = new RecordingAnalysisQueueFake();
    private final RequestRecordingAnalysisService service = new RequestRecordingAnalysisService(
            repository,
            queue,
            Clock.fixed(ANALYSIS_REQUESTED_AT, ZoneOffset.UTC)
    );

    @Test
    void requestsAnalysisForStoredRecordingAndSavesIt() {
        repository.save(storedRecording());

        Recording requested = service.requestAnalysis(new RequestRecordingAnalysisCommand(new RecordingId("rec-123")));

        assertThat(requested.status()).isEqualTo(RecordingStatus.ANALYSIS_REQUESTED);
        assertThat(requested.analysisRequestedAt()).contains(ANALYSIS_REQUESTED_AT);
        assertThat(repository.findById(new RecordingId("rec-123"))).contains(requested);
        assertThat(queue.recording).isSameAs(requested);
    }

    @Test
    void rejectsMissingRecordingWithoutQueueingAnalysis() {
        assertThatThrownBy(() -> service.requestAnalysis(new RequestRecordingAnalysisCommand(new RecordingId("missing"))))
                .isInstanceOf(RecordingNotFoundException.class)
                .hasMessage("Recording missing was not found");

        assertThat(queue.recording).isNull();
    }

    @Test
    void rejectsRecordingThatIsNotStoredWithoutQueueingAnalysis() {
        repository.save(Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.webm",
                "audio/webm",
                REGISTERED_AT
        ));

        assertThatThrownBy(() -> service.requestAnalysis(new RequestRecordingAnalysisCommand(new RecordingId("rec-123"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recording rec-123 must be STORED before analysis can be requested");

        assertThat(queue.recording).isNull();
    }

    @Test
    void rejectsAlreadyRequestedRecordingWithoutQueueingAnalysisAgain() {
        Recording requested = storedRecording().requestAnalysis(ANALYSIS_REQUESTED_AT);
        repository.save(requested);

        assertThatThrownBy(() -> service.requestAnalysis(new RequestRecordingAnalysisCommand(new RecordingId("rec-123"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recording rec-123 must be STORED before analysis can be requested");

        assertThat(queue.recording).isNull();
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

    private static final class RecordingAnalysisQueueFake implements RecordingAnalysisQueue {

        private Recording recording;

        @Override
        public void enqueueAnalysis(Recording recording) {
            this.recording = recording;
        }
    }
}
