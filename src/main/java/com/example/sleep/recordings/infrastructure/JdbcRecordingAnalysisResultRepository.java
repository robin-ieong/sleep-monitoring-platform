package com.example.sleep.recordings.infrastructure;

import com.example.sleep.recordings.RecordingAnalysisResult;
import com.example.sleep.recordings.RecordingAnalysisResultStatus;
import com.example.sleep.recordings.RecordingId;
import com.example.sleep.recordings.application.RecordingAnalysisResultRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

public final class JdbcRecordingAnalysisResultRepository implements RecordingAnalysisResultRepository {

    private static final RowMapper<RecordingAnalysisResult> ROW_MAPPER = new RecordingAnalysisResultRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public JdbcRecordingAnalysisResultRepository(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            throw new IllegalArgumentException("jdbcTemplate must not be null");
        }
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RecordingAnalysisResult save(RecordingAnalysisResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }

        int updatedRows = jdbcTemplate.update("""
                        UPDATE recording_analysis_results
                        SET status = ?,
                            completed_at = ?,
                            summary = ?
                        WHERE recording_id = ?
                        """,
                result.status().name(),
                toOffsetDateTime(result.completedAt()),
                result.summary(),
                result.recordingId().value()
        );

        if (updatedRows == 0) {
            jdbcTemplate.update("""
                            INSERT INTO recording_analysis_results (
                                recording_id,
                                status,
                                completed_at,
                                summary
                            )
                            VALUES (?, ?, ?, ?)
                            """,
                    result.recordingId().value(),
                    result.status().name(),
                    toOffsetDateTime(result.completedAt()),
                    result.summary()
            );
        }

        return result;
    }

    @Override
    public Optional<RecordingAnalysisResult> findByRecordingId(RecordingId recordingId) {
        if (recordingId == null) {
            throw new IllegalArgumentException("recordingId must not be null");
        }

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM recording_analysis_results WHERE recording_id = ?",
                    ROW_MAPPER,
                    recordingId.value()
            ));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
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

    private static final class RecordingAnalysisResultRowMapper implements RowMapper<RecordingAnalysisResult> {

        @Override
        public RecordingAnalysisResult mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return RecordingAnalysisResult.rehydrate(
                    new RecordingId(resultSet.getString("recording_id")),
                    RecordingAnalysisResultStatus.valueOf(resultSet.getString("status")),
                    instant(resultSet, "completed_at"),
                    resultSet.getString("summary")
            );
        }
    }
}
