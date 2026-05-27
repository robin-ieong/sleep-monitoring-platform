package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;

public interface PresignedRecordingUploadPort {

    PresignedRecordingUpload createUploadFor(Recording recording);
}
