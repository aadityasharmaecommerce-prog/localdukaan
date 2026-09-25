-- ONE OWNER -> ONE SHOP (business rule: ek user sirf ek dukaan OWN kar sakta hai)
-- Ye partial unique index database level par enforce karta hai, chahe app code miss ho jaye.
-- Concurrent CREATE_SHOP requests me batch constraint violation se roll back hoga.
-- NOTE: Staff (MANAGER/STAFF) memberships isse affected NAHI hote — sirf OWNER role unique hai.
CREATE UNIQUE INDEX IF NOT EXISTS shop_staff_single_owner_active
  ON shop_staff(user_id) WHERE role='OWNER' AND is_active=1;
