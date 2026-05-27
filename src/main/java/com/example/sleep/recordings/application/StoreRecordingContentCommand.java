package com.example.sleep.recordings.application;

import com.example.sleep.recordings.RecordingId;

import java.util.Arrays;

public record StoreRecordingContentCommand(
        RecordingId id,
        byte[] content
) {

    public StoreRecordingContentCommand {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("content must not be empty");
        }
        content = Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
