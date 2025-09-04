CREATE TABLE branch (
	id VARCHAR(255) PRIMARY KEY,
	name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE label (
	id VARCHAR(255) PRIMARY KEY,
	name VARCHAR(255) NOT NULL UNIQUE,
	description VARCHAR(255),
	color VARCHAR(255) NOT NULL
);

CREATE TABLE issue_type (
	id VARCHAR(255) PRIMARY KEY,
	name VARCHAR(255) NOT NULL UNIQUE,
	description VARCHAR(255),
	color VARCHAR(255) NOT NULL
);

CREATE TABLE issue_priority (
	id VARCHAR(255) PRIMARY KEY,
	name VARCHAR(255) NOT NULL UNIQUE,
	description VARCHAR(255),
	color VARCHAR(255) NOT NULL
);

CREATE TABLE milestone (
	id VARCHAR(255) PRIMARY KEY,
	number INT NOT NULL UNIQUE,
	title VARCHAR(255) NOT NULL UNIQUE,
	url VARCHAR(255),
	state INT NOT NULL,
	due_on TIMESTAMPTZ,
	open_issue_count INT NOT NULL,
	closed_issue_count INT NOT NULL
);

CREATE TABLE release (
	id VARCHAR(255) PRIMARY KEY,
	tag_name VARCHAR(255) NOT NULL UNIQUE,
	name VARCHAR(255) NOT NULL UNIQUE,
	published_at TIMESTAMPTZ,
	branch_id VARCHAR(255) REFERENCES branch(id)
);

CREATE TABLE pull_request (
	id VARCHAR(255) PRIMARY KEY,
	number INT NOT NULL,
	title VARCHAR(255) NOT NULL,
	url VARCHAR(255) NOT NULL,
	merged_at TIMESTAMPTZ,
	milestone_id VARCHAR(255) REFERENCES milestone(id)
);

CREATE TABLE issue (
	id VARCHAR(255) PRIMARY KEY,
	number INT NOT NULL,
	title VARCHAR(255) NOT NULL,
	state INT NOT NULL,
	url VARCHAR(255) NOT NULL,
	closed_at TIMESTAMPTZ,
	business_value TEXT,
	points DOUBLE PRECISION,
	milestone_id VARCHAR(255) REFERENCES milestone(id),
	issue_type_id VARCHAR(255) REFERENCES issue_type(id),
	issue_priority_id VARCHAR(255) REFERENCES issue_priority(id)
);


CREATE TABLE issue_sub_issues (
	issue_id VARCHAR(255) NOT NULL REFERENCES issue(id),
	sub_issues_id VARCHAR(255) NOT NULL REFERENCES issue(id),
	PRIMARY KEY (issue_id, sub_issues_id)
);

CREATE TABLE branch_pull_request (
	branch_id VARCHAR(255) NOT NULL REFERENCES branch(id),
	pull_request_id VARCHAR(255) NOT NULL REFERENCES pull_request(id),
	PRIMARY KEY (branch_id, pull_request_id)
);

CREATE TABLE issue_label (
	issue_id VARCHAR(255) NOT NULL REFERENCES issue(id),
	label_id VARCHAR(255) NOT NULL REFERENCES label(id),
	PRIMARY KEY (issue_id, label_id)
);

CREATE TABLE pull_request_issue (
	pull_request_id VARCHAR(255) NOT NULL REFERENCES pull_request(id),
	issue_id VARCHAR(255) NOT NULL REFERENCES issue(id),
	PRIMARY KEY (pull_request_id, issue_id)
);

CREATE TABLE pull_request_label (
	pull_request_id VARCHAR(255) NOT NULL REFERENCES pull_request(id),
	label_id VARCHAR(255) NOT NULL REFERENCES label(id),
	PRIMARY KEY (pull_request_id, label_id)
);

CREATE TABLE release_pull_request (
	release_id VARCHAR(255) NOT NULL REFERENCES release(id),
	pull_request_id VARCHAR(255) NOT NULL REFERENCES pull_request(id),
	PRIMARY KEY (release_id, pull_request_id)
);

CREATE TABLE shedlock (
	name VARCHAR(64) PRIMARY KEY,
	lock_until TIMESTAMPTZ NOT NULL,
	locked_at TIMESTAMPTZ NOT NULL,
	locked_by VARCHAR(255) NOT NULL
);
