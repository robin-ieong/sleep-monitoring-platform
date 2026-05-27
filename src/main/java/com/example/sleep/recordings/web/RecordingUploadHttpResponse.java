package com.example.sleep.recordings.web;

import com.example.sleep.recordings.application.CreateRecordingUploadResult;

public record RecordingUploadHttpResponse(
        RecordingHttpResponse recording,
        PresignedUploadHttpResponse upload
) {

    static RecordingUploadHttpResponse from(CreateRecordingUploadResult result) {
        return new RecordingUploadHttpResponse(
                RecordingHttpResponse.from(result.recording()),
                PresignedUploadHttpResponse.from(result.upload())
        );
    }
}
