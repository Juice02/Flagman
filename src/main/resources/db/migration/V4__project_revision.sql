-- Counts changes to a project's flags, configs and rules. Snapshots carry it so a client can
-- ignore an older snapshot that arrives after a newer one.
ALTER TABLE projects ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
