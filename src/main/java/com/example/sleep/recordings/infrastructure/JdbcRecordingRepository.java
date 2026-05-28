package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.Recording;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.RecordingStatus;
import com.example.sleep.recordings.StorageObjectReference;
import com.example.sleep.recordings.application.RecordingRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

public final class JdbcRecordingRepository implements RecordingRepository {

    private static final RowMapper<Recording> ROW_MAPPER = new RecordingRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public JdbcRecordingRepository(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            throw new IllegalArgumentException("jdbcTemplate must not be null");
        }
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Recording save(Recording recording) {
        if (recording == null) {
            throw new IllegalArgumentException("recording must not be null");
        }

        int updatedRows = jdbcTemplate.update("""
                        UPDATE recordings
                        SET owner_id = ?,
                            original_filename = ?,
                            content_type = ?,
                            status = ?,
                            registered_at = ?,
                            stored_at = ?,
                            storage_bucket = ?,
                            storage_key = ?,
                            analysis_requested_at = ?,
                            analysis_completed_at = ?
                        WHERE id = ?
                        """,
                recording.ownerId(),
                recording.originalFilename(),
                recording.contentType(),
                recording.status().name(),
                toOffsetDateTime(recording.registeredAt()),
                recording.storedAt().map(JdbcRecordingRepository::toOffsetDateTime).orElse(null),
                recording.storageObject().map(StorageObjectReference::bucketName).orElse(null),
                recording.storageObject().map(StorageObjectReference::objectKey).orElse(null),
                recording.analysisRequestedAt().map(JdbcRecordingRepository::toOffsetDateTime).orElse(null),
                recording.analysisCompletedAt().map(JdbcRecordingRepository::toOffsetDateTime).orElse(null),
                recording.id().value()
        );

        if (updatedRows == 0) {
            jdbcTemplate.update("""
                            INSERT INTO recordings (
                                id,
                                owner_id,
                                original_filename,
                                content_type,
                                status,
                                registered_at,
                                stored_at,
                                storage_bucket,
                                storage_key,
                                analysis_requested_at,
                                analysis_completed_at
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    recording.id().value(),
                    recording.ownerId(),
                    recording.originalFilename(),
                    recording.contentType(),
                    recording.status().name(),
                    toOffsetDateTime(recording.registeredAt()),
                    recording.storedAt().map(JdbcRecordingRepository::toOffsetDateTime).orElse(null),
                    recording.storageObject().map(StorageObjectReference::bucketName).orElse(null),
                    recording.storageObject().map(StorageObjectReference::objectKey).orElse(null),
                    recording.analysisRequestedAt().map(JdbcRecordingRepository::toOffsetDateTime).orElse(null),
                    recording.analysisCompletedAt().map(JdbcRecordingRepository::toOffsetDateTime).orElse(null)
            );
        }

        return recording;
    }

    @Override
    public Optional<Recording> findById(RecordingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM recordings WHERE id = ?",
                    ROW_MAPPER,
                    id.value()
            ));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(RecordingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recordings WHERE id = ?",
                Integer.class,
                id.value()
        );
        return count != null && count > 0;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet resultSet, String columnName) throws SQLException {
        OffsetDateTime value = resultSet.getObject(columnName, OffsetDateTime.class);
        if (value == null) {
            return null;
        }
        return value.toInstant();
    }

    private static final class RecordingRowMapper implements RowMapper<Recording> {

        @Override
        public Recording mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            String storageBucket = resultSet.getString("storage_bucket");
            String storageKey = resultSet.getString("storage_key");
            StorageObjectReference storageObject = null;
            if (storageBucket != null && storageKey != null) {
                storageObject = new StorageObjectReference(storageBucket, storageKey);
            }

            return Recording.rehydrate(
                    new RecordingId(resultSet.getString("id")),
                    resultSet.getString("owner_id"),
                    resultSet.getString("original_filename"),
                    resultSet.getString("content_type"),
                    RecordingStatus.valueOf(resultSet.getString("status")),
                    instant(resultSet, "registered_at"),
                    instant(resultSet, "stored_at"),
                    storageObject,
                    instant(resultSet, "analysis_requested_at"),
                    instant(resultSet, "analysis_completed_at")
            );
        }
    }
}
