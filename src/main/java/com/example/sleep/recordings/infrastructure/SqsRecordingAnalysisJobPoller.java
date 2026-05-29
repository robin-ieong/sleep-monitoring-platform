package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.application.RecordingAnalysisJobProcessor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

public final class SqsRecordingAnalysisJobPoller {

    private static final int MAX_MESSAGES = 10;
    private static final int WAIT_TIME_SECONDS = 10;

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final RecordingAnalysisJobProcessor processor;

    public SqsRecordingAnalysisJobPoller(
            SqsClient sqsClient,
            String queueUrl,
            RecordingAnalysisJobProcessor processor
    ) {
        if (sqsClient == null) {
            throw new IllegalArgumentException("sqsClient must not be null");
        }
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalArgumentException("queueUrl must not be blank");
        }
        if (processor == null) {
            throw new IllegalArgumentException("processor must not be null");
        }
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.processor = processor;
    }

    /**
     * Polls SQS once and deletes a message only after its body is processed successfully.
     */
    public int pollOnce() {
        int processedCount = 0;
        for (Message message : receiveMessages()) {
            try {
                processor.process(message.body());
                deleteMessage(message);
                processedCount++;
            } catch (RuntimeException ignored) {
                // Leaving the message undeleted lets SQS make it visible again for retry.
            }
        }
        return processedCount;
    }

    private Iterable<Message> receiveMessages() {
        return sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(MAX_MESSAGES)
                        .waitTimeSeconds(WAIT_TIME_SECONDS)
                        .build())
                .messages();
    }

    private void deleteMessage(Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }
}
