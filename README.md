# LocalDukaan — Cloudflare-native through Phase 3

LocalDukaan is an offline-first, secure, multi-tenant digital operating system for India's neighborhood shops. Phase 1–2C complete the shopkeeper side (auth, shop, catalogue, inventory, barcode, quick sale, customers, Udhari/credit). Phase 3 adds the local marketplace: nearby shop discovery, public shop pages, customer cart and order placement (pickup/delivery), order tracking, and shopkeeper order management. Delivery operations, payments, subscriptions, suppliers, loyalty, referrals, CRM, notifications, analytics and admin are intentionally NOT implemented yet.

## Backend migrations

- `0001_phase1.sql` — PIN identity, sessions, profiles, shops, staff, trial
- `0002_catalogue.sql` — global products, shop products, R2 image metadata, opening inventory
- `0003_inventory_sales.sql` — immutable inventory ledger, sales, sale snapshots, purchase list
- `0004_customers_credit.sql` — shop-scoped customers and immutable credit ledger
- `0005_marketplace.sql` — orders, order items, order status history (shopkeeper-owned fulfilment)

All quantities use integer thousandths. Money uses integer paise. New changes must be added as new migration files; existing migrations are never rewritten.

## Phase 2A/2B/2C Worker routes

- `POST|GET /v1/shops/:shopId/products`, `GET|PUT|DELETE /v1/shops/:shopId/products/:shopProductId`
- `POST|GET /v1/shops/:shopId/products/:shopProductId/image`, `DELETE .../image`
- `POST /v1/products/barcode/lookup`
- `POST /v1/shops/:shopId/products/:shopProductId/stock-in|stock-out`, `GET .../inventory`
- `POST|GET /v1/shops/:shopId/sales`, `GET /v1/shops/:shopId/sales/:saleId`
- `GET /v1/shops/:shopId/low-stock`, `GET|POST /v1/shops/:shopId/purchase-list`, `PUT|DELETE .../purchase-list/:itemId`
- `GET|POST /v1/shops/:shopId/customers`, `GET|PUT|DELETE /v1/shops/:shopId/customers/:id`
- `GET /v1/shops/:shopId/customers/:id/statement`
- `POST /v1/shops/:shopId/customers/:id/credit|payment|adjustment`

All routes authenticate the session and resolve shop membership server-side. Stock changes with relative D1 updates inside atomic batches. Quick Sale derives prices/names from D1 and writes sale, immutable snapshots, stock deltas, inventory entries and an idempotency receipt atomically; a CREDIT sale additionally writes the credit ledger transaction in the same batch. Credit mutations update the canonical balance and append an immutable `credit_transactions` row atomically. SQLite constraints roll the batch back on any violation.

## Phase 3 marketplace Worker routes

- `GET /v1/market/shops` — nearby published shops (optional `latitude`/`longitude`/`radiusM`/`q`)
- `GET /v1/market/shops/:slug` — public shop profile + catalogue
- `POST|GET /v1/addresses` — customer delivery addresses
- `POST|GET /v1/orders` — place and list customer orders
- `GET /v1/orders/:id`, `POST /v1/orders/:id/cancel`
- `GET /v1/shops/:shopId/orders`, `POST /v1/shops/:shopId/orders/:id/status`

Order creation is server-authoritative: prices, minimum-order and delivery-range checks happen server-side, stock is **reserved** atomically (SQLite CHECK constraints roll back the whole batch on oversell), and the same idempotent mutation replays return the original order. Delivery decrements stock and writes immutable `SALE` inventory entries; rejection/cancellation releases the reservation. Order items keep immutable name/unit/price snapshots. Shopkeeper status transitions are validated server-side; customer and shop access remain tenant-isolated.

## Android

Offline-first architecture: Compose UI → ViewModel → Repository → Room → Outbox → WorkManager → Worker → D1/R2.

