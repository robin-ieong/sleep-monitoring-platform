package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingNotFoundException;
import com.example.sleep.recordings.RecordingStatus;
import com.example.sleep.recordings.StorageObjectReference;

import java.time.Clock;

public final class StoreRecordingContentService {

    private final RecordingRepository repository;
    private final RecordingStorage storage;
    private final Clock clock;

    public StoreRecordingContentService(
            RecordingRepository repository,
            RecordingStorage storage,
            Clock clock
    ) {
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        if (storage == null) {
            throw new IllegalArgumentException("storage must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.repository = repository;
        this.storage = storage;
        this.clock = clock;
    }

    public Recording store(StoreRecordingContentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }

        Recording recording = repository.findById(command.id())
                .orElseThrow(() -> new RecordingNotFoundException(command.id()));

        if (recording.status() != RecordingStatus.AWAITING_UPLOAD) {
            throw new IllegalStateException("Recording " + recording.id() + " must be awaiting upload before it can be stored");
        }

        StorageObjectReference storageObject = storage.store(recording, command.content());
        Recording stored = recording.markStored(storageObject, clock.instant());

        return repository.save(stored);
    }
}
