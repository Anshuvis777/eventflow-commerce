-- V5: Create similar_incidents table
CREATE TABLE similar_incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    similar_incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    similarity_score REAL NOT NULL CHECK (similarity_score >= 0 AND similarity_score <= 1),
    matched_on VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_similar_incident_id ON similar_incidents(incident_id);
CREATE INDEX idx_similar_similar_incident_id ON similar_incidents(similar_incident_id);
CREATE INDEX idx_similar_score ON similar_incidents(similarity_score DESC);
