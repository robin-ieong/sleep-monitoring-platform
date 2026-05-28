package com.example.sleep.recordings.application;

import com.example.sleep.recordings.RecordingId;

public record RequestRecordingAnalysisCommand(RecordingId id) {

    public RequestRecordingAnalysisCommand {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
    }
}
