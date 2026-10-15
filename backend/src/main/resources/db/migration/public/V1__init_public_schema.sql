CREATE TABLE public.businesses (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(40)   NOT NULL,
    name            VARCHAR(120)  NOT NULL,
    tax_id          VARCHAR(30),
    address         VARCHAR(200),
    phone           VARCHAR(30),
    currency        VARCHAR(3)    NOT NULL DEFAULT 'BOB',
    currency_symbol VARCHAR(5)    NOT NULL DEFAULT 'Bs',
    tax_rate        NUMERIC(5, 2) NOT NULL DEFAULT 0,
    receipt_message VARCHAR(250),
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    schema_name     VARCHAR(63),
    CONSTRAINT uq_businesses_code UNIQUE (code),
    CONSTRAINT uq_businesses_schema_name UNIQUE (schema_name),
    CONSTRAINT ck_businesses_tax_rate CHECK (tax_rate BETWEEN 0 AND 100)
);

CREATE TABLE public.platform_admins (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    name          VARCHAR(120) NOT NULL,
    CONSTRAINT uq_platform_admins_username UNIQUE (username)
);
