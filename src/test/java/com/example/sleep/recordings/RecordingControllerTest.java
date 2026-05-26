package com.example.sleep.recordings;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecordingControllerTest {

    private static final Instant NOW = Instant.parse("2026-05-26T11:20:00Z");

    private final InMemoryRecordingRepository repository = new InMemoryRecordingRepository();
    private final RegisterRecordingService service = new RegisterRecordingService(
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new RecordingController(service))
            .setControllerAdvice(new RecordingExceptionHandler())
            .build();

    @Test
    void registersRecordingMetadata() throws Exception {
        mockMvc.perform(post("/recordings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "rec-123",
                                  "ownerId": "user-456",
                                  "originalFilename": "night-audio.m4a",
                                  "contentType": "audio/mp4"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/recordings/rec-123")))
                .andExpect(jsonPath("$.id").value("rec-123"))
                .andExpect(jsonPath("$.ownerId").value("user-456"))
                .andExpect(jsonPath("$.originalFilename").value("night-audio.m4a"))
                .andExpect(jsonPath("$.contentType").value("audio/mp4"))
                .andExpect(jsonPath("$.status").value("AWAITING_UPLOAD"))
                .andExpect(jsonPath("$.registeredAt").value("2026-05-26T11:20:00Z"));
    }

    @Test
    void returnsConflictWhenRecordingIdAlreadyExists() throws Exception {
        String body = """
                {
                  "id": "rec-123",
                  "ownerId": "user-456",
                  "originalFilename": "night-audio.m4a",
                  "contentType": "audio/mp4"
                }
                """;

        mockMvc.perform(post("/recordings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/recordings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Recording rec-123 already exists"));
    }

    @Test
    void returnsBadRequestForInvalidMetadata() throws Exception {
        mockMvc.perform(post("/recordings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "rec-123",
                                  "ownerId": " ",
                                  "originalFilename": "night-audio.m4a",
                                  "contentType": "audio/mp4"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ownerId must not be blank"));
    }
}
