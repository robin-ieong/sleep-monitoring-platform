package com.example.sleep.recordings.web;

import com.example.sleep.recordings.StorageObjectReference;

public record MarkRecordingStoredHttpRequest(
        String bucketName,
        String objectKey
) {

    StorageObjectReference toStorageObjectReference() {
        return new StorageObjectReference(bucketName, objectKey);
    }
}
