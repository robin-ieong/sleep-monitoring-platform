package com.example.sleep.recordings;

public final class RecordingNotFoundException extends RuntimeException {

    public RecordingNotFoundException(RecordingId id) {
        super("Recording " + id + " was not found");
    }
}
