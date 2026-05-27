package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.StorageObjectReference;
import com.example.sleep.recordings.application.RecordingObjectVerifier;

public final class FakeRecordingObjectVerifier implements RecordingObjectVerifier {

    @Override
    public boolean exists(StorageObjectReference storageObject) {
        if (storageObject == null) {
            throw new IllegalArgumentException("storageObject must not be null");
        }
        return true;
    }
}
