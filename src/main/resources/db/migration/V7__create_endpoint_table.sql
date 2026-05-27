-- ==============================
-- ENDPOINT_PERMISSIONS (DYNAMIC RBAC POLICY)
-- ==============================

CREATE TABLE endpoint_permissions
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),

    http_method   VARCHAR(10)  NOT NULL,
    path_pattern  VARCHAR(255) NOT NULL,
    permission_id UUID,

    is_public     BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,

    description   TEXT,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,

    CONSTRAINT fk_endpoint_permissions_permission
        FOREIGN KEY (permission_id)
            REFERENCES permissions (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_endpoint_permissions_method
        CHECK (http_method IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS', 'HEAD')),

    CONSTRAINT chk_endpoint_permissions_public_rule
        CHECK (
            (is_public = TRUE AND permission_id IS NULL) OR
            (is_public = FALSE AND permission_id IS NOT NULL)
            )
);

-- One active mapping per method + path.
CREATE UNIQUE INDEX uk_endpoint_permissions_active_route
    ON endpoint_permissions (http_method, path_pattern) WHERE deleted = FALSE;

-- Fast lookup for runtime auth check.
CREATE INDEX idx_endpoint_permissions_lookup
    ON endpoint_permissions (deleted, enabled, http_method, path_pattern);

-- Fast reverse lookup: where a permission is used.
CREATE INDEX idx_endpoint_permissions_permission
    ON endpoint_permissions (permission_id);


-- ==============================
-- AUTHZ CACHE VERSION
-- ==============================
-- App can keep Redis/in-memory authz cache and refresh only when version changes.

CREATE TABLE authz_cache_version
(
    key        VARCHAR(50) PRIMARY KEY,
    version    BIGINT      NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO authz_cache_version (key, version)
VALUES ('GLOBAL', 1);


-- ==============================
-- VERSION BUMP FUNCTION + TRIGGERS
-- ==============================

CREATE
OR REPLACE FUNCTION bump_global_authz_cache_version()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
UPDATE authz_cache_version
SET version    = version + 1,
    updated_at = NOW()
WHERE key = 'GLOBAL';

RETURN NULL;
END;
$$;

-- Bump when endpoint policy changes.
CREATE TRIGGER trg_bump_authz_on_endpoint_permissions
    AFTER INSERT OR
UPDATE OR
DELETE
ON endpoint_permissions
    FOR EACH STATEMENT
    EXECUTE FUNCTION bump_global_authz_cache_version();

-- Bump when role-to-permission mapping changes.
CREATE TRIGGER trg_bump_authz_on_role_permissions
    AFTER INSERT OR
UPDATE OR
DELETE
ON role_permissions
    FOR EACH STATEMENT
    EXECUTE FUNCTION bump_global_authz_cache_version();

-- Bump when user-to-role mapping changes.
CREATE TRIGGER trg_bump_authz_on_user_roles
    AFTER INSERT OR
UPDATE OR
DELETE
ON user_roles
    FOR EACH STATEMENT
    EXECUTE FUNCTION bump_global_authz_cache_version();
