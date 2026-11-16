-- Baseline: the schema as it was before Flyway was introduced (from database/schema).

CREATE TABLE projects (
    project_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE feature_flags (
    flag_id INT PRIMARY KEY AUTO_INCREMENT,
    project_id INT,
    flag_key VARCHAR(100),
    friendly_name VARCHAR(255),
    description TEXT,
    created_on TIMESTAMP,
    updated_on TIMESTAMP
);

CREATE TABLE feature_flag_config (
    config_id INT PRIMARY KEY AUTO_INCREMENT,
    flag_id INT,
    environment varchar(10),
    enabled BOOLEAN,
    rollout_percentage INT,
    created_on TIMESTAMP,
    updated_on TIMESTAMP
);

CREATE TABLE rules (
    rule_id INT PRIMARY KEY AUTO_INCREMENT,
    config_id INT,
    attribute varchar(20),
    operator varchar(10),
    value varchar(20)
);

ALTER TABLE feature_flags ADD CONSTRAINT fk_projectId FOREIGN KEY (project_id) REFERENCES projects(project_id);

ALTER TABLE feature_flag_config ADD CONSTRAINT fk_flagId FOREIGN KEY (flag_id) REFERENCES feature_flags(flag_id);

ALTER TABLE rules ADD CONSTRAINT fk_configId FOREIGN KEY (config_id) REFERENCES feature_flag_config(config_id);

-- A flag key is unique within its project (different projects can reuse a key).
ALTER TABLE feature_flags ADD CONSTRAINT uq_project_flag_key UNIQUE (project_id, flag_key);

-- A flag has at most one config per environment.
ALTER TABLE feature_flag_config ADD CONSTRAINT uq_flag_environment UNIQUE (flag_id, environment);

-- Only the fixed environments are allowed (matches the Java Environment enum).
ALTER TABLE feature_flag_config ADD CONSTRAINT chk_environment CHECK (environment IN ('DEV', 'STAGING', 'PREPROD', 'PROD'));
