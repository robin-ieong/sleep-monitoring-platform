package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;

import java.time.Clock;

public final class RegisterRecordingService {

    private final RecordingRepository repository;
    private final Clock clock;

    public RegisterRecordingService(RecordingRepository repository, Clock clock) {
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.repository = repository;
        this.clock = clock;
    }

    public Recording register(RegisterRecordingCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
        if (repository.existsById(command.id())) {
            throw new IllegalStateException("Recording " + command.id() + " already exists");
        }

        Recording recording = Recording.register(
                command.id(),
                command.ownerId(),
                command.originalFilename(),
                command.contentType(),
                clock.instant()
        );

        return repository.save(recording);
    }
}
