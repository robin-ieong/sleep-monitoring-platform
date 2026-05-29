package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.RecordingAnalysisResult;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.application.RecordingAnalysisResultRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryRecordingAnalysisResultRepository implements RecordingAnalysisResultRepository {

    private final Map<RecordingId, RecordingAnalysisResult> results = new HashMap<>();

    @Override
    public RecordingAnalysisResult save(RecordingAnalysisResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        results.put(result.recordingId(), result);
        return result;
    }

    @Override
    public Optional<RecordingAnalysisResult> findByRecordingId(RecordingId recordingId) {
        if (recordingId == null) {
            throw new IllegalArgumentException("recordingId must not be null");
        }
        return Optional.ofNullable(results.get(recordingId));
    }
}
