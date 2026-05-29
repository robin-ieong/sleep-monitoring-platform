package com.example.sleep.recordings.application;

import com.example.sleep.recordings.RecordingAnalysisResult;
import com.example.sleep.recordings.RecordingId;

import java.util.Optional;

public interface RecordingAnalysisResultRepository {

    RecordingAnalysisResult save(RecordingAnalysisResult result);

    Optional<RecordingAnalysisResult> findByRecordingId(RecordingId recordingId);
}
