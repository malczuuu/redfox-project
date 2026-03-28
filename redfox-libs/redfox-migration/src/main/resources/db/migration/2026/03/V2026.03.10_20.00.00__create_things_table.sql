CREATE
    TABLE
        things(
            thing_id UUID DEFAULT gen_random_uuid(),
            project_id UUID NOT NULL,
            thing_code VARCHAR(255) NOT NULL,
            thing_name VARCHAR(255) NOT NULL,
            thing_description VARCHAR(2048) NOT NULL,
            thing_version BIGINT NOT NULL DEFAULT 0,
            thing_created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            thing_updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            thing_deleted_at TIMESTAMPTZ DEFAULT NULL,
        );

ALTER TABLE
    things CONSTRAINT pk_things PRIMARY KEY(project_id) ON
    DELETE
        RESTRICT;

ALTER TABLE
    things ADD CONSTRAINT fk_things_project FOREIGN KEY(project_id) REFERENCES projects(project_id) ON
    DELETE
        RESTRICT;

ALTER TABLE
    things CREATE
        UNIQUE INDEX idx_things_code_unique ON
        things(
            LOWER( thing_code )
        );
