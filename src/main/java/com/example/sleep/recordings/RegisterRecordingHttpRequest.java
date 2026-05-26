package com.example.sleep.recordings;

public record RegisterRecordingHttpRequest(
        String id,
        String ownerId,
        String originalFilename,
        String contentType
) {

    RegisterRecordingCommand toCommand() {
        return new RegisterRecordingCommand(
                new RecordingId(id),
                ownerId,
                originalFilename,
                contentType
        );
    }
}
