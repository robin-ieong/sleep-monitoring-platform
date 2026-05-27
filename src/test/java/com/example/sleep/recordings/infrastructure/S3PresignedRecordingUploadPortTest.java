package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.application.PresignedRecordingUpload;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class S3PresignedRecordingUploadPortTest {

    private static final Instant NOW = Instant.parse("2026-05-27T15:20:00Z");
    private static final Duration EXPIRY = Duration.ofMinutes(15);

    private final S3PresignerFake presigner = new S3PresignerFake(
            URI.create("http://localhost:4566/sleep-recordings/recordings/user-456/rec-123/audio?signature=abc")
    );
    private final S3PresignedRecordingUploadPort port = new S3PresignedRecordingUploadPort(
            presigner.proxy(),
            "sleep-recordings",
            EXPIRY,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void createsPresignedPutUploadForRecording() {
        PresignedRecordingUpload upload = port.createUploadFor(recording());

        assertThat(upload.uploadUrl()).isEqualTo(presigner.signedUri);
        assertThat(upload.method()).isEqualTo("PUT");
        assertThat(upload.storageObject().bucketName()).isEqualTo("sleep-recordings");
        assertThat(upload.storageObject().objectKey()).isEqualTo("recordings/user-456/rec-123/audio");
        assertThat(upload.expiresAt()).isEqualTo(NOW.plus(EXPIRY));

        PutObjectPresignRequest request = presigner.request;
        assertThat(request.signatureDuration()).isEqualTo(EXPIRY);
        assertThat(request.putObjectRequest().bucket()).isEqualTo("sleep-recordings");
        assertThat(request.putObjectRequest().key()).isEqualTo("recordings/user-456/rec-123/audio");
        assertThat(request.putObjectRequest().contentType()).isEqualTo("audio/mp4");
    }

    private static Recording recording() {
        return Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4",
                NOW
        );
    }

    private static final class S3PresignerFake {

        private final URI signedUri;
        private PutObjectPresignRequest request;

        private S3PresignerFake(URI signedUri) {
            this.signedUri = signedUri;
        }

        private S3Presigner proxy() {
            return (S3Presigner) Proxy.newProxyInstance(
                    S3Presigner.class.getClassLoader(),
                    new Class<?>[]{S3Presigner.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("presignPutObject")) {
                            request = (PutObjectPresignRequest) args[0];
                            return PresignedPutObjectRequest.builder()
                                    .expiration(NOW.plus(EXPIRY))
                                    .isBrowserExecutable(false)
                                    .signedHeaders(Map.of("host", List.of("localhost:4566")))
                                    .httpRequest(SdkHttpFullRequest.builder()
                                            .method(SdkHttpMethod.PUT)
                                            .uri(signedUri)
                                            .build())
                                    .build();
                        }
                        if (method.getName().equals("close")) {
                            return null;
                        }
                        if (method.getName().equals("toString")) {
                            return "S3PresignerFake";
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
