-- ============================================================
-- RBAC SCHEMA — Users, Roles, Permissions
-- PostgreSQL | Border Tax Payment Platform
-- ============================================================

-- ─────────────────────────────────────────────
-- 1. PERMISSIONS
-- ─────────────────────────────────────────────
-- Pattern: RESOURCE:ACTION
-- e.g.     APPLICATION:CREATE, TAX_RATE:UPDATE
-- ─────────────────────────────────────────────
CREATE TABLE permissions
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE, -- 'APPLICATION:CREATE'
    resource    VARCHAR(50)  NOT NULL,        -- 'APPLICATION'
    action      VARCHAR(50)  NOT NULL,        -- 'CREATE'
    description TEXT,

    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_permission_resource_action UNIQUE (resource, action)
);

CREATE INDEX idx_permissions_resource ON permissions (resource);

-- ─────────────────────────────────────────────
-- 2. ROLES
-- ─────────────────────────────────────────────
CREATE TABLE roles
(
    id           UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name         VARCHAR(50)  NOT NULL UNIQUE, -- 'SUPER_ADMIN', 'ADMIN', 'AGENT', 'USER'
    display_name VARCHAR(100) NOT NULL,        -- 'Super Administrator', 'Agent'
    description  TEXT,

    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────
-- 3. USERS
-- ─────────────────────────────────────────────
CREATE TABLE users
(
    id                       UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    full_name                VARCHAR(150) NOT NULL,
    phone                    VARCHAR(15)  NOT NULL UNIQUE,
    email                    VARCHAR(150) UNIQUE,
    password_hash            VARCHAR(255) NOT NULL,

    -- Account state
    enabled                  BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted                  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_email_verified        BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Security tracking
    failed_login_attempts    INT          NOT NULL DEFAULT 0,
    locked_until             TIMESTAMP,    -- NULL = not locked
    last_login_at            TIMESTAMP,
    password_changed_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    -- Refresh token support (JWT rotation)
    refresh_token_hash       VARCHAR(255), -- hashed, not plain
    refresh_token_expires_at TIMESTAMP,

    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_phone_format CHECK (phone ~ '^\+?[0-9]{10,15}$')
);

CREATE INDEX idx_users_phone ON users (phone);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_enabled ON users (enabled);

-- ─────────────────────────────────────────────
-- 4. ROLE_PERMISSIONS  (Many-to-Many)
-- ─────────────────────────────────────────────
CREATE TABLE role_permissions
(
    role_id       UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,

    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role ON role_permissions (role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_id);

-- ── Assign permissions to SUPER_ADMIN (everything) ────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'SUPER_ADMIN'),
       id
FROM permissions;

-- ─────────────────────────────────────────────
-- 5. USER_ROLES  (Many-to-Many)
-- ─────────────────────────────────────────────
-- One user can have multiple roles
-- e.g. someone can be both AGENT and USER
-- ─────────────────────────────────────────────
CREATE TABLE user_roles
(
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,

    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_roles (user_id);
CREATE INDEX idx_user_roles_role ON user_roles (role_id);