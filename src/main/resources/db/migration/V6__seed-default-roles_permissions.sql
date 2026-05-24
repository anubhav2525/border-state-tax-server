
-- ═════════════════════════════════════════════════════════════
-- SEED DATA
-- ═════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────
-- Seed: Roles
-- ─────────────────────────────────────────────────────────────
INSERT INTO roles (name, display_name, description)
VALUES
    ('SUPER_ADMIN', 'Super Administrator', 'Full system access; can manage everything including other admins'),
    ('ADMIN',       'Administrator',       'Manages states, rates, and applications; cannot manage other admins'),
    ('AGENT',       'Agent / Operator',    'Creates and manages applications on behalf of users'),
    ('USER',        'End User',            'Can submit own applications and make payments');


-- ─────────────────────────────────────────────────────────────
-- Seed: Permissions  (fixed — do not add/remove at runtime)
-- ─────────────────────────────────────────────────────────────
INSERT INTO permissions (name, resource, action, description)
VALUES
    -- Applications
    ('APPLICATION:CREATE',     'APPLICATION', 'CREATE',     'Submit a new tax application'),
    ('APPLICATION:READ_OWN',   'APPLICATION', 'READ_OWN',   'View own applications only'),
    ('APPLICATION:READ_ALL',   'APPLICATION', 'READ_ALL',   'View all applications in the system'),
    ('APPLICATION:UPDATE_OWN', 'APPLICATION', 'UPDATE_OWN', 'Edit own draft application'),
    ('APPLICATION:UPDATE_ANY', 'APPLICATION', 'UPDATE_ANY', 'Edit any application (admin use)'),
    ('APPLICATION:CANCEL_OWN', 'APPLICATION', 'CANCEL_OWN', 'Cancel own application'),
    ('APPLICATION:CANCEL_ANY', 'APPLICATION', 'CANCEL_ANY', 'Cancel any application'),
    ('APPLICATION:EXPORT',     'APPLICATION', 'EXPORT',     'Export applications to CSV/PDF'),

    -- Payments
    ('PAYMENT:INITIATE',  'PAYMENT', 'INITIATE',  'Initiate payment for an application'),
    ('PAYMENT:VIEW_OWN',  'PAYMENT', 'VIEW_OWN',  'View own payment history'),
    ('PAYMENT:VIEW_ALL',  'PAYMENT', 'VIEW_ALL',  'View all payments in the system'),
    ('PAYMENT:REFUND',    'PAYMENT', 'REFUND',    'Initiate a refund'),

    -- Tax Certificates
    ('CERTIFICATE:DOWNLOAD_OWN', 'CERTIFICATE', 'DOWNLOAD_OWN', 'Download own tax certificate'),
    ('CERTIFICATE:DOWNLOAD_ANY', 'CERTIFICATE', 'DOWNLOAD_ANY', 'Download any certificate'),
    ('CERTIFICATE:REGENERATE',   'CERTIFICATE', 'REGENERATE',   'Re-generate a certificate PDF'),

    -- Tax Rate Config
    ('TAX_RATE:READ',   'TAX_RATE', 'READ',   'View tax rates (all roles)'),
    ('TAX_RATE:CREATE', 'TAX_RATE', 'CREATE', 'Add new rate config'),
    ('TAX_RATE:UPDATE', 'TAX_RATE', 'UPDATE', 'Modify existing rate config'),
    ('TAX_RATE:DELETE', 'TAX_RATE', 'DELETE', 'Deactivate a rate config'),

    -- Master Data
    ('MASTER_DATA:READ',   'MASTER_DATA', 'READ',   'View states and vehicle categories'),
    ('MASTER_DATA:MANAGE', 'MASTER_DATA', 'MANAGE', 'Add/edit states and vehicle categories'),

    -- User Management
    ('USER:READ_OWN_PROFILE',   'USER', 'READ_OWN_PROFILE',   'View own profile'),
    ('USER:UPDATE_OWN_PROFILE', 'USER', 'UPDATE_OWN_PROFILE', 'Edit own profile'),
    ('USER:READ_ALL',           'USER', 'READ_ALL',           'View all users'),
    ('USER:CREATE',             'USER', 'CREATE',             'Create a new user account'),
    ('USER:UPDATE_ANY',         'USER', 'UPDATE_ANY',         'Edit any user account'),
    ('USER:DEACTIVATE',         'USER', 'DEACTIVATE',         'Deactivate a user account'),

    -- Role Management
    ('ROLE:READ',        'ROLE', 'READ',        'View roles and their permissions'),
    ('ROLE:MANAGE',      'ROLE', 'MANAGE',      'Assign or revoke roles from users'),
    ('ROLE:CREATE_EDIT', 'ROLE', 'CREATE_EDIT', 'Create or edit roles (super admin only)'),

    -- Reports & Dashboard
    ('REPORT:VIEW_OWN', 'REPORT', 'VIEW_OWN', 'View own activity dashboard'),
    ('REPORT:VIEW_ALL', 'REPORT', 'VIEW_ALL', 'View system-wide reports'),
    ('REPORT:EXPORT',   'REPORT', 'EXPORT',   'Export reports to Excel/PDF');


