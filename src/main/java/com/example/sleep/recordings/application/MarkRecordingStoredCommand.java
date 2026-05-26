package com.example.sleep.recordings.application;

import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.StorageObjectReference;

public record MarkRecordingStoredCommand(
        RecordingId id,
        StorageObjectReference storageObject
) {

    public MarkRecordingStoredCommand {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (storageObject == null) {
            throw new IllegalArgumentException("storageObject must not be null");
        }
    }
}
