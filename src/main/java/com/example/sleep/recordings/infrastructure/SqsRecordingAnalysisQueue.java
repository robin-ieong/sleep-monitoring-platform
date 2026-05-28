package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.application.RecordingAnalysisQueue;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public final class SqsRecordingAnalysisQueue implements RecordingAnalysisQueue {

    private final SqsClient sqsClient;
    private final String queueUrl;

    public SqsRecordingAnalysisQueue(SqsClient sqsClient, String queueUrl) {
        if (sqsClient == null) {
            throw new IllegalArgumentException("sqsClient must not be null");
        }
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalArgumentException("queueUrl must not be blank");
        }
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void enqueueAnalysis(Recording recording) {
        if (recording == null) {
            throw new IllegalArgumentException("recording must not be null");
        }

        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody(recording))
                .build());
    }

    private static String messageBody(Recording recording) {
        return "{\"recordingId\":\"" + jsonEscape(recording.id().value())
                + "\",\"status\":\"" + recording.status().name() + "\"}";
    }

    private static String jsonEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
