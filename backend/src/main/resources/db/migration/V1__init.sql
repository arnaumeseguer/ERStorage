-- Users of ERStorage
CREATE TABLE users (
                       id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       email               VARCHAR(255) NOT NULL UNIQUE,
                       password_hash       VARCHAR(100) NOT NULL,
                       display_name        VARCHAR(100) NOT NULL,
                       role                VARCHAR(20)  NOT NULL DEFAULT 'USER',
                       storage_used_bytes  BIGINT       NOT NULL DEFAULT 0,
                       storage_quota_bytes BIGINT       NOT NULL DEFAULT 1073741824, -- 1 GB
                       created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT chk_users_storage_used CHECK (storage_used_bytes >= 0)
);

-- Folders: parent_id NULL = folder is in the user's root
CREATE TABLE folders (
                         id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                         owner_id   UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                         parent_id  UUID         REFERENCES folders (id) ON DELETE CASCADE,
                         name       VARCHAR(255) NOT NULL,
                         created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
                         updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
                         CONSTRAINT uq_folders_name UNIQUE NULLS NOT DISTINCT (owner_id, parent_id, name)
);

CREATE INDEX idx_folders_parent ON folders (parent_id);

-- Files: only metadata here, the bytes live in RustFS / R2
CREATE TABLE files (
                       id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       owner_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                       folder_id    UUID         REFERENCES folders (id) ON DELETE CASCADE,
                       name         VARCHAR(255) NOT NULL,
                       storage_key  VARCHAR(512) NOT NULL UNIQUE,
                       content_type VARCHAR(255) NOT NULL,
                       size_bytes   BIGINT       NOT NULL,
                       created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT uq_files_name UNIQUE NULLS NOT DISTINCT (owner_id, folder_id, name),
                       CONSTRAINT chk_files_size CHECK (size_bytes >= 0)
);

CREATE INDEX idx_files_folder ON files (folder_id);