CREATE TABLE recordings (
    id VARCHAR(120) PRIMARY KEY,
    owner_id VARCHAR(120) NOT NULL,
    original_filename VARCHAR(512) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    stored_at TIMESTAMP WITH TIME ZONE,
    storage_bucket VARCHAR(255),
    storage_key VARCHAR(1024),
    analysis_requested_at TIMESTAMP WITH TIME ZONE,
    analysis_completed_at TIMESTAMP WITH TIME ZONE
);
