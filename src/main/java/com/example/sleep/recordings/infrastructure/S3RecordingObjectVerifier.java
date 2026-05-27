package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.StorageObjectReference;
import com.example.sleep.recordings.application.RecordingObjectVerifier;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

public final class S3RecordingObjectVerifier implements RecordingObjectVerifier {

    private final S3Client s3Client;

    public S3RecordingObjectVerifier(S3Client s3Client) {
        if (s3Client == null) {
            throw new IllegalArgumentException("s3Client must not be null");
        }
        this.s3Client = s3Client;
    }

    @Override
    public boolean exists(StorageObjectReference storageObject) {
        if (storageObject == null) {
            throw new IllegalArgumentException("storageObject must not be null");
        }

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(storageObject.bucketName())
                    .key(storageObject.objectKey())
                    .build());
            return true;
        } catch (NoSuchKeyException exception) {
            return false;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }
}
