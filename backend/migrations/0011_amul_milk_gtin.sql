PRAGMA foreign_keys = ON;

-- Amul Taaza GTINs (verified: Open Food Facts + EAN registries, valid EAN-13 check digits).
-- 1L Tetra Pak = 8901262150064, 500ml = 8901262150217.
INSERT OR IGNORE INTO users(id,phone_e164,pin_hash,pin_salt,pin_iterations,created_at,updated_at) VALUES('0000seed-0000-4000-8000-000000000000','+919000000000','c2VlZC1wbGFjZWhvbGRlci1ub3QtYS1yZWFsLWhhc2g9PQ==','c2VlZC1zYWx0LXBsYWNlaG9sZGVyPT0=',100000,1790000000,1790000000);

INSERT OR IGNORE INTO products(id,barcode,name,brand,unit,description,created_by_user_id,created_at,updated_at) VALUES('seed-8901262150064','8901262150064','Amul Taaza Toned Milk 1L','Amul','litre',NULL,'0000seed-0000-4000-8000-000000000000',1790000000,1790000000);
INSERT OR IGNORE INTO products(id,barcode,name,brand,unit,description,created_by_user_id,created_at,updated_at) VALUES('seed-8901262150217','8901262150217','Amul Taaza Toned Milk 500ml','Amul','litre',NULL,'0000seed-0000-4000-8000-000000000000',1790000000,1790000000);
