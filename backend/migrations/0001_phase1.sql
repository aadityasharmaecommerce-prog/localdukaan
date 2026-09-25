PRAGMA foreign_keys = ON;
CREATE TABLE users (
 id TEXT PRIMARY KEY, phone_e164 TEXT NOT NULL UNIQUE, pin_hash TEXT NOT NULL, pin_salt TEXT NOT NULL, pin_iterations INTEGER NOT NULL CHECK(pin_iterations>=100000),
 status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','SUSPENDED','DELETED')), failed_login_count INTEGER NOT NULL DEFAULT 0, locked_until INTEGER,
 created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
) STRICT;
CREATE TABLE profiles (
 user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE, display_name TEXT NOT NULL DEFAULT '', preferred_language TEXT NOT NULL DEFAULT 'en' CHECK(preferred_language IN ('en','hi')),
 is_customer INTEGER NOT NULL DEFAULT 0 CHECK(is_customer IN (0,1)), is_shopkeeper INTEGER NOT NULL DEFAULT 0 CHECK(is_shopkeeper IN (0,1)), version INTEGER NOT NULL DEFAULT 1, updated_at INTEGER NOT NULL
) STRICT;
CREATE TABLE auth_rate_limits (key TEXT NOT NULL, window_start INTEGER NOT NULL, count INTEGER NOT NULL, PRIMARY KEY(key,window_start)) STRICT;
CREATE TABLE sessions (
 id TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, access_hash TEXT NOT NULL UNIQUE, refresh_hash TEXT NOT NULL UNIQUE,
 access_expires_at INTEGER NOT NULL, refresh_expires_at INTEGER NOT NULL, revoked_at INTEGER, device_name TEXT, created_at INTEGER NOT NULL, last_used_at INTEGER NOT NULL
) STRICT;
CREATE INDEX sessions_user_idx ON sessions(user_id,refresh_expires_at);
CREATE TABLE shops (
 id TEXT PRIMARY KEY, name TEXT NOT NULL, owner_name TEXT NOT NULL, address_line TEXT NOT NULL, locality TEXT NOT NULL, city TEXT NOT NULL, pincode TEXT NOT NULL,
 category TEXT NOT NULL, latitude REAL NOT NULL CHECK(latitude BETWEEN -90 AND 90), longitude REAL NOT NULL CHECK(longitude BETWEEN -180 AND 180),
 description TEXT, opens_at TEXT, closes_at TEXT, status TEXT NOT NULL DEFAULT 'PENDING_APPROVAL' CHECK(status IN ('DRAFT','PENDING_APPROVAL','ACTIVE','SUSPENDED','CLOSED')),
 is_published INTEGER NOT NULL DEFAULT 0 CHECK(is_published IN(0,1)), accepts_online_orders INTEGER NOT NULL DEFAULT 1 CHECK(accepts_online_orders IN(0,1)),
 version INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
) STRICT;
CREATE INDEX shops_geo_idx ON shops(latitude,longitude,status,is_published);
CREATE TABLE shop_staff (
 shop_id TEXT NOT NULL REFERENCES shops(id) ON DELETE CASCADE, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 role TEXT NOT NULL CHECK(role IN ('OWNER','MANAGER','STAFF')), is_active INTEGER NOT NULL DEFAULT 1 CHECK(is_active IN(0,1)), created_at INTEGER NOT NULL,
 PRIMARY KEY(shop_id,user_id)
) STRICT;
CREATE INDEX shop_staff_user_idx ON shop_staff(user_id,is_active);
CREATE TABLE shop_delivery_settings (
 shop_id TEXT PRIMARY KEY REFERENCES shops(id) ON DELETE CASCADE, delivery_enabled INTEGER NOT NULL DEFAULT 0 CHECK(delivery_enabled IN(0,1)),
 radius_m INTEGER NOT NULL DEFAULT 1000 CHECK(radius_m BETWEEN 100 AND 20000), minimum_order_paise INTEGER NOT NULL DEFAULT 0 CHECK(minimum_order_paise>=0),
 delivery_fee_paise INTEGER NOT NULL DEFAULT 0 CHECK(delivery_fee_paise>=0), free_delivery_threshold_paise INTEGER CHECK(free_delivery_threshold_paise>=0),
 estimated_minutes INTEGER NOT NULL DEFAULT 60 CHECK(estimated_minutes BETWEEN 5 AND 1440), version INTEGER NOT NULL DEFAULT 1, updated_at INTEGER NOT NULL
) STRICT;
CREATE TABLE customers (
 id TEXT PRIMARY KEY, user_id TEXT UNIQUE REFERENCES users(id) ON DELETE SET NULL, name TEXT NOT NULL, mobile_e164 TEXT, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
) STRICT;
CREATE TABLE addresses (
 id TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, label TEXT, recipient_name TEXT NOT NULL, mobile_e164 TEXT,
 address_line TEXT NOT NULL, locality TEXT NOT NULL, city TEXT NOT NULL, pincode TEXT NOT NULL, latitude REAL, longitude REAL,
 is_default INTEGER NOT NULL DEFAULT 0 CHECK(is_default IN(0,1)), version INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
) STRICT;
CREATE INDEX addresses_user_idx ON addresses(user_id);
CREATE TABLE subscriptions (
 id TEXT PRIMARY KEY, shop_id TEXT NOT NULL UNIQUE REFERENCES shops(id) ON DELETE CASCADE, status TEXT NOT NULL CHECK(status IN ('TRIAL','ACTIVE','EXPIRED','CANCELLED')),
 trial_started_at INTEGER, trial_ends_at INTEGER, current_period_start INTEGER, current_period_end INTEGER, provider TEXT, provider_product_id TEXT,
 purchase_token_hash TEXT, verified_at INTEGER, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
) STRICT;
CREATE TABLE sync_operations (
 user_id TEXT NOT NULL REFERENCES users(id), mutation_id TEXT NOT NULL, operation TEXT NOT NULL, shop_id TEXT, request_hash TEXT NOT NULL,
 response_json TEXT, created_at INTEGER NOT NULL, PRIMARY KEY(user_id,mutation_id)
) STRICT;
CREATE TABLE audit_logs (
 id TEXT PRIMARY KEY, actor_user_id TEXT, action TEXT NOT NULL, resource_type TEXT NOT NULL, resource_id TEXT, shop_id TEXT, metadata_json TEXT NOT NULL DEFAULT '{}', created_at INTEGER NOT NULL
) STRICT;
