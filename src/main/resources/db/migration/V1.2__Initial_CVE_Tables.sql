CREATE TABLE dependency (
	id UUID PRIMARY KEY,
	group_id VARCHAR(255) NOT NULL,
	artifact_id VARCHAR(255) NOT NULL,
	version VARCHAR(255) NOT NULL,
	file_name VARCHAR(255),
	CONSTRAINT uq_dependency_gav UNIQUE (group_id, artifact_id, version)
);

CREATE TABLE vulnerability (
	cve_id VARCHAR(32) PRIMARY KEY,
	severity VARCHAR(255) NOT NULL,
	cvss_score DOUBLE PRECISION,
	description VARCHAR(2048)
);

CREATE TABLE vulnerability_cwes (
	vulnerability_cve_id VARCHAR(32) NOT NULL REFERENCES vulnerability(cve_id),
	cwes VARCHAR(255)
);

CREATE TABLE release_dependency (
	release_id VARCHAR(255) NOT NULL REFERENCES release(id),
	dependency_id UUID  NOT NULL REFERENCES dependency(id),
	PRIMARY KEY (release_id, dependency_id)
);

CREATE TABLE release_vulnerability (
	release_id VARCHAR(255) NOT NULL REFERENCES release(id),
	vulnerability_cve_id VARCHAR(32) NOT NULL REFERENCES vulnerability(cve_id),
	PRIMARY KEY (release_id, vulnerability_cve_id)
);
