PRAGMA foreign_keys = ON;

-- Phase 3: local marketplace. Shopkeeper remains inventory owner and fulfilment point.
CREATE TABLE orders (
 id TEXT PRIMARY KEY,
 shop_id TEXT NOT NULL REFERENCES shops(id),
 customer_user_id TEXT NOT NULL REFERENCES users(id),
 status TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','ACCEPTED','REJECTED','PACKING','READY','OUT_FOR_DELIVERY','DELIVERED','CANCELLED')),
 fulfillment_type TEXT NOT NULL CHECK(fulfillment_type IN ('DELIVERY','PICKUP')),
 address_id TEXT REFERENCES addresses(id),
 recipient_name TEXT,
 mobile_e164 TEXT,
 address_line TEXT,
 locality TEXT,
 city TEXT,
 pincode TEXT,
 latitude REAL,
 longitude REAL,
 subtotal_paise INTEGER NOT NULL CHECK(subtotal_paise >= 0),
 delivery_fee_paise INTEGER NOT NULL DEFAULT 0 CHECK(delivery_fee_paise >= 0),
 total_paise INTEGER NOT NULL CHECK(total_paise >= 0),
 note TEXT,
 mutation_id TEXT NOT NULL,
 version INTEGER NOT NULL DEFAULT 1,
 created_at INTEGER NOT NULL,
 updated_at INTEGER NOT NULL,
 UNIQUE(customer_user_id, mutation_id)
) STRICT;
CREATE INDEX orders_shop_idx ON orders(shop_id, status, created_at DESC);
CREATE INDEX orders_customer_idx ON orders(customer_user_id, created_at DESC);

CREATE TABLE order_items (
 id TEXT PRIMARY KEY,
 order_id TEXT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
 shop_id TEXT NOT NULL REFERENCES shops(id),
 shop_product_id TEXT NOT NULL REFERENCES shop_products(id),
 product_name_snapshot TEXT NOT NULL,
 unit_snapshot TEXT NOT NULL,
 unit_price_paise INTEGER NOT NULL CHECK(unit_price_paise >= 0),
 quantity_milli INTEGER NOT NULL CHECK(quantity_milli > 0),
 line_total_paise INTEGER NOT NULL CHECK(line_total_paise >= 0),
 created_at INTEGER NOT NULL
) STRICT;
CREATE INDEX order_items_order_idx ON order_items(order_id);
CREATE INDEX order_items_product_idx ON order_items(shop_product_id);

CREATE TABLE order_status_history (
 id TEXT PRIMARY KEY,
 order_id TEXT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
 status TEXT NOT NULL,
 actor_user_id TEXT REFERENCES users(id),
 note TEXT,
 created_at INTEGER NOT NULL
) STRICT;
CREATE INDEX order_status_history_order_idx ON order_status_history(order_id, created_at);
