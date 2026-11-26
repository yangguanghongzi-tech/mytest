CREATE TABLE watch_progress (
    id               BIGINT       NOT NULL PRIMARY KEY,
    tenant_id        VARCHAR(64)  NOT NULL,
    user_id          VARCHAR(64)  NOT NULL,
    content_id       BIGINT       NOT NULL,
    progress_seconds INT          NOT NULL DEFAULT 0,
    created_by       VARCHAR(64),
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_watch_progress_user_content (tenant_id, user_id, content_id),
    KEY idx_watch_progress_recent (tenant_id, user_id, updated_at)
);
