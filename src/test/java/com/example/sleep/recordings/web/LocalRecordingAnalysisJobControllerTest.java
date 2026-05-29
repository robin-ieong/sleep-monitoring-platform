package com.example.sleep.recordings.web;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.application.RecordingAnalysisJobProcessor;
import com.example.sleep.recordings.infrastructure.SqsRecordingAnalysisJobPoller;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LocalRecordingAnalysisJobControllerTest {

    private static final String QUEUE_URL = "http://localhost:4566/000000000000/sleep-recording-analysis";

    @Test
    void pollsRecordingAnalysisJobs() throws Exception {
        SqsClientFake sqsClient = new SqsClientFake(List.of(
                Message.builder()
                        .messageId("message-1")
                        .receiptHandle("receipt-1")
                        .body("{\"recordingId\":\"rec-123\",\"status\":\"ANALYSIS_REQUESTED\"}")
                        .build()
        ));
        RecordingAnalysisJobProcessorFake processor = new RecordingAnalysisJobProcessorFake();
        SqsRecordingAnalysisJobPoller poller = new SqsRecordingAnalysisJobPoller(
                sqsClient.client(),
                QUEUE_URL,
                processor
        );
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new LocalRecordingAnalysisJobController(poller))
                .build();

        mockMvc.perform(post("/dev/recording-analysis-jobs/poll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(1));

        assertThat(processor.processedBody)
                .isEqualTo("{\"recordingId\":\"rec-123\",\"status\":\"ANALYSIS_REQUESTED\"}");
        assertThat(sqsClient.deleteRequest.receiptHandle()).isEqualTo("receipt-1");
    }

    private static final class RecordingAnalysisJobProcessorFake implements RecordingAnalysisJobProcessor {

        private String processedBody;

        @Override
        public Recording process(String messageBody) {
            processedBody = messageBody;
            return null;
        }
    }

    private static final class SqsClientFake {

        private final List<Message> messages;
        private DeleteMessageRequest deleteRequest;

        private SqsClientFake(List<Message> messages) {
            this.messages = messages;
        }

        private SqsClient client() {
            return (SqsClient) Proxy.newProxyInstance(
                    SqsClient.class.getClassLoader(),
                    new Class<?>[]{SqsClient.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("receiveMessage")) {
                            return ReceiveMessageResponse.builder()
                                    .messages(messages)
                                    .build();
                        }
                        if (method.getName().equals("deleteMessage")) {
                            deleteRequest = (DeleteMessageRequest) args[0];
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
