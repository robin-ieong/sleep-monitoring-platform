package com.example.sleep.recordings;

import java.time.Instant;

public record RegisterRecordingHttpResponse(
        String id,
        String ownerId,
        String originalFilename,
        String contentType,
        RecordingStatus status,
        Instant registeredAt
) {

    static RegisterRecordingHttpResponse from(Recording recording) {
        return new RegisterRecordingHttpResponse(
                recording.id().value(),
                recording.ownerId(),
                recording.originalFilename(),
                recording.contentType(),
                recording.status(),
                recording.registeredAt()
        );
    }
}
