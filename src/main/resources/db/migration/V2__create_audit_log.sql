-- Every change to a claim, and every refused attempt to approve one's own claim, is recorded here.
--
-- Rows form a hash chain: each row's hash covers its own fields and the previous row's hash, and
-- seq has no gaps. Editing, deleting or inserting a row anywhere in the middle breaks the chain,
-- which GET /audit/verify detects.
CREATE TABLE claim_audit_log (
    seq          BIGINT         PRIMARY KEY,
    claim_id     UUID           NOT NULL,
    action       VARCHAR(32)    NOT NULL,
    actor_id     VARCHAR(64)    NOT NULL,
    actor_name   VARCHAR(128)   NOT NULL,
    amount       NUMERIC(10, 2) NOT NULL,
    occurred_at  TIMESTAMPTZ    NOT NULL,
    prev_hash    VARCHAR(64)    NOT NULL,
    hash         VARCHAR(64)    NOT NULL UNIQUE
);

CREATE INDEX idx_audit_claim ON claim_audit_log (claim_id, seq);

-- The application only ever inserts. The trigger makes that a database rule, not just a convention.
CREATE FUNCTION reject_audit_log_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'claim_audit_log is append-only (% blocked)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER claim_audit_log_no_update_delete
    BEFORE UPDATE OR DELETE ON claim_audit_log
    FOR EACH ROW EXECUTE FUNCTION reject_audit_log_change();

CREATE TRIGGER claim_audit_log_no_truncate
    BEFORE TRUNCATE ON claim_audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION reject_audit_log_change();
