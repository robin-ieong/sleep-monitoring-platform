package com.example.sleep.recordings.application;

import com.example.sleep.recordings.StorageObjectReference;

public interface RecordingObjectVerifier {

    boolean exists(StorageObjectReference storageObject);
}
