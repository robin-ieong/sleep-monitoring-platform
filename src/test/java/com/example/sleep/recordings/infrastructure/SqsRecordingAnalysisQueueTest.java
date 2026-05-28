package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.StorageObjectReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.lang.reflect.Proxy;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SqsRecordingAnalysisQueueTest {

    @Test
    void sendsAnalysisMessageToConfiguredQueue() {
        SqsClientFake sqsClient = new SqsClientFake();
        SqsRecordingAnalysisQueue queue = new SqsRecordingAnalysisQueue(
                sqsClient.client(),
                "http://localhost:4566/000000000000/sleep-recording-analysis"
        );

        queue.enqueueAnalysis(storedRecording().requestAnalysis(Instant.parse("2026-05-28T09:06:00Z")));

        assertThat(sqsClient.request.queueUrl())
                .isEqualTo("http://localhost:4566/000000000000/sleep-recording-analysis");
        assertThat(sqsClient.request.messageBody())
                .isEqualTo("{\"recordingId\":\"rec-123\",\"status\":\"ANALYSIS_REQUESTED\"}");
    }

    private static Recording storedRecording() {
        return Recording.register(
                        new RecordingId("rec-123"),
                        "user-456",
                        "night-audio.webm",
                        "audio/webm",
                        Instant.parse("2026-05-28T09:00:00Z")
                )
                .markStored(
                        new StorageObjectReference("sleep-recordings", "recordings/user-456/rec-123/audio"),
                        Instant.parse("2026-05-28T09:05:00Z")
                );
    }

    private static final class SqsClientFake {

        private SendMessageRequest request;

        private SqsClient client() {
            return (SqsClient) Proxy.newProxyInstance(
                    SqsClient.class.getClassLoader(),
                    new Class<?>[]{SqsClient.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("sendMessage")) {
                            request = (SendMessageRequest) args[0];
                            return SendMessageResponse.builder().messageId("message-123").build();
                        }
                        if (method.getName().equals("serviceName")) {
                            return "sqs";
                        }
                        if (method.getName().equals("close")) {
                            return null;
                        }
                        if (method.getName().equals("toString")) {
                            return "SqsClientFake";
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
