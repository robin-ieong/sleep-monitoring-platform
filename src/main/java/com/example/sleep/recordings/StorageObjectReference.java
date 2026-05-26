package com.example.sleep.recordings;

public record StorageObjectReference(String bucketName, String objectKey) {

    public StorageObjectReference {
        requireNotBlank(bucketName, "bucketName");
        requireNotBlank(objectKey, "objectKey");
    }

    private static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
