package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingNotFoundException;

import java.time.Clock;

public final class RequestRecordingAnalysisService {

    private final RecordingRepository repository;
    private final RecordingAnalysisQueue queue;
    private final Clock clock;

    public RequestRecordingAnalysisService(
            RecordingRepository repository,
            RecordingAnalysisQueue queue,
            Clock clock
    ) {
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        if (queue == null) {
            throw new IllegalArgumentException("queue must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.repository = repository;
        this.queue = queue;
        this.clock = clock;
    }

    public Recording requestAnalysis(RequestRecordingAnalysisCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }

        Recording recording = repository.findById(command.id())
                .orElseThrow(() -> new RecordingNotFoundException(command.id()));
        Recording requested = recording.requestAnalysis(clock.instant());
        Recording saved = repository.save(requested);
        queue.enqueueAnalysis(saved);

        return saved;
    }
}
