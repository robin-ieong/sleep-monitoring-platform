package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.StorageObjectReference;
import com.example.sleep.recordings.application.PresignedRecordingUpload;
import com.example.sleep.recordings.application.PresignedRecordingUploadPort;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

public final class FakePresignedRecordingUploadPort implements PresignedRecordingUploadPort {

    private static final String BUCKET_NAME = "sleep-recordings";
    private static final Duration EXPIRY = Duration.ofMinutes(15);

    private final Clock clock;

    public FakePresignedRecordingUploadPort(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.clock = clock;
    }

    @Override
    public PresignedRecordingUpload createUploadFor(Recording recording) {
        if (recording == null) {
            throw new IllegalArgumentException("recording must not be null");
        }

        String objectKey = "recordings/" + recording.ownerId() + "/" + recording.id() + "/audio";
        return new PresignedRecordingUpload(
                URI.create("https://localstack.example.invalid/" + BUCKET_NAME + "/" + objectKey),
                "PUT",
                new StorageObjectReference(BUCKET_NAME, objectKey),
                clock.instant().plus(EXPIRY)
        );
    }
}
