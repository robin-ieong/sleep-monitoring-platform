package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;

public interface RecordingAnalysisQueue {

    void enqueueAnalysis(Recording recording);
}
