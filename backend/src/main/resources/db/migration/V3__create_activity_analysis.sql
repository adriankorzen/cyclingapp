-- Stored result of the FIT file analysis. Key metrics are columns so they can be queried and aggregated
-- (e.g. training load over time), the complete analysis is kept as JSON.
CREATE TABLE activity_analysis (
    activity_id           BIGINT PRIMARY KEY REFERENCES activity (id) ON DELETE CASCADE,
    analyzer_version      INTEGER                  NOT NULL,
    analyzed_at           TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Rider thresholds the analysis was calculated with
    ftp_watts             INTEGER,
    lthr_bpm              INTEGER,
    max_heart_rate_bpm    INTEGER,

    duration_seconds      INTEGER                  NOT NULL,
    average_power         DOUBLE PRECISION,
    normalized_power      DOUBLE PRECISION,
    variability_index     DOUBLE PRECISION,
    intensity_factor      DOUBLE PRECISION,
    training_stress_score DOUBLE PRECISION,
    work_kilojoules       DOUBLE PRECISION,
    average_heart_rate    DOUBLE PRECISION,
    max_heart_rate        DOUBLE PRECISION,
    interval_count        INTEGER                  NOT NULL,

    result                JSONB                    NOT NULL
);
