-- A sandbox is a project created for one demo visitor. Only a SHA-256 hash of its token is stored,
-- so a database leak does not reveal tokens. Sandboxes unused for a day are deleted.
CREATE TABLE sandboxes (
    project_id INT PRIMARY KEY,
    token_hash CHAR(64) NOT NULL,
    created_on TIMESTAMP NOT NULL,
    last_used_on TIMESTAMP NOT NULL,
    CONSTRAINT fk_sandbox_project FOREIGN KEY (project_id) REFERENCES projects(project_id),
    CONSTRAINT uq_sandbox_token UNIQUE (token_hash)
);

CREATE INDEX ix_sandboxes_last_used ON sandboxes (last_used_on);
