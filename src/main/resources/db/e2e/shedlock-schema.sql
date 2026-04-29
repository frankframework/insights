CREATE TABLE IF NOT EXISTS shedlock (
    name        VARCHAR(64)              NOT NULL,
    lock_until  TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_by   VARCHAR(255)             NOT NULL,
    PRIMARY KEY (name)
);
