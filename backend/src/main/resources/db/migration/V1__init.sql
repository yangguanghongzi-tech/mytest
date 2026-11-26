-- 所有业务表均带 tenant_id，由 MyBatis-Plus 租户插件自动追加过滤条件与写入值。

CREATE TABLE category (
    id          BIGINT       NOT NULL PRIMARY KEY,
    tenant_id   VARCHAR(64)  NOT NULL,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(50)  NOT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    created_by  VARCHAR(64),
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    KEY idx_category_tenant_parent (tenant_id, parent_id, sort)
);

CREATE TABLE content (
    id           BIGINT       NOT NULL PRIMARY KEY,
    tenant_id    VARCHAR(64)  NOT NULL,
    category_id  BIGINT       NOT NULL,
    type         VARCHAR(16)  NOT NULL,
    title        VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    cover_key    VARCHAR(255),
    status       VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    sort         INT          NOT NULL DEFAULT 0,
    published_at DATETIME,
    created_by   VARCHAR(64),
    created_at   DATETIME     NOT NULL,
    updated_at   DATETIME     NOT NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    KEY idx_content_tenant_cat_status (tenant_id, category_id, status)
);

CREATE TABLE media_file (
    id               BIGINT       NOT NULL PRIMARY KEY,
    tenant_id        VARCHAR(64)  NOT NULL,
    content_id       BIGINT       NOT NULL,
    object_key       VARCHAR(255) NOT NULL,
    file_name        VARCHAR(200) NOT NULL,
    mime             VARCHAR(128) NOT NULL,
    file_size        BIGINT       NOT NULL,
    duration         INT,
    hls_key          VARCHAR(255),
    transcode_status VARCHAR(16),
    created_by       VARCHAR(64),
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    KEY idx_media_tenant_content (tenant_id, content_id)
);

CREATE TABLE text_body (
    id          BIGINT       NOT NULL PRIMARY KEY,
    tenant_id   VARCHAR(64)  NOT NULL,
    content_id  BIGINT       NOT NULL,
    body        MEDIUMTEXT,
    created_by  VARCHAR(64),
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    KEY idx_text_tenant_content (tenant_id, content_id)
);

CREATE TABLE upload_task (
    id          BIGINT       NOT NULL PRIMARY KEY,
    tenant_id   VARCHAR(64)  NOT NULL,
    purpose     VARCHAR(16)  NOT NULL,
    upload_id   VARCHAR(128) NOT NULL,
    object_key  VARCHAR(255) NOT NULL,
    file_name   VARCHAR(200) NOT NULL,
    mime        VARCHAR(128) NOT NULL,
    file_size   BIGINT       NOT NULL,
    part_size   BIGINT       NOT NULL,
    part_count  INT          NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    created_by  VARCHAR(64),
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    KEY idx_upload_tenant_status (tenant_id, status, created_at)
);
