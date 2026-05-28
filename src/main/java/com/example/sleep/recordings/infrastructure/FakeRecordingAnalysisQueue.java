package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.application.RecordingAnalysisQueue;

public final class FakeRecordingAnalysisQueue implements RecordingAnalysisQueue {

    @Override
    public void enqueueAnalysis(Recording recording) {
        if (recording == null) {
            throw new IllegalArgumentException("recording must not be null");
        }
    }
}
