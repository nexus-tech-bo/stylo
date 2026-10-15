CREATE TABLE ${flyway:defaultSchema}.users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120)  NOT NULL,
    username        VARCHAR(50)   NOT NULL,
    password_hash   VARCHAR(100)  NOT NULL,
    role            VARCHAR(20)   NOT NULL,
    phone           VARCHAR(30),
    commission_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'CASHIER', 'STYLIST')),
    CONSTRAINT ck_users_commission_rate CHECK (commission_rate BETWEEN 0 AND 100)
);

CREATE TABLE ${flyway:defaultSchema}.categories (
    id     BIGSERIAL PRIMARY KEY,
    name   VARCHAR(80) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE TABLE ${flyway:defaultSchema}.salon_services (
    id               BIGSERIAL PRIMARY KEY,
    category_id      BIGINT         NOT NULL REFERENCES ${flyway:defaultSchema}.categories (id),
    name             VARCHAR(120)   NOT NULL,
    description      VARCHAR(500),
    duration_minutes INTEGER        NOT NULL,
    price            NUMERIC(12, 2) NOT NULL,
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    image_url        VARCHAR(500),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_salon_services_duration CHECK (duration_minutes >= 1),
    CONSTRAINT ck_salon_services_price CHECK (price >= 0)
);
CREATE INDEX ix_salon_services_category ON ${flyway:defaultSchema}.salon_services (category_id);

CREATE TABLE ${flyway:defaultSchema}.products (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120)   NOT NULL,
    sku        VARCHAR(50),
    price      NUMERIC(12, 2) NOT NULL,
    stock      INTEGER        NOT NULL DEFAULT 0,
    min_stock  INTEGER        NOT NULL DEFAULT 0,
    active     BOOLEAN        NOT NULL DEFAULT TRUE,
    image_url  VARCHAR(500),
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version    BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT ck_products_stock CHECK (stock >= 0),
    CONSTRAINT ck_products_min_stock CHECK (min_stock >= 0)
);

CREATE TABLE ${flyway:defaultSchema}.customers (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    phone      VARCHAR(30),
    email      VARCHAR(120),
    tax_id     VARCHAR(30),
    notes      VARCHAR(500),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE ${flyway:defaultSchema}.cash_registers (
    id             BIGSERIAL PRIMARY KEY,
    status         VARCHAR(10)    NOT NULL,
    opened_by_id   BIGINT         NOT NULL REFERENCES ${flyway:defaultSchema}.users (id),
    opened_at      TIMESTAMPTZ    NOT NULL,
    opening_amount NUMERIC(12, 2) NOT NULL,
    closed_by_id   BIGINT REFERENCES ${flyway:defaultSchema}.users (id),
    closed_at      TIMESTAMPTZ,
    expected_cash  NUMERIC(12, 2),
    counted_cash   NUMERIC(12, 2),
    notes          VARCHAR(500),
    version        BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_cash_registers_status CHECK (status IN ('OPEN', 'CLOSED'))
);
-- At most one OPEN cash register per business (schema).
CREATE UNIQUE INDEX uq_cash_registers_one_open ON ${flyway:defaultSchema}.cash_registers (status) WHERE status = 'OPEN';
CREATE INDEX ix_cash_registers_opened_at ON ${flyway:defaultSchema}.cash_registers (opened_at);

CREATE TABLE ${flyway:defaultSchema}.sales (
    id               BIGSERIAL PRIMARY KEY,
    cash_register_id BIGINT         NOT NULL REFERENCES ${flyway:defaultSchema}.cash_registers (id),
    cashier_id       BIGINT         NOT NULL REFERENCES ${flyway:defaultSchema}.users (id),
    customer_id      BIGINT REFERENCES ${flyway:defaultSchema}.customers (id),
    subtotal         NUMERIC(12, 2) NOT NULL,
    discount         NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total            NUMERIC(12, 2) NOT NULL,
    tax              NUMERIC(12, 2) NOT NULL DEFAULT 0,
    payment_method   VARCHAR(10)    NOT NULL,
    amount_received  NUMERIC(12, 2),
    change_amount    NUMERIC(12, 2),
    status           VARCHAR(10)    NOT NULL,
    voided_by_id     BIGINT REFERENCES ${flyway:defaultSchema}.users (id),
    voided_at        TIMESTAMPTZ,
    void_reason      VARCHAR(250),
    notes            VARCHAR(500),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version          BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_sales_payment_method CHECK (payment_method IN ('CASH', 'QR', 'CARD', 'TRANSFER')),
    CONSTRAINT ck_sales_status CHECK (status IN ('COMPLETED', 'VOIDED'))
);
CREATE INDEX ix_sales_created_at ON ${flyway:defaultSchema}.sales (created_at);
CREATE INDEX ix_sales_customer ON ${flyway:defaultSchema}.sales (customer_id);
CREATE INDEX ix_sales_cash_register ON ${flyway:defaultSchema}.sales (cash_register_id);

CREATE TABLE ${flyway:defaultSchema}.sale_items (
    id              BIGSERIAL PRIMARY KEY,
    sale_id         BIGINT         NOT NULL REFERENCES ${flyway:defaultSchema}.sales (id) ON DELETE CASCADE,
    type            VARCHAR(10)    NOT NULL,
    item_id         BIGINT         NOT NULL,
    description     VARCHAR(120)   NOT NULL,
    quantity        INTEGER        NOT NULL,
    unit_price      NUMERIC(12, 2) NOT NULL,
    discount        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    subtotal        NUMERIC(12, 2) NOT NULL,
    stylist_id      BIGINT REFERENCES ${flyway:defaultSchema}.users (id),
    commission_rate NUMERIC(5, 2),
    CONSTRAINT ck_sale_items_type CHECK (type IN ('SERVICE', 'PRODUCT')),
    CONSTRAINT ck_sale_items_quantity CHECK (quantity BETWEEN 1 AND 999)
);
CREATE INDEX ix_sale_items_sale ON ${flyway:defaultSchema}.sale_items (sale_id);
CREATE INDEX ix_sale_items_stylist ON ${flyway:defaultSchema}.sale_items (stylist_id);
CREATE INDEX ix_sale_items_type_item ON ${flyway:defaultSchema}.sale_items (type, item_id);
