package com.example.sleep.recordings;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordingAnalysisResultTest {

    @Test
    void createsPlaceholderResultForCompletedAnalysisJob() {
        RecordingAnalysisResult result = RecordingAnalysisResult.placeholderCompleted(
                new RecordingId("rec-123"),
                Instant.parse("2026-05-29T16:00:00Z")
        );

        assertThat(result.recordingId()).isEqualTo(new RecordingId("rec-123"));
        assertThat(result.status()).isEqualTo(RecordingAnalysisResultStatus.PLACEHOLDER_COMPLETED);
        assertThat(result.completedAt()).isEqualTo(Instant.parse("2026-05-29T16:00:00Z"));
        assertThat(result.summary()).isEqualTo("Analysis job completed; audio analysis is not implemented yet.");
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertThatThrownBy(() -> RecordingAnalysisResult.placeholderCompleted(
                null,
                Instant.parse("2026-05-29T16:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("recordingId must not be null");

        assertThatThrownBy(() -> RecordingAnalysisResult.placeholderCompleted(
                new RecordingId("rec-123"),
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("completedAt must not be null");
    }
}
