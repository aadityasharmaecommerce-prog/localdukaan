PRAGMA foreign_keys = OFF;

ALTER TABLE inventory_transactions RENAME TO inventory_transactions_v2;
CREATE TABLE inventory_transactions (
 id TEXT PRIMARY KEY,
 shop_id TEXT NOT NULL REFERENCES shops(id),
 shop_product_id TEXT NOT NULL REFERENCES shop_products(id),
 type TEXT NOT NULL CHECK(type IN ('OPENING_STOCK','PURCHASE','SALE','DAMAGE','EXPIRED','LOSS','ADJUSTMENT','RETURN','RETURN_OUT','TRANSFER')),
 quantity_delta_milli INTEGER NOT NULL,
 stock_after_milli INTEGER NOT NULL CHECK(stock_after_milli >= 0),
 reference_type TEXT,
 reference_id TEXT,
 note TEXT,
 mutation_id TEXT NOT NULL,
 actor_user_id TEXT NOT NULL REFERENCES users(id),
 created_at INTEGER NOT NULL,
 UNIQUE(shop_id, mutation_id)
) STRICT;
INSERT INTO inventory_transactions(id,shop_id,shop_product_id,type,quantity_delta_milli,stock_after_milli,reference_type,reference_id,mutation_id,actor_user_id,created_at)
SELECT id,shop_id,shop_product_id,type,quantity_delta_milli,stock_after_milli,reference_type,reference_id,mutation_id,actor_user_id,created_at FROM inventory_transactions_v2;
DROP TABLE inventory_transactions_v2;
CREATE INDEX inventory_transactions_shop_idx ON inventory_transactions(shop_id, created_at DESC);
CREATE INDEX inventory_transactions_product_idx ON inventory_transactions(shop_product_id, created_at DESC);
CREATE INDEX inventory_transactions_created_idx ON inventory_transactions(created_at DESC);

CREATE TABLE sales (
 id TEXT PRIMARY KEY,
 shop_id TEXT NOT NULL REFERENCES shops(id),
 subtotal_paise INTEGER NOT NULL CHECK(subtotal_paise >= 0),
 total_paise INTEGER NOT NULL CHECK(total_paise >= 0),
 customer_id TEXT REFERENCES customers(id),
 status TEXT NOT NULL DEFAULT 'COMPLETED' CHECK(status IN ('COMPLETED')),
 mutation_id TEXT NOT NULL,
 client_created_at INTEGER,
 created_at INTEGER NOT NULL,
 UNIQUE(shop_id, mutation_id)
) STRICT;
CREATE INDEX sales_shop_idx ON sales(shop_id, created_at DESC);
CREATE INDEX sales_created_idx ON sales(created_at DESC);

CREATE TABLE sale_items (
 id TEXT PRIMARY KEY,
 sale_id TEXT NOT NULL REFERENCES sales(id) ON DELETE RESTRICT,
 shop_id TEXT NOT NULL REFERENCES shops(id),
 shop_product_id TEXT NOT NULL REFERENCES shop_products(id),
 product_name_snapshot TEXT NOT NULL,
 unit_snapshot TEXT NOT NULL,
 unit_price_paise INTEGER NOT NULL CHECK(unit_price_paise >= 0),
 quantity_milli INTEGER NOT NULL CHECK(quantity_milli > 0),
 line_total_paise INTEGER NOT NULL CHECK(line_total_paise >= 0),
 created_at INTEGER NOT NULL
) STRICT;
CREATE INDEX sale_items_sale_idx ON sale_items(sale_id);
CREATE INDEX sale_items_product_idx ON sale_items(shop_product_id);

CREATE TABLE purchase_list_items (
 id TEXT PRIMARY KEY,
 shop_id TEXT NOT NULL REFERENCES shops(id),
 shop_product_id TEXT NOT NULL REFERENCES shop_products(id),
 quantity_milli INTEGER NOT NULL CHECK(quantity_milli > 0),
 note TEXT,
 is_purchased INTEGER NOT NULL DEFAULT 0 CHECK(is_purchased IN (0,1)),
 version INTEGER NOT NULL DEFAULT 1,
 created_at INTEGER NOT NULL,
 updated_at INTEGER NOT NULL,
 UNIQUE(shop_id, shop_product_id)
) STRICT;
CREATE INDEX purchase_list_shop_idx ON purchase_list_items(shop_id, is_purchased, updated_at DESC);

PRAGMA foreign_keys = ON;
