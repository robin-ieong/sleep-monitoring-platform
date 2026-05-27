package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.StorageObjectReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3RecordingObjectVerifierTest {

    private static final StorageObjectReference STORAGE_OBJECT =
            new StorageObjectReference("sleep-recordings", "recordings/user-456/rec-123/audio");

    private final S3ClientFake s3Client = new S3ClientFake(null);
    private final S3RecordingObjectVerifier verifier = new S3RecordingObjectVerifier(s3Client.proxy());

    @Test
    void returnsTrueWhenHeadObjectSucceeds() {
        assertThat(verifier.exists(STORAGE_OBJECT)).isTrue();

        assertThat(s3Client.request.bucket()).isEqualTo("sleep-recordings");
        assertThat(s3Client.request.key()).isEqualTo("recordings/user-456/rec-123/audio");
    }

    @Test
    void returnsFalseWhenObjectDoesNotExist() {
        S3RecordingObjectVerifier verifier = new S3RecordingObjectVerifier(new S3ClientFake(
                NoSuchKeyException.builder().message("missing").build()
        ).proxy());

        assertThat(verifier.exists(STORAGE_OBJECT)).isFalse();
    }

    @Test
    void returnsFalseForGenericS3NotFoundResponse() {
        S3RecordingObjectVerifier verifier = new S3RecordingObjectVerifier(new S3ClientFake(
                S3Exception.builder()
                        .statusCode(404)
                        .build()
        ).proxy());

        assertThat(verifier.exists(STORAGE_OBJECT)).isFalse();
    }

    @Test
    void propagatesUnexpectedS3Errors() {
        S3RecordingObjectVerifier verifier = new S3RecordingObjectVerifier(new S3ClientFake(
                S3Exception.builder()
                        .statusCode(500)
                        .build()
        ).proxy());

        assertThatThrownBy(() -> verifier.exists(STORAGE_OBJECT))
                .isInstanceOf(S3Exception.class);
    }

    private static final class S3ClientFake {

        private final RuntimeException exception;
        private HeadObjectRequest request;

        private S3ClientFake(RuntimeException exception) {
            this.exception = exception;
        }

        private S3Client proxy() {
            return (S3Client) Proxy.newProxyInstance(
                    S3Client.class.getClassLoader(),
                    new Class<?>[]{S3Client.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("headObject")) {
                            request = (HeadObjectRequest) args[0];
                            if (exception != null) {
                                throw exception;
                            }
                            return HeadObjectResponse.builder().build();
                        }
                        if (method.getName().equals("close")) {
                            return null;
                        }
                        if (method.getName().equals("toString")) {
                            return "S3ClientFake";
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
