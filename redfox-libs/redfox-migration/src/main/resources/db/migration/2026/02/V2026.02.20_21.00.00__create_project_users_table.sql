CREATE
    TABLE
        project_users(
            project_user_id UUID DEFAULT gen_random_uuid(),
            project_id UUID NOT NULL,
            user_id UUID NOT NULL
        );

ALTER TABLE
    project_users ADD CONSTRAINT pk_project_users PRIMARY KEY(project_user_id);

ALTER TABLE
    project_users ADD CONSTRAINT fk_project_users_project FOREIGN KEY(project_id) REFERENCES projects(project_id);

ALTER TABLE
    project_users ADD CONSTRAINT fk_project_users_user FOREIGN KEY(user_id) REFERENCES users(user_id);
