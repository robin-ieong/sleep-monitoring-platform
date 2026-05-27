package com.example.sleep.recordings.web;

import com.example.sleep.recordings.StorageObjectReference;
import com.example.sleep.recordings.application.PresignedRecordingUpload;

import java.net.URI;
import java.time.Instant;

public record PresignedUploadHttpResponse(
        URI uploadUrl,
        String method,
        StorageObjectReference storageObject,
        Instant expiresAt
) {

    static PresignedUploadHttpResponse from(PresignedRecordingUpload upload) {
        return new PresignedUploadHttpResponse(
                upload.uploadUrl(),
                upload.method(),
                upload.storageObject(),
                upload.expiresAt()
        );
    }
}
