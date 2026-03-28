CREATE
    TABLE
        projects(
            project_id UUID DEFAULT gen_random_uuid(),
            project_code VARCHAR(255) NOT NULL,
            project_name VARCHAR(255) NOT NULL,
            project_description VARCHAR(2048) NOT NULL,
            project_version BIGINT NOT NULL DEFAULT 0,
            project_created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            project_updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            project_deleted_at TIMESTAMPTZ DEFAULT NULL,
        );

ALTER TABLE
    projects CONSTRAINT pk_projects PRIMARY KEY(project_id) ON
    DELETE
        RESTRICT;

ALTER TABLE
    projects CREATE
        UNIQUE INDEX idx_projects_code_unique ON
        projects(
            LOWER( project_code )
        );
