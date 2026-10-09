CREATE TABLE claims (
    id            UUID PRIMARY KEY,
    owner_id      VARCHAR(64)    NOT NULL,
    owner_name    VARCHAR(128)   NOT NULL,
    description   VARCHAR(200)   NOT NULL,
    amount        NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    status        VARCHAR(16)    NOT NULL,
    submitted_at  TIMESTAMPTZ    NOT NULL,
    decided_by    VARCHAR(128),
    decided_at    TIMESTAMPTZ
);

CREATE INDEX idx_claims_owner ON claims (owner_id, submitted_at DESC);
CREATE INDEX idx_claims_status ON claims (status, submitted_at);
