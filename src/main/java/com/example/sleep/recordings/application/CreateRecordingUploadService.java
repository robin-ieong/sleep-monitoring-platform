package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;

import java.time.Clock;

public final class CreateRecordingUploadService {

    private final RecordingRepository repository;
    private final PresignedRecordingUploadPort uploadPort;
    private final Clock clock;

    public CreateRecordingUploadService(
            RecordingRepository repository,
            PresignedRecordingUploadPort uploadPort,
            Clock clock
    ) {
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        if (uploadPort == null) {
            throw new IllegalArgumentException("uploadPort must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.repository = repository;
        this.uploadPort = uploadPort;
        this.clock = clock;
    }

    public CreateRecordingUploadResult createUpload(CreateRecordingUploadCommand command) {
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
        Recording saved = repository.save(recording);
        PresignedRecordingUpload upload = uploadPort.createUploadFor(saved);

        return new CreateRecordingUploadResult(saved, upload);
    }
}
