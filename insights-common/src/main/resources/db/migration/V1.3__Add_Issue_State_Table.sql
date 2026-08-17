CREATE TABLE issue_state (
	id VARCHAR(255) PRIMARY KEY,
	name VARCHAR(255) NOT NULL UNIQUE,
	description VARCHAR(255),
	color VARCHAR(255) NOT NULL
);

ALTER TABLE issue ADD COLUMN issue_state_id VARCHAR(255);

ALTER TABLE issue ADD CONSTRAINT fk_issue_state FOREIGN KEY (issue_state_id) REFERENCES issue_state(id);
