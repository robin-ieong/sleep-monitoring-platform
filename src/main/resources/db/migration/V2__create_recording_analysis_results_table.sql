CREATE TABLE recording_analysis_results (
    recording_id VARCHAR(120) PRIMARY KEY,
    status VARCHAR(60) NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_recording_analysis_results_recording
        FOREIGN KEY (recording_id)
        REFERENCES recordings (id)
);
