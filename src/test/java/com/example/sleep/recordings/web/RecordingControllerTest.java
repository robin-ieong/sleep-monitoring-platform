package com.example.sleep.recordings.web;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.StorageObjectReference;
import com.example.sleep.recordings.application.CompleteRecordingUploadService;
import com.example.sleep.recordings.application.CreateRecordingUploadService;
import com.example.sleep.recordings.application.MarkRecordingStoredService;
import com.example.sleep.recordings.application.PresignedRecordingUpload;
import com.example.sleep.recordings.application.PresignedRecordingUploadPort;
import com.example.sleep.recordings.application.RecordingObjectVerifier;
import com.example.sleep.recordings.application.RegisterRecordingService;
import com.example.sleep.recordings.infrastructure.InMemoryRecordingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.endsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecordingControllerTest {

    private static final Instant NOW = Instant.parse("2026-05-26T11:20:00Z");
    private static final Instant UPLOAD_EXPIRES_AT = Instant.parse("2026-05-26T11:35:00Z");
    private static final PresignedRecordingUpload UPLOAD = new PresignedRecordingUpload(
            URI.create("https://example.com/upload/rec-123"),
            "PUT",
            new StorageObjectReference("sleep-recordings", "recordings/user-456/rec-123/audio.m4a"),
            UPLOAD_EXPIRES_AT
    );

    private final InMemoryRecordingRepository repository = new InMemoryRecordingRepository();
    private final RegisterRecordingService service = new RegisterRecordingService(
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );
    private final MarkRecordingStoredService markStoredService = new MarkRecordingStoredService(
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );
    private final RecordingObjectVerifierFake verifier = new RecordingObjectVerifierFake(true);
    private final CompleteRecordingUploadService completeUploadService = new CompleteRecordingUploadService(
            repository,
            verifier,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );
    private final PresignedRecordingUploadFake uploadPort = new PresignedRecordingUploadFake(UPLOAD);
    private final CreateRecordingUploadService createUploadService = new CreateRecordingUploadService(
            repository,
            uploadPort,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new RecordingController(
                    service,
                    markStoredService,
                    createUploadService,
                    completeUploadService,
                    repository
            ))
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
                                  "originalFilename": "",
                                  "contentType": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.ownerId").value("ownerId must not be blank"))
                .andExpect(jsonPath("$.fieldErrors.originalFilename").value("originalFilename must not be blank"))
                .andExpect(jsonPath("$.fieldErrors.contentType").value("contentType must not be blank"));
    }

    @Test
    void createsRecordingUploadInstructions() throws Exception {
        mockMvc.perform(post("/recording-uploads")
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
                .andExpect(jsonPath("$.recording.id").value("rec-123"))
                .andExpect(jsonPath("$.recording.ownerId").value("user-456"))
                .andExpect(jsonPath("$.recording.status").value("AWAITING_UPLOAD"))
                .andExpect(jsonPath("$.upload.uploadUrl").value("https://example.com/upload/rec-123"))
                .andExpect(jsonPath("$.upload.method").value("PUT"))
                .andExpect(jsonPath("$.upload.storageObject.bucketName").value("sleep-recordings"))
                .andExpect(jsonPath("$.upload.storageObject.objectKey").value("recordings/user-456/rec-123/audio.m4a"))
                .andExpect(jsonPath("$.upload.expiresAt").value("2026-05-26T11:35:00Z"));

        assertThat(repository.findById(new RecordingId("rec-123")))
                .hasValueSatisfying(recording -> assertThat(recording.status().name()).isEqualTo("AWAITING_UPLOAD"));
        assertThat(uploadPort.recording.id()).isEqualTo(new RecordingId("rec-123"));
    }

    @Test
    void getsRegisteredRecordingMetadata() throws Exception {
        repository.save(Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4",
                NOW
        ));

        mockMvc.perform(get("/recordings/rec-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("rec-123"))
                .andExpect(jsonPath("$.ownerId").value("user-456"))
                .andExpect(jsonPath("$.originalFilename").value("night-audio.m4a"))
                .andExpect(jsonPath("$.contentType").value("audio/mp4"))
                .andExpect(jsonPath("$.status").value("AWAITING_UPLOAD"))
                .andExpect(jsonPath("$.registeredAt").value("2026-05-26T11:20:00Z"));
    }

    @Test
    void returnsNotFoundWhenRecordingDoesNotExist() throws Exception {
        mockMvc.perform(get("/recordings/missing-recording"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recording missing-recording was not found"));
    }

    @Test
    void marksRecordingAsStored() throws Exception {
        repository.save(Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4",
                NOW
        ));

        mockMvc.perform(patch("/recordings/rec-123/storage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bucketName": "sleep-recordings",
                                  "objectKey": "recordings/rec-123/audio.m4a"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("rec-123"))
                .andExpect(jsonPath("$.status").value("STORED"))
                .andExpect(jsonPath("$.storedAt").value("2026-05-26T11:20:00Z"))
                .andExpect(jsonPath("$.storageObject.bucketName").value("sleep-recordings"))
                .andExpect(jsonPath("$.storageObject.objectKey").value("recordings/rec-123/audio.m4a"));

        assertThat(repository.findById(new RecordingId("rec-123")))
                .hasValueSatisfying(recording -> {
                    assertThat(recording.storageObject()).contains(new StorageObjectReference(
                            "sleep-recordings",
                            "recordings/rec-123/audio.m4a"
                    ));
                    assertThat(recording.storedAt()).contains(NOW);
                });
    }

    @Test
    void completesUploadWhenObjectExists() throws Exception {
        repository.save(Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4",
                NOW
        ));

        mockMvc.perform(post("/recordings/rec-123/upload-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bucketName": "sleep-recordings",
                                  "objectKey": "recordings/user-456/rec-123/audio.m4a"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("rec-123"))
                .andExpect(jsonPath("$.status").value("STORED"))
                .andExpect(jsonPath("$.storedAt").value("2026-05-26T11:20:00Z"))
                .andExpect(jsonPath("$.storageObject.bucketName").value("sleep-recordings"))
                .andExpect(jsonPath("$.storageObject.objectKey").value("recordings/user-456/rec-123/audio.m4a"));

        assertThat(verifier.storageObject).isEqualTo(new StorageObjectReference(
                "sleep-recordings",
                "recordings/user-456/rec-123/audio.m4a"
        ));
    }

    @Test
    void returnsConflictWhenCompletingUploadForMissingObject() throws Exception {
        repository.save(Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4",
                NOW
        ));
        verifier.exists = false;

        mockMvc.perform(post("/recordings/rec-123/upload-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bucketName": "sleep-recordings",
                                  "objectKey": "recordings/user-456/rec-123/audio.m4a"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Recording object recordings/user-456/rec-123/audio.m4a does not exist in bucket sleep-recordings"
                ));
    }

    @Test
    void returnsBadRequestForInvalidStorageReference() throws Exception {
        repository.save(Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4",
                NOW
        ));

        mockMvc.perform(patch("/recordings/rec-123/storage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bucketName": " ",
                                  "objectKey": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.bucketName").value("bucketName must not be blank"))
                .andExpect(jsonPath("$.fieldErrors.objectKey").value("objectKey must not be blank"));
    }

    private static final class PresignedRecordingUploadFake implements PresignedRecordingUploadPort {

        private final PresignedRecordingUpload upload;
        private Recording recording;

        private PresignedRecordingUploadFake(PresignedRecordingUpload upload) {
            this.upload = upload;
        }

        @Override
        public PresignedRecordingUpload createUploadFor(Recording recording) {
            this.recording = recording;
            return upload;
        }
    }

    private static final class RecordingObjectVerifierFake implements RecordingObjectVerifier {

        private boolean exists;
        private StorageObjectReference storageObject;

        private RecordingObjectVerifierFake(boolean exists) {
            this.exists = exists;
        }

        @Override
        public boolean exists(StorageObjectReference storageObject) {
            this.storageObject = storageObject;
            return exists;
        }
    }
}
