package com.example.sleep.recordings.web;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingStatus;
import com.example.sleep.recordings.StorageObjectReference;

import java.time.Instant;

public record RecordingHttpResponse(
        String id,
        String ownerId,
        String originalFilename,
        String contentType,
        RecordingStatus status,
        Instant registeredAt,
        Instant storedAt,
        StorageObjectReference storageObject
) {

    static RecordingHttpResponse from(Recording recording) {
        return new RecordingHttpResponse(
                recording.id().value(),
                recording.ownerId(),
                recording.originalFilename(),
                recording.contentType(),
                recording.status(),
                recording.registeredAt(),
                recording.storedAt().orElse(null),
                recording.storageObject().orElse(null)
        );
    }
}
