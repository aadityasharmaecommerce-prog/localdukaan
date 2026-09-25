PRAGMA foreign_keys = ON;

ALTER TABLE shops ADD COLUMN public_slug TEXT;
ALTER TABLE shops ADD COLUMN phone_e164 TEXT;
ALTER TABLE shops ADD COLUMN logo_image_key TEXT;
ALTER TABLE shops ADD COLUMN banner_image_key TEXT;
CREATE UNIQUE INDEX shops_public_slug_uq ON shops(public_slug) WHERE public_slug IS NOT NULL;

CREATE TABLE products (
 id TEXT PRIMARY KEY,
 barcode TEXT,
 name TEXT NOT NULL CHECK(length(trim(name)) BETWEEN 1 AND 160),
 brand TEXT,
 unit TEXT NOT NULL CHECK(length(trim(unit)) BETWEEN 1 AND 40),
 description TEXT,
 created_by_user_id TEXT NOT NULL REFERENCES users(id),
 version INTEGER NOT NULL DEFAULT 1,
 created_at INTEGER NOT NULL,
 updated_at INTEGER NOT NULL
) STRICT;
CREATE UNIQUE INDEX products_barcode_uq ON products(barcode) WHERE barcode IS NOT NULL;
CREATE INDEX products_name_idx ON products(name);

CREATE TABLE shop_products (
 id TEXT PRIMARY KEY,
 shop_id TEXT NOT NULL REFERENCES shops(id),
 product_id TEXT NOT NULL REFERENCES products(id),
 selling_price_paise INTEGER NOT NULL CHECK(selling_price_paise >= 0),
 stock_quantity_milli INTEGER NOT NULL DEFAULT 0 CHECK(stock_quantity_milli >= 0),
 reserved_quantity_milli INTEGER NOT NULL DEFAULT 0 CHECK(reserved_quantity_milli >= 0 AND reserved_quantity_milli <= stock_quantity_milli),
 minimum_stock_milli INTEGER NOT NULL DEFAULT 0 CHECK(minimum_stock_milli >= 0),
 is_available INTEGER NOT NULL DEFAULT 1 CHECK(is_available IN (0,1)),
 is_archived INTEGER NOT NULL DEFAULT 0 CHECK(is_archived IN (0,1)),
 image_key TEXT,
 image_mime_type TEXT,
 image_size_bytes INTEGER CHECK(image_size_bytes IS NULL OR image_size_bytes >= 0),
 version INTEGER NOT NULL DEFAULT 1,
 created_at INTEGER NOT NULL,
 updated_at INTEGER NOT NULL,
 UNIQUE(shop_id, product_id)
) STRICT;
CREATE INDEX shop_products_shop_idx ON shop_products(shop_id, is_archived, updated_at DESC);
CREATE INDEX shop_products_product_idx ON shop_products(product_id);

CREATE TABLE inventory_transactions (
 id TEXT PRIMARY KEY,
 shop_id TEXT NOT NULL REFERENCES shops(id),
 shop_product_id TEXT NOT NULL REFERENCES shop_products(id),
 type TEXT NOT NULL CHECK(type IN ('OPENING_STOCK','PURCHASE','SALE','ADJUSTMENT','RETURN','DAMAGE','TRANSFER')),
 quantity_delta_milli INTEGER NOT NULL,
 stock_after_milli INTEGER NOT NULL CHECK(stock_after_milli >= 0),
 reference_type TEXT,
 reference_id TEXT,
 mutation_id TEXT NOT NULL,
 actor_user_id TEXT NOT NULL REFERENCES users(id),
 created_at INTEGER NOT NULL,
 UNIQUE(shop_id, mutation_id)
) STRICT;
CREATE INDEX inventory_transactions_shop_idx ON inventory_transactions(shop_id, created_at DESC);
CREATE INDEX inventory_transactions_product_idx ON inventory_transactions(shop_product_id, created_at DESC);
