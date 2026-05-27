package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.StorageObjectReference;
import com.example.sleep.recordings.application.PresignedRecordingUpload;
import com.example.sleep.recordings.application.PresignedRecordingUploadPort;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Clock;
import java.time.Duration;

public final class S3PresignedRecordingUploadPort implements PresignedRecordingUploadPort {

    private final S3Presigner presigner;
    private final String bucketName;
    private final Duration expiry;
    private final Clock clock;

    public S3PresignedRecordingUploadPort(
            S3Presigner presigner,
            String bucketName,
            Duration expiry,
            Clock clock
    ) {
        if (presigner == null) {
            throw new IllegalArgumentException("presigner must not be null");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalArgumentException("bucketName must not be blank");
        }
        if (expiry == null || expiry.isZero() || expiry.isNegative()) {
            throw new IllegalArgumentException("expiry must be positive");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.presigner = presigner;
        this.bucketName = bucketName;
        this.expiry = expiry;
        this.clock = clock;
    }

    @Override
    public PresignedRecordingUpload createUploadFor(Recording recording) {
        if (recording == null) {
            throw new IllegalArgumentException("recording must not be null");
        }

        String objectKey = "recordings/" + recording.ownerId() + "/" + recording.id() + "/audio";
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(recording.contentType())
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        return new PresignedRecordingUpload(
                java.net.URI.create(presignedRequest.url().toString()),
                "PUT",
                new StorageObjectReference(bucketName, objectKey),
                clock.instant().plus(expiry)
        );
    }
}
