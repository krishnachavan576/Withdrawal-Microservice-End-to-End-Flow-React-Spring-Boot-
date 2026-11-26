CREATE TABLE users (
    id            UUID PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(120) NOT NULL,
    phone         VARCHAR(15)  NOT NULL
);

CREATE TABLE accounts (
    id                  UUID PRIMARY KEY,
    account_number      VARCHAR(20)   NOT NULL UNIQUE,
    user_id             UUID          NOT NULL REFERENCES users(id),
    balance             NUMERIC(19,2) NOT NULL CHECK (balance >= 0),
    status              VARCHAR(20)   NOT NULL,
    daily_limit         NUMERIC(19,2) NOT NULL,
    rzp_contact_id      VARCHAR(50),
    rzp_fund_account_id VARCHAR(50),
    beneficiary_name    VARCHAR(100),
    beneficiary_ifsc    VARCHAR(11),
    beneficiary_last4   VARCHAR(4),
    version             BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_accounts_user ON accounts(user_id);

CREATE TABLE transactions (
    id              UUID PRIMARY KEY,
    account_id      UUID          NOT NULL REFERENCES accounts(id),
    amount          NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    balance_after   NUMERIC(19,2),
    status          VARCHAR(20)   NOT NULL,
    idempotency_key VARCHAR(100)  NOT NULL,
    payout_id       VARCHAR(50),
    failure_reason  VARCHAR(500),
    refunded        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_txn_idem UNIQUE (account_id, idempotency_key)
);
CREATE INDEX idx_txn_account_created ON transactions(account_id, created_at DESC);
CREATE INDEX idx_txn_status_created  ON transactions(status, created_at);
CREATE INDEX idx_txn_payout          ON transactions(payout_id);
