package com.example.sleep.recordings.application;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingId;

import java.util.Optional;

public interface RecordingRepository {

    Recording save(Recording recording);

    Optional<Recording> findById(RecordingId id);

    boolean existsById(RecordingId id);
}
