package com.example.sleep.recordings;

import java.time.Instant;
import java.util.Optional;

public final class Recording {

    private final RecordingId id;
    private final String ownerId;
    private final String originalFilename;
    private final String contentType;
    private final RecordingStatus status;
    private final Instant registeredAt;
    private final Instant storedAt;
    private final StorageObjectReference storageObject;
    private final Instant analysisRequestedAt;
    private final Instant analysisCompletedAt;

    private Recording(
            RecordingId id,
            String ownerId,
            String originalFilename,
            String contentType,
            RecordingStatus status,
            Instant registeredAt,
            Instant storedAt,
            StorageObjectReference storageObject,
            Instant analysisRequestedAt,
            Instant analysisCompletedAt
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        requireNotBlank(ownerId, "ownerId");
        requireNotBlank(originalFilename, "originalFilename");
        requireNotBlank(contentType, "contentType");
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (registeredAt == null) {
            throw new IllegalArgumentException("registeredAt must not be null");
        }

        this.id = id;
        this.ownerId = ownerId;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.status = status;
        this.registeredAt = registeredAt;
        this.storedAt = storedAt;
        this.storageObject = storageObject;
        this.analysisRequestedAt = analysisRequestedAt;
        this.analysisCompletedAt = analysisCompletedAt;
    }

    public static Recording register(
            RecordingId id,
            String ownerId,
            String originalFilename,
            String contentType,
            Instant registeredAt
    ) {
        return new Recording(
                id,
                ownerId,
                originalFilename,
                contentType,
                RecordingStatus.AWAITING_UPLOAD,
                registeredAt,
                null,
                null,
                null,
                null
        );
    }

    public static Recording rehydrate(
            RecordingId id,
            String ownerId,
            String originalFilename,
            String contentType,
            RecordingStatus status,
            Instant registeredAt,
            Instant storedAt,
            StorageObjectReference storageObject,
            Instant analysisRequestedAt,
            Instant analysisCompletedAt
    ) {
        return new Recording(
                id,
                ownerId,
                originalFilename,
                contentType,
                status,
                registeredAt,
                storedAt,
                storageObject,
                analysisRequestedAt,
                analysisCompletedAt
        );
    }

    public Recording markStored(StorageObjectReference storageObject, Instant storedAt) {
        requireStatus(RecordingStatus.AWAITING_UPLOAD, "be awaiting upload before it can be marked stored");
        if (storageObject == null) {
            throw new IllegalArgumentException("storageObject must not be null");
        }
        if (storedAt == null) {
            throw new IllegalArgumentException("storedAt must not be null");
        }
        return new Recording(
                id,
                ownerId,
                originalFilename,
                contentType,
                RecordingStatus.STORED,
                registeredAt,
                storedAt,
                storageObject,
                analysisRequestedAt,
                analysisCompletedAt
        );
    }

    public Recording requestAnalysis(Instant analysisRequestedAt) {
        requireStatus(RecordingStatus.STORED, "be STORED before analysis can be requested");
        if (analysisRequestedAt == null) {
            throw new IllegalArgumentException("analysisRequestedAt must not be null");
        }
        return new Recording(
                id,
                ownerId,
                originalFilename,
                contentType,
                RecordingStatus.ANALYSIS_REQUESTED,
                registeredAt,
                storedAt,
                storageObject,
                analysisRequestedAt,
                analysisCompletedAt
        );
    }

    public Recording completeAnalysis(Instant analysisCompletedAt) {
        requireStatus(RecordingStatus.ANALYSIS_REQUESTED, "have analysis requested before analysis can be completed");
        if (analysisCompletedAt == null) {
            throw new IllegalArgumentException("analysisCompletedAt must not be null");
        }
        return new Recording(
                id,
                ownerId,
                originalFilename,
                contentType,
                RecordingStatus.ANALYSIS_COMPLETED,
                registeredAt,
                storedAt,
                storageObject,
                analysisRequestedAt,
                analysisCompletedAt
        );
    }

    public RecordingId id() {
        return id;
    }

    public String ownerId() {
        return ownerId;
    }

    public String originalFilename() {
        return originalFilename;
    }

    public String contentType() {
        return contentType;
    }

    public RecordingStatus status() {
        return status;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    public Optional<Instant> storedAt() {
        return Optional.ofNullable(storedAt);
    }

    public Optional<StorageObjectReference> storageObject() {
        return Optional.ofNullable(storageObject);
    }

    public Optional<Instant> analysisRequestedAt() {
        return Optional.ofNullable(analysisRequestedAt);
    }

    public Optional<Instant> analysisCompletedAt() {
        return Optional.ofNullable(analysisCompletedAt);
    }

    private void requireStatus(RecordingStatus expected, String requirement) {
        if (status != expected) {
            throw new IllegalStateException("Recording " + id + " must " + requirement);
        }
    }

    private static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