- **Auth**: mobile + 4/6-digit PIN, Android Keystore AES-GCM token storage, session restoration via refresh rotation; PIN is never persisted locally.
- **Catalogue/inventory**: Room catalogue cache, barcode scanning (CameraX + ZXing Core, no Play Services), stock IN/OUT with local optimistic updates and server reconciliation, low-stock and purchase list.
- **Quick Sale**: continuous scan, quantity aggregation, integer-paise totals, PAID and CREDIT modes; offline sales are queued in the outbox and server-validated (rejected drafts are retained with a visible status).
- **Customers + Udhari (Phase 2C)**: customer list (due/limit/available), customer form, customer profile with statement, Add Udhari / Receive Payment / Adjust +− actions, and a payment-mode selector with customer picker inside Quick Sale. All customer/credit mutations are optimistic locally, queued in the outbox, and reconciled to server truth (canonical balance + `SYNCED`/`REJECTED` status); local placeholder customer IDs are remapped to server IDs on create.
- **Marketplace (Phase 3)**: nearby shop discovery, public shop page with catalogue and cart, pickup/delivery checkout with saved addresses, customer order list/detail with cancellation, and a shopkeeper order queue with server-validated status transitions (`OrderStatusFlow`). Orders are server-authoritative and fetched live rather than queued offline.
- **Localization**: user-facing strings live in Android string resources with English (`values/`) and Hindi (`values-hi/`) variants.
- Room schema version 4 with incremental 1→2→3→4 migrations.

## Verification

```bash
cd backend && npm run check
cd backend && npx wrangler d1 migrations apply localdukaan --local
cd android && ./gradlew clean
cd android && ./gradlew testDebugUnitTest
cd android && ./gradlew assembleDebug
```

### Current local baseline

- Backend: **41/41** tests passing (`vitest`)
- Android: **28/28** unit tests passing
- Clean Android build + `assembleDebug`: passing
- Debug APK: `artifacts/LocalDukaan-phase2c-debug.apk`

Remote Cloudflare and physical-device verification require external credentials/hardware and must not be inferred from local tests.

## Phase 1 production configuration and verification readiness

### Required `PIN_PEPPER` secret

`PIN_PEPPER` is mandatory for PIN hashing in the Worker. Production must configure it as a **Cloudflare Worker secret**, for example from an authenticated deployment environment:

```bash
cd backend
npx wrangler secret put PIN_PEPPER
```

Enter a high-entropy production value only into Wrangler's secure prompt or an approved CI secret channel. The production value must **never** be committed to Git, placed in `wrangler.toml`, included in Android, returned by an API, or printed in source, tests, logs, documentation, APKs, exceptions, or error messages. Local development and automated tests must use a separate non-production test secret. Do not use a production secret in local tests.

### D1 production database ID

`backend/wrangler.toml` intentionally contains:

```toml
database_id = "REPLACE_WITH_D1_DATABASE_ID"
```

An authenticated operator must create or identify the real production D1 database and replace that placeholder (or supply an environment-specific Wrangler configuration managed by the deployment system) before production deployment. Never invent an ID. Keeping production IDs and non-secret binding metadata in a deployment-specific Wrangler file is acceptable; secrets must still use Wrangler/CI secret storage.

### Production deployment checklist

**LOCAL VERIFIED** means local type-checks, tests, migrations, and APK assembly completed. It does not prove production deployment or on-device behavior.

**PRODUCTION REQUIRES CREDENTIALS/DEPLOYMENT:**

1. Authenticate Wrangler with the intended Cloudflare account.
2. Create/identify D1, configure the `DB` binding with its real ID, and apply migrations in order with `wrangler d1 migrations apply localdukaan --remote` after reviewing the target.
3. Create/verify the R2 bucket and `MEDIA` binding required by the current Worker configuration.
4. Create/verify the queue and `NOTIFICATION_QUEUE` producer binding required by the current Worker configuration.
5. Configure `PIN_PEPPER` with `wrangler secret put PIN_PEPPER`; never put its value in config or logs.
6. Deploy the Worker and record its actual Cloudflare/custom-domain HTTPS URL. Do not substitute an assumed URL.
7. Configure Android `LOCALDUKAAN_API_URL` to that production `https://` URL at build time. The default is `https://api.localdukaan.in`; it is only valid if that domain is actually provisioned. Android explicitly disables cleartext traffic.
8. Run post-deployment smoke tests: register with 4- and 6-digit PINs, reject invalid/wrong PINs, login, authenticated profile, refresh rotation and reuse rejection, logout/revocation, shop creation/trial, unauthenticated rejection, and cross-tenant rejection.
9. On a real device/emulator, verify register → login → close/reopen → session restoration → logout → login again, plus catalogue/inventory/barcode/quick-sale/credit flows.

## Verification status

- **LOCAL VERIFIED**: backend type-check, backend tests, Android unit tests, migrations, clean build, APK assembly.
- **DEVICE VERIFICATION: BLOCKED** — no Android device/emulator run was performed; the real lifecycle remains unverified.
- **PRODUCTION VERIFICATION: BLOCKED** — no Cloudflare credentials/resources; production D1 ID, bindings, `PIN_PEPPER`, deployed URL and smoke tests remain unverified.
