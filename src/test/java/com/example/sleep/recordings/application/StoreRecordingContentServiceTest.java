package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.RecordingNotFoundException;
import com.example.sleep.recordings.RecordingStatus;
import com.example.sleep.recordings.StorageObjectReference;
import com.example.sleep.recordings.infrastructure.InMemoryRecordingRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreRecordingContentServiceTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-05-27T10:10:00Z");
    private static final Instant STORED_AT = Instant.parse("2026-05-27T10:12:00Z");
    private static final byte[] CONTENT = "fake audio bytes".getBytes();
    private static final StorageObjectReference STORAGE_OBJECT =
            new StorageObjectReference("sleep-recordings", "recordings/rec-123/audio.m4a");

    private final InMemoryRecordingRepository repository = new InMemoryRecordingRepository();
    private final RecordingStorage storage = new RecordingStorageFake(STORAGE_OBJECT);
    private final StoreRecordingContentService service = new StoreRecordingContentService(
            repository,
            storage,
            Clock.fixed(STORED_AT, ZoneOffset.UTC)
    );

    @Test
    void storesContentForExistingRecordingAndMarksItStored() {
        Recording registered = registeredRecording();
        repository.save(registered);

        Recording stored = service.store(new StoreRecordingContentCommand(
                new RecordingId("rec-123"),
                CONTENT
        ));

        assertThat(stored.status()).isEqualTo(RecordingStatus.STORED);
        assertThat(stored.storageObject()).contains(STORAGE_OBJECT);
        assertThat(stored.storedAt()).contains(STORED_AT);
        assertThat(repository.findById(new RecordingId("rec-123"))).contains(stored);

        RecordingStorageFake fake = (RecordingStorageFake) storage;
        assertThat(fake.storedRecording).isSameAs(registered);
        assertThat(fake.storedContent).containsExactly(CONTENT);
    }

    @Test
    void rejectsMissingRecordingWithoutCallingStorage() {
        assertThatThrownBy(() -> service.store(new StoreRecordingContentCommand(
                new RecordingId("missing-recording"),
                CONTENT
        )))
                .isInstanceOf(RecordingNotFoundException.class)
                .hasMessage("Recording missing-recording was not found");

        assertThat(((RecordingStorageFake) storage).wasCalled()).isFalse();
    }

    @Test
    void rejectsAlreadyStoredRecordingWithoutCallingStorage() {
        repository.save(registeredRecording().markStored(STORAGE_OBJECT, STORED_AT));

        assertThatThrownBy(() -> service.store(new StoreRecordingContentCommand(
                new RecordingId("rec-123"),
                CONTENT
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recording rec-123 must be awaiting upload before it can be stored");

        assertThat(((RecordingStorageFake) storage).wasCalled()).isFalse();
    }

    @Test
    void commandDefensivelyCopiesContentBytes() {
        byte[] content = CONTENT.clone();

        StoreRecordingContentCommand command = new StoreRecordingContentCommand(
                new RecordingId("rec-123"),
                content
        );
        content[0] = 'X';

        assertThat(command.content()).containsExactly(CONTENT);
        assertThat(command.content()).isNotSameAs(command.content());
    }

    @Test
    void commandRejectsEmptyContent() {
        assertThatThrownBy(() -> new StoreRecordingContentCommand(
                new RecordingId("rec-123"),
                new byte[0]
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("content must not be empty");
    }

    private static Recording registeredRecording() {
        return Recording.register(
                new RecordingId("rec-123"),
                "user-456",
                "night-audio.m4a",
                "audio/mp4",
                REGISTERED_AT
        );
    }

    private static final class RecordingStorageFake implements RecordingStorage {

        private final StorageObjectReference storageObject;
        private Recording storedRecording;
        private byte[] storedContent;

        private RecordingStorageFake(StorageObjectReference storageObject) {
            this.storageObject = storageObject;
        }

        @Override
        public StorageObjectReference store(Recording recording, byte[] content) {
            storedRecording = recording;
            storedContent = Arrays.copyOf(content, content.length);
            return storageObject;
        }

        private boolean wasCalled() {
            return storedRecording != null;
        }
    }
}
