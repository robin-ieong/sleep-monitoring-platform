package com.example.sleep.recordings.web;

import com.example.sleep.recordings.StorageObjectReference;
import jakarta.validation.constraints.NotBlank;

public record MarkRecordingStoredHttpRequest(
        @NotBlank(message = "bucketName must not be blank")
        String bucketName,

        @NotBlank(message = "objectKey must not be blank")
        String objectKey
) {

    StorageObjectReference toStorageObjectReference() {
        return new StorageObjectReference(bucketName, objectKey);
    }
}
