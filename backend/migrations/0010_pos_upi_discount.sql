-- 0010_pos_upi_discount.sql — POS payment methods + discounts + product category.
-- Non-breaking additions: all new columns are nullable or defaulted.
ALTER TABLE sales ADD COLUMN payment_method TEXT CHECK(payment_method IN ('CASH','UPI','CREDIT'));
ALTER TABLE sales ADD COLUMN discount_paise INTEGER NOT NULL DEFAULT 0 CHECK(discount_paise >= 0);
ALTER TABLE sale_items ADD COLUMN cost_price_paise INTEGER NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN category TEXT;
