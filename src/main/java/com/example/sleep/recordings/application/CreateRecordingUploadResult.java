package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;

public record CreateRecordingUploadResult(
        Recording recording,
        PresignedRecordingUpload upload
) {

    public CreateRecordingUploadResult {
        if (recording == null) {
            throw new IllegalArgumentException("recording must not be null");
        }
        if (upload == null) {
            throw new IllegalArgumentException("upload must not be null");
        }
    }
}
