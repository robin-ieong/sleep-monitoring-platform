package com.example.sleep.recordings;

import java.util.Optional;

public interface RecordingRepository {

    Recording save(Recording recording);

    Optional<Recording> findById(RecordingId id);

    boolean existsById(RecordingId id);
}
