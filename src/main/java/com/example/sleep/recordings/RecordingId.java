package com.example.sleep.recordings;

public record RecordingId(String value) {

    public RecordingId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("recordingId must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
