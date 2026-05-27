package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingNotFoundException;
import com.example.sleep.recordings.RecordingStatus;

import java.time.Clock;

public final class CompleteRecordingUploadService {

    private final RecordingRepository repository;
    private final RecordingObjectVerifier verifier;
    private final Clock clock;

    public CompleteRecordingUploadService(
            RecordingRepository repository,
            RecordingObjectVerifier verifier,
            Clock clock
    ) {
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        if (verifier == null) {
            throw new IllegalArgumentException("verifier must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.repository = repository;
        this.verifier = verifier;
        this.clock = clock;
    }

    public Recording completeUpload(CompleteRecordingUploadCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }

        Recording recording = repository.findById(command.id())
                .orElseThrow(() -> new RecordingNotFoundException(command.id()));
        if (recording.status() != RecordingStatus.AWAITING_UPLOAD) {
            throw new IllegalStateException("Recording " + recording.id() + " must be awaiting upload before upload can be completed");
        }
        if (!verifier.exists(command.storageObject())) {
            throw new IllegalStateException(
                    "Recording object " + command.storageObject().objectKey()
                            + " does not exist in bucket " + command.storageObject().bucketName()
            );
        }

        Recording stored = recording.markStored(command.storageObject(), clock.instant());
        return repository.save(stored);
    }
}
