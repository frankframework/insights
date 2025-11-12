CREATE TABLE "user" (
    id UUID PRIMARY KEY,
    github_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    is_frank_framework_member BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_user_github_id ON "user"(github_id);
CREATE INDEX idx_user_username ON "user"(username);
