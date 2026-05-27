package com.example.sleep.recordings.web;

import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.application.RegisterRecordingCommand;
import jakarta.validation.constraints.NotBlank;

public record RegisterRecordingHttpRequest(
        @NotBlank(message = "id must not be blank")
        String id,

        @NotBlank(message = "ownerId must not be blank")
        String ownerId,

        @NotBlank(message = "originalFilename must not be blank")
        String originalFilename,

        @NotBlank(message = "contentType must not be blank")
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
