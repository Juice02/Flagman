-- Unique constraints ignore NULLs, so a NULL flag_key or environment could slip past them.
-- Every flag needs a key and every config needs an environment.
ALTER TABLE feature_flags MODIFY flag_key VARCHAR(100) NOT NULL;

ALTER TABLE feature_flag_config MODIFY environment VARCHAR(10) NOT NULL;
