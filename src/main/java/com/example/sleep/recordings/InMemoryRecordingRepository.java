package com.example.sleep.recordings;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryRecordingRepository implements RecordingRepository {

    private final Map<RecordingId, Recording> recordings = new ConcurrentHashMap<>();

    @Override
    public Recording save(Recording recording) {
        if (recording == null) {
            throw new IllegalArgumentException("recording must not be null");
        }
        recordings.put(recording.id(), recording);
        return recording;
    }

    @Override
    public Optional<Recording> findById(RecordingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return Optional.ofNullable(recordings.get(id));
    }

    @Override
    public boolean existsById(RecordingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return recordings.containsKey(id);
    }
}