-- ─────────────────────────────────────────────────────────────
-- Seed: Role → Permission assignments
-- ─────────────────────────────────────────────────────────────

-- SUPER_ADMIN gets everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r, permissions p
WHERE  r.name = 'SUPER_ADMIN';

-- ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
           JOIN   permissions p ON p.name IN (
                                              'APPLICATION:CREATE',     'APPLICATION:READ_ALL',   'APPLICATION:UPDATE_ANY',
                                              'APPLICATION:CANCEL_ANY', 'APPLICATION:EXPORT',
                                              'PAYMENT:VIEW_ALL',       'PAYMENT:REFUND',
                                              'CERTIFICATE:DOWNLOAD_ANY','CERTIFICATE:REGENERATE',
                                              'TAX_RATE:READ',          'TAX_RATE:CREATE',        'TAX_RATE:UPDATE',
                                              'MASTER_DATA:READ',       'MASTER_DATA:MANAGE',
                                              'USER:READ_ALL',          'USER:CREATE',            'USER:UPDATE_ANY',  'USER:DEACTIVATE',
                                              'USER:READ_OWN_PROFILE',  'USER:UPDATE_OWN_PROFILE',
                                              'ROLE:READ',              'ROLE:MANAGE',
                                              'REPORT:VIEW_ALL',        'REPORT:EXPORT'
    )
WHERE  r.name = 'ADMIN';

-- AGENT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
           JOIN   permissions p ON p.name IN (
                                              'APPLICATION:CREATE',    'APPLICATION:READ_ALL',
                                              'APPLICATION:UPDATE_OWN','APPLICATION:CANCEL_OWN',
                                              'PAYMENT:INITIATE',      'PAYMENT:VIEW_ALL',
                                              'CERTIFICATE:DOWNLOAD_ANY',
                                              'TAX_RATE:READ',
                                              'MASTER_DATA:READ',
                                              'USER:READ_OWN_PROFILE', 'USER:UPDATE_OWN_PROFILE',
                                              'REPORT:VIEW_ALL'
    )
WHERE  r.name = 'AGENT';

-- USER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r
           JOIN   permissions p ON p.name IN (
                                              'APPLICATION:CREATE',    'APPLICATION:READ_OWN',
                                              'APPLICATION:UPDATE_OWN','APPLICATION:CANCEL_OWN',
                                              'PAYMENT:INITIATE',      'PAYMENT:VIEW_OWN',
                                              'CERTIFICATE:DOWNLOAD_OWN',
                                              'TAX_RATE:READ',
                                              'MASTER_DATA:READ',
                                              'USER:READ_OWN_PROFILE', 'USER:UPDATE_OWN_PROFILE',
                                              'REPORT:VIEW_OWN'
    )
WHERE  r.name = 'USER';