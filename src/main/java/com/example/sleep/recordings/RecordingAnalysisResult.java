package com.example.sleep.recordings;

import java.time.Instant;
import java.util.Objects;

public final class RecordingAnalysisResult {

    private static final String PLACEHOLDER_SUMMARY =
            "Analysis job completed; audio analysis is not implemented yet.";

    private final RecordingId recordingId;
    private final RecordingAnalysisResultStatus status;
    private final Instant completedAt;
    private final String summary;

    private RecordingAnalysisResult(
            RecordingId recordingId,
            RecordingAnalysisResultStatus status,
            Instant completedAt,
            String summary
    ) {
        if (recordingId == null) {
            throw new IllegalArgumentException("recordingId must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (completedAt == null) {
            throw new IllegalArgumentException("completedAt must not be null");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        this.recordingId = recordingId;
        this.status = status;
        this.completedAt = completedAt;
        this.summary = summary;
    }

    public static RecordingAnalysisResult placeholderCompleted(RecordingId recordingId, Instant completedAt) {
        return new RecordingAnalysisResult(
                recordingId,
                RecordingAnalysisResultStatus.PLACEHOLDER_COMPLETED,
                completedAt,
                PLACEHOLDER_SUMMARY
        );
    }

    public static RecordingAnalysisResult rehydrate(
            RecordingId recordingId,
            RecordingAnalysisResultStatus status,
            Instant completedAt,
            String summary
    ) {
        return new RecordingAnalysisResult(recordingId, status, completedAt, summary);
    }

    public RecordingId recordingId() {
        return recordingId;
    }

    public RecordingAnalysisResultStatus status() {
        return status;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public String summary() {
        return summary;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RecordingAnalysisResult that)) {
            return false;
        }
        return recordingId.equals(that.recordingId)
                && status == that.status
                && completedAt.equals(that.completedAt)
                && summary.equals(that.summary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordingId, status, completedAt, summary);
    }
}
