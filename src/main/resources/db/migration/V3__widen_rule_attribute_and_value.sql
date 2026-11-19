-- varchar(20) was too short for an IN list (e.g. "IN,US,GB,DE,FR,ES,IT") or a longer attribute name.
ALTER TABLE rules MODIFY attribute VARCHAR(100);

ALTER TABLE rules MODIFY value VARCHAR(255);
