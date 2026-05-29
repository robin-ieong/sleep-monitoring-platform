package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.application.RecordingAnalysisJobProcessor;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqsRecordingAnalysisJobPollerTest {

    private static final String QUEUE_URL = "http://localhost:4566/000000000000/sleep-recording-analysis";

    @Test
    void pollsMessagesProcessesEachBodyAndDeletesSuccessfulMessages() {
        SqsClientFake sqsClient = new SqsClientFake(List.of(
                message("message-1", "receipt-1", "{\"recordingId\":\"rec-1\",\"status\":\"ANALYSIS_REQUESTED\"}"),
                message("message-2", "receipt-2", "{\"recordingId\":\"rec-2\",\"status\":\"ANALYSIS_REQUESTED\"}")
        ));
        RecordingAnalysisJobProcessorFake processor = new RecordingAnalysisJobProcessorFake();
        SqsRecordingAnalysisJobPoller poller = new SqsRecordingAnalysisJobPoller(
                sqsClient.client(),
                QUEUE_URL,
                processor
        );

        int processedCount = poller.pollOnce();

        assertThat(processedCount).isEqualTo(2);
        assertThat(sqsClient.receiveRequest.queueUrl()).isEqualTo(QUEUE_URL);
        assertThat(sqsClient.receiveRequest.maxNumberOfMessages()).isEqualTo(10);
        assertThat(sqsClient.receiveRequest.waitTimeSeconds()).isEqualTo(10);
        assertThat(processor.processedBodies).containsExactly(
                "{\"recordingId\":\"rec-1\",\"status\":\"ANALYSIS_REQUESTED\"}",
                "{\"recordingId\":\"rec-2\",\"status\":\"ANALYSIS_REQUESTED\"}"
        );
        assertThat(sqsClient.deleteRequests)
                .extracting(DeleteMessageRequest::receiptHandle)
                .containsExactly("receipt-1", "receipt-2");
    }

    @Test
    void doesNotDeleteMessageWhenProcessingFails() {
        SqsClientFake sqsClient = new SqsClientFake(List.of(
                message("message-1", "receipt-1", "{\"recordingId\":\"rec-1\",\"status\":\"ANALYSIS_REQUESTED\"}"),
                message("message-2", "receipt-2", "{\"recordingId\":\"rec-2\",\"status\":\"ANALYSIS_REQUESTED\"}")
        ));
        RecordingAnalysisJobProcessorFake processor = new RecordingAnalysisJobProcessorFake();
        processor.failForBody = "{\"recordingId\":\"rec-1\",\"status\":\"ANALYSIS_REQUESTED\"}";
        SqsRecordingAnalysisJobPoller poller = new SqsRecordingAnalysisJobPoller(
                sqsClient.client(),
                QUEUE_URL,
                processor
        );

        int processedCount = poller.pollOnce();

        assertThat(processedCount).isEqualTo(1);
        assertThat(processor.processedBodies).containsExactly(
                "{\"recordingId\":\"rec-1\",\"status\":\"ANALYSIS_REQUESTED\"}",
                "{\"recordingId\":\"rec-2\",\"status\":\"ANALYSIS_REQUESTED\"}"
        );
        assertThat(sqsClient.deleteRequests)
                .extracting(DeleteMessageRequest::receiptHandle)
                .containsExactly("receipt-2");
    }

    private static Message message(String id, String receiptHandle, String body) {
        return Message.builder()
                .messageId(id)
                .receiptHandle(receiptHandle)
                .body(body)
                .build();
    }

    private static final class RecordingAnalysisJobProcessorFake implements RecordingAnalysisJobProcessor {

        private final List<String> processedBodies = new ArrayList<>();
        private String failForBody;

        @Override
        public Recording process(String messageBody) {
            processedBodies.add(messageBody);
            if (messageBody.equals(failForBody)) {
                throw new IllegalStateException("processing failed");
            }
            return null;
        }
    }

    private static final class SqsClientFake {

        private final List<Message> messages;
        private final List<DeleteMessageRequest> deleteRequests = new ArrayList<>();
        private ReceiveMessageRequest receiveRequest;

        private SqsClientFake(List<Message> messages) {
            this.messages = messages;
        }

        private SqsClient client() {
            return (SqsClient) Proxy.newProxyInstance(
                    SqsClient.class.getClassLoader(),
                    new Class<?>[]{SqsClient.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("receiveMessage")) {
                            receiveRequest = (ReceiveMessageRequest) args[0];
                            return ReceiveMessageResponse.builder()
                                    .messages(messages)
                                    .build();
                        }
                        if (method.getName().equals("deleteMessage")) {
                            deleteRequests.add((DeleteMessageRequest) args[0]);
                            return DeleteMessageResponse.builder().build();
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
