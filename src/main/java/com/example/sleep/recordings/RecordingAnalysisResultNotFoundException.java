package com.example.sleep.recordings;

public final class RecordingAnalysisResultNotFoundException extends RuntimeException {

    public RecordingAnalysisResultNotFoundException(RecordingId recordingId) {
        super("Analysis result for recording " + recordingId + " was not found");
    }
}
