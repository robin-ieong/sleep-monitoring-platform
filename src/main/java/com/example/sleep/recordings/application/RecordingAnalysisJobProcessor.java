package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;

public interface RecordingAnalysisJobProcessor {

    Recording process(String messageBody);
}
