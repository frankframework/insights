CREATE TABLE vulnerability (
	cve_id VARCHAR(128) PRIMARY KEY,
	severity VARCHAR(255) NOT NULL,
	cvss_score DOUBLE PRECISION,
	description TEXT
);

CREATE TABLE vulnerability_cwes (
	vulnerability_cve_id VARCHAR(128) NOT NULL REFERENCES vulnerability(cve_id),
	cwes VARCHAR(255)
);

CREATE TABLE release_vulnerability (
	release_id VARCHAR(255) NOT NULL REFERENCES release(id),
	vulnerability_cve_id VARCHAR(128) NOT NULL REFERENCES vulnerability(cve_id),
	PRIMARY KEY (release_id, vulnerability_cve_id)
);
