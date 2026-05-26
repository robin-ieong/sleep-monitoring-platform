package com.example.sleep.recordings;

public record RegisterRecordingCommand(
        RecordingId id,
        String ownerId,
        String originalFilename,
        String contentType
) {

    public RegisterRecordingCommand {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        requireNotBlank(ownerId, "ownerId");
        requireNotBlank(originalFilename, "originalFilename");
        requireNotBlank(contentType, "contentType");
    }

    private static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
