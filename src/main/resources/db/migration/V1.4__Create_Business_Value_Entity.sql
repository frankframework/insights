CREATE TABLE business_value (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

ALTER TABLE issue DROP COLUMN IF EXISTS business_value;

ALTER TABLE issue ADD COLUMN business_value_id UUID;
ALTER TABLE issue ADD CONSTRAINT fk_issue_business_value
    FOREIGN KEY (business_value_id) REFERENCES business_value(id);
