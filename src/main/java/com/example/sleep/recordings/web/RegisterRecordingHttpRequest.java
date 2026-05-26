package com.example.sleep.recordings.web;

import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.application.RegisterRecordingCommand;

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
