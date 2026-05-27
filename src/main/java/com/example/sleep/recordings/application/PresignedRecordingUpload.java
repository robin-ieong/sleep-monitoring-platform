package com.example.sleep.recordings.application;

import com.example.sleep.recordings.StorageObjectReference;

import java.net.URI;
import java.time.Instant;

public record PresignedRecordingUpload(
        URI uploadUrl,
        String method,
        StorageObjectReference storageObject,
        Instant expiresAt
) {

    public PresignedRecordingUpload {
        if (uploadUrl == null) {
            throw new IllegalArgumentException("uploadUrl must not be null");
        }
        requireNotBlank(method, "method");
        if (storageObject == null) {
            throw new IllegalArgumentException("storageObject must not be null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }
    }

    private static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
