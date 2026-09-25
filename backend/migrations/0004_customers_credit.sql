PRAGMA foreign_keys = ON;

ALTER TABLE customers ADD COLUMN normalized_phone TEXT;
CREATE INDEX customers_phone_idx ON customers(normalized_phone);

CREATE TABLE shop_customers (
 id TEXT PRIMARY KEY,
 shop_id TEXT NOT NULL REFERENCES shops(id),
 customer_id TEXT NOT NULL REFERENCES customers(id),
 normalized_phone TEXT,
 display_name_override TEXT,
 notes TEXT,
 credit_enabled INTEGER NOT NULL DEFAULT 0 CHECK(credit_enabled IN (0,1)),
 credit_limit_paise INTEGER CHECK(credit_limit_paise IS NULL OR credit_limit_paise >= 0),
 current_balance_paise INTEGER NOT NULL DEFAULT 0 CHECK(current_balance_paise >= 0),
 is_archived INTEGER NOT NULL DEFAULT 0 CHECK(is_archived IN (0,1)),
 version INTEGER NOT NULL DEFAULT 1,
 created_at INTEGER NOT NULL,
 updated_at INTEGER NOT NULL,
 UNIQUE(shop_id, customer_id)
) STRICT;
CREATE UNIQUE INDEX shop_customers_phone_uq ON shop_customers(shop_id,normalized_phone) WHERE normalized_phone IS NOT NULL;
CREATE INDEX shop_customers_shop_idx ON shop_customers(shop_id,is_archived,updated_at DESC);
CREATE INDEX shop_customers_customer_idx ON shop_customers(customer_id);
CREATE INDEX shop_customers_balance_idx ON shop_customers(shop_id,current_balance_paise);

CREATE TABLE credit_transactions (
 id TEXT PRIMARY KEY,
 shop_id TEXT NOT NULL REFERENCES shops(id),
 shop_customer_id TEXT NOT NULL REFERENCES shop_customers(id),
 type TEXT NOT NULL CHECK(type IN ('CREDIT_SALE','PAYMENT','ADJUSTMENT_CREDIT','ADJUSTMENT_DEBIT','REVERSAL')),
 amount_paise INTEGER NOT NULL CHECK(amount_paise > 0),
 balance_after_paise INTEGER NOT NULL CHECK(balance_after_paise >= 0),
 reference_type TEXT,
 reference_id TEXT,
 note TEXT,
 mutation_id TEXT NOT NULL,
 actor_user_id TEXT NOT NULL REFERENCES users(id),
 created_at INTEGER NOT NULL,
 UNIQUE(shop_id,mutation_id)
) STRICT;
CREATE INDEX credit_transactions_shop_idx ON credit_transactions(shop_id,created_at DESC);
CREATE INDEX credit_transactions_customer_idx ON credit_transactions(shop_customer_id,created_at DESC);
CREATE INDEX credit_transactions_created_idx ON credit_transactions(created_at DESC);

ALTER TABLE sales ADD COLUMN shop_customer_id TEXT REFERENCES shop_customers(id);
ALTER TABLE sales ADD COLUMN payment_type TEXT NOT NULL DEFAULT 'PAID' CHECK(payment_type IN ('PAID','CREDIT'));
ALTER TABLE sales ADD COLUMN credit_amount_paise INTEGER NOT NULL DEFAULT 0 CHECK(credit_amount_paise >= 0);
CREATE INDEX sales_shop_customer_idx ON sales(shop_id,shop_customer_id,created_at DESC);
