package com.example.sleep.recordings.web;

import java.util.Map;

public record RecordingErrorResponse(String message, Map<String, String> fieldErrors) {

    public RecordingErrorResponse(String message) {
        this(message, Map.of());
    }
}
