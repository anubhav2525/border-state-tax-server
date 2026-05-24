-- ============================================================
-- BORDER TAX PAYMENT PLATFORM — Production Schema (PostgreSQL)
-- ============================================================

CREATE TYPE payment_mode_enum AS ENUM ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY');
CREATE TYPE app_status_enum AS ENUM ('DRAFT', 'SUBMITTED', 'PAYMENT_PENDING', 'PAID', 'CANCELLED', 'EXPIRED');
CREATE TYPE pay_status_enum AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED');
CREATE TYPE tax_mode_enum AS ENUM ('BORDER','ROAD','ALL_INDIA_TAX');
CREATE TYPE permit_mode_enum AS ENUM ('AUTHENTICATION_FEE','ALL_INDIA_TAX')