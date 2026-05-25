-- ----------------------------
-- 4. TAX RATE CONFIGURATION  ← CORE TABLE
-- ----------------------------
CREATE TABLE tax_rate_configs
(
    id                   UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    state_name           VARCHAR(30)    NOT NULL,
    vehicle_seating      VARCHAR(30)    NOT NULL,
    tax_type             tax_mode_enum  NOT NULL,

    -- Rates (tax + commission stored separately for reporting)
    daily_rate           NUMERIC(10, 2) NOT NULL DEFAULT 0,
    daily_commission     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    weekly_rate          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    weekly_commission    NUMERIC(10, 2) NOT NULL DEFAULT 0,
    monthly_rate         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    monthly_commission   NUMERIC(10, 2) NOT NULL DEFAULT 0,
    quarterly_rate       NUMERIC(10, 2) NOT NULL DEFAULT 0,
    quarterly_commission NUMERIC(10, 2) NOT NULL DEFAULT 0,
    yearly_rate          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    yearly_commission    NUMERIC(10, 2) NOT NULL DEFAULT 0,

    -- Validity window (for future rate revisions)
    enabled              BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted              BOOLEAN        NOT NULL DEFAULT FALSE,

    created_at           TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP      NOT NULL DEFAULT NOW(),

    -- Only one active config per state+vehicle+taxtype combination
    CONSTRAINT uq_rate_config UNIQUE (state_name, vehicle_seating, tax_type)
);
