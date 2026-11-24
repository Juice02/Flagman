-- One row per day (server time, UTC on Render): how many sandboxes were created, i.e. how many new
-- browsers used the demo. Kept separately because sandboxes themselves are deleted after a day idle.
-- No IPs or other personal data.
CREATE TABLE demo_visits (
    day DATE PRIMARY KEY,
    visitors INT NOT NULL
);
