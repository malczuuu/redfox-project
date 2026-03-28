CREATE
    TABLE
        users(
            user_id UUID DEFAULT gen_random_uuid(),
            user_login VARCHAR(255) NOT NULL,
            user_passhash VARCHAR(255) NOT NULL,
            user_first_name VARCHAR(255) NOT NULL,
            user_last_name VARCHAR(255) NOT NULL,
            user_version BIGINT NOT NULL DEFAULT 0,
            user_created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            user_updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            user_deleted_at TIMESTAMPTZ DEFAULT NULL
        );

ALTER TABLE
    users ADD CONSTRAINT pk_users PRIMARY KEY(user_id);

CREATE
    UNIQUE INDEX idx_users_login_unique ON
    users(
        LOWER( user_login )
    );
