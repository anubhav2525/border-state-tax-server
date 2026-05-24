-- ----------------------------
-- 1. APPLICATIONS  ← USER FORM SUBMISSION
-- ----------------------------
CREATE TABLE applications
(
    id                 UUID PRIMARY KEY           DEFAULT gen_random_uuid(),

    -- Rate matrix selection
    state_name         VARCHAR(30)       NOT NULL UNIQUE,
    vehicle_seating    VARCHAR(30)       NOT NULL UNIQUE,
    tax_type           tax_mode_enum     NOT NULL,
    tax_rate_config_id UUID              NOT NULL REFERENCES tax_rate_configs (id), -- LOCKED at submission

    -- Payment period
    payment_mode       payment_mode_enum NOT NULL,
    start_date         DATE              NOT NULL,
    end_date           DATE              NOT NULL,
    number_of_days     INT               NOT NULL,

    -- Calculated amounts (snapshot at submission — never recalculate from live rates)
    base_tax_amount    NUMERIC(12, 2)    NOT NULL,
    commission_amount  NUMERIC(12, 2)    NOT NULL,
    total_amount       NUMERIC(12, 2)    NOT NULL,

    -- Lifecycle
    status             app_status_enum   NOT NULL DEFAULT 'DRAFT',
    submitted_at       TIMESTAMP,
    created_at         TIMESTAMP         NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP         NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_days CHECK (number_of_days > 0),
    CONSTRAINT chk_amounts CHECK (total_amount = base_tax_amount + commission_amount)
);

CREATE INDEX idx_app_id ON applications (id);
CREATE INDEX idx_app_status ON applications (status);
CREATE INDEX idx_app_dates ON applications (start_date, end_date);

-- ----------------------------
-- 2. PAYMENTS
-- ----------------------------
CREATE TABLE payments
(
    id                 UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    application_id     UUID            NOT NULL REFERENCES applications (id),
    payment_reference  VARCHAR(50)     NOT NULL UNIQUE, -- internal ref
    gateway_order_id   VARCHAR(100) UNIQUE,             -- Razorpay order_id
    gateway_payment_id VARCHAR(100) UNIQUE,             -- Razorpay payment_id
    amount             NUMERIC(12, 2)  NOT NULL,
    payment_method     VARCHAR(30),                     -- UPI, CARD, NETBANKING
    status             pay_status_enum NOT NULL DEFAULT 'PENDING',
    failure_reason     TEXT,
    paid_at            TIMESTAMP,
    created_at         TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_app ON payments (application_id);

-- ----------------------------
-- 3. TAX CERTIFICATES
-- ----------------------------
CREATE TABLE tax_certificates
(
    id                 UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    application_id     UUID        NOT NULL REFERENCES applications (id) UNIQUE,
    payment_id         UUID        NOT NULL REFERENCES payments (id) UNIQUE,
    certificate_number VARCHAR(50) NOT NULL UNIQUE,
    file_path          VARCHAR(500), -- S3 / local path to PDF
    valid_from         DATE        NOT NULL,
    valid_to           DATE        NOT NULL,

    issued_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP   NOT NULL DEFAULT NOW()
);
