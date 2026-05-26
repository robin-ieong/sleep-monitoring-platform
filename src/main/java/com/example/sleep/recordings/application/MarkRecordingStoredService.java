package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingNotFoundException;

import java.time.Clock;

public final class MarkRecordingStoredService {

    private final RecordingRepository repository;
    private final Clock clock;

    public MarkRecordingStoredService(RecordingRepository repository, Clock clock) {
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.repository = repository;
        this.clock = clock;
    }

    public Recording markStored(MarkRecordingStoredCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }

        Recording recording = repository.findById(command.id())
                .orElseThrow(() -> new RecordingNotFoundException(command.id()));
        Recording stored = recording.markStored(command.storageObject(), clock.instant());

        return repository.save(stored);
    }
}
