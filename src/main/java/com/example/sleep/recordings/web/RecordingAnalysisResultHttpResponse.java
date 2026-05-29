package com.example.sleep.recordings.web;

import com.example.sleep.recordings.RecordingAnalysisResult;
import com.example.sleep.recordings.RecordingAnalysisResultStatus;

import java.time.Instant;

public record RecordingAnalysisResultHttpResponse(
        String recordingId,
        RecordingAnalysisResultStatus status,
        Instant completedAt,
        String summary
) {

    static RecordingAnalysisResultHttpResponse from(RecordingAnalysisResult result) {
        return new RecordingAnalysisResultHttpResponse(
                result.recordingId().value(),
                result.status(),
                result.completedAt(),
                result.summary()
        );
    }
}
