PRAGMA foreign_keys = ON;

-- Global product images without R2: store the Open Food/Beauty/Products Facts image URL.
-- The worker streams bytes from this URL via /v1/global-products/{barcode}/image.
ALTER TABLE products ADD COLUMN remote_image_url TEXT;
