package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.RecordingNotFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

public final class ProcessRecordingAnalysisJobService implements RecordingAnalysisJobProcessor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RecordingRepository repository;
    private final Clock clock;

    public ProcessRecordingAnalysisJobService(RecordingRepository repository, Clock clock) {
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Recording process(String messageBody) {
        AnalysisJobMessage message = parse(messageBody);
        if (!message.status().equals("ANALYSIS_REQUESTED")) {
            throw new IllegalArgumentException("analysis job status must be ANALYSIS_REQUESTED");
        }

        RecordingId recordingId = new RecordingId(message.recordingId());
        Recording recording = repository.findById(recordingId)
                .orElseThrow(() -> new RecordingNotFoundException(recordingId));
        Recording completed = recording.completeAnalysis(clock.instant());

        return repository.save(completed);
    }

    private static AnalysisJobMessage parse(String messageBody) {
        if (messageBody == null || messageBody.isBlank()) {
            throw new IllegalArgumentException("analysis job message must include recordingId and status");
        }

        AnalysisJobMessage message;
        try {
            message = OBJECT_MAPPER.readValue(messageBody, AnalysisJobMessage.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("analysis job message must include recordingId and status", exception);
        }

        if (message.recordingId() == null || message.recordingId().isBlank()
                || message.status() == null || message.status().isBlank()) {
            throw new IllegalArgumentException("analysis job message must include recordingId and status");
        }

        return message;
    }

    private record AnalysisJobMessage(String recordingId, String status) {
    }
}
