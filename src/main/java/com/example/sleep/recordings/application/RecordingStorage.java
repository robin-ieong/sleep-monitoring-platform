package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.StorageObjectReference;

public interface RecordingStorage {

    StorageObjectReference store(Recording recording, byte[] content);
}
