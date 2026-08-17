-- Remove business value connections from all issues
UPDATE issue SET business_value_id = NULL WHERE business_value_id IS NOT NULL;

-- Delete all existing business values
DELETE FROM business_value;

-- Drop the existing unique constraint on title
ALTER TABLE business_value DROP CONSTRAINT IF EXISTS business_value_name_key;
ALTER TABLE business_value DROP CONSTRAINT IF EXISTS business_value_title_key;

-- Add release_id column
ALTER TABLE business_value ADD COLUMN release_id VARCHAR(255) NOT NULL;

-- Add foreign key constraint to release
ALTER TABLE business_value ADD CONSTRAINT fk_business_value_release
    FOREIGN KEY (release_id) REFERENCES release(id);

-- Add composite unique constraint on (title, release_id)
ALTER TABLE business_value ADD CONSTRAINT uq_business_value_title_release
    UNIQUE (title, release_id);
