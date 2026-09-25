var __defProp = Object.defineProperty;
var __name = (target, value) => __defProp(target, "name", { value, configurable: true });

// src/index.ts
var ApiError = class extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
  static {
    __name(this, "ApiError");
  }
};
var json = /* @__PURE__ */ __name((body2, status = 200) => new Response(JSON.stringify(body2), { status, headers: { "content-type": "application/json", "cache-control": "no-store" } }), "json");
var now = /* @__PURE__ */ __name(() => Math.floor(Date.now() / 1e3), "now");
var id = /* @__PURE__ */ __name(() => crypto.randomUUID(), "id");
var enc = new TextEncoder();
async function hash(s) {
  const b = await crypto.subtle.digest("SHA-256", enc.encode(s));
  return [...new Uint8Array(b)].map((x) => x.toString(16).padStart(2, "0")).join("");
}
__name(hash, "hash");
var PBKDF2_MAX_ITERATIONS = 1e5;
function pinIterations(env) {
  const n = Math.floor(Number(env.PIN_ITERATIONS));
  return Number.isFinite(n) && n >= 1e3 ? Math.min(n, PBKDF2_MAX_ITERATIONS) : PBKDF2_MAX_ITERATIONS;
}
__name(pinIterations, "pinIterations");
function phone(v) {
  const s = String(v ?? "").replace(/[\s-]/g, "");
  if (!/^\+[1-9]\d{7,14}$/.test(s)) throw new ApiError(400, "INVALID_PHONE", "Use E.164 phone format");
  return s;
}
__name(phone, "phone");
async function body(req) {
  try {
    return await req.json();
  } catch {
    throw new ApiError(400, "INVALID_JSON", "Expected JSON body");
  }
}
__name(body, "body");
function str(v, name, min = 1, max = 200) {
  if (typeof v !== "string" || v.trim().length < min || v.trim().length > max) throw new ApiError(400, "VALIDATION_ERROR", `Invalid ${name}`);
  return v.trim();
}
__name(str, "str");
async function bearer(req, env) {
  const h = req.headers.get("authorization");
  if (!h?.startsWith("Bearer ")) throw new ApiError(401, "UNAUTHENTICATED", "Authentication required");
  const token = h.slice(7), tokenHash = await hash(token), t = now();
  const row = await env.DB.prepare(`SELECT s.id session_id,s.user_id FROM sessions s JOIN users u ON u.id=s.user_id WHERE s.access_hash=? AND s.revoked_at IS NULL AND s.access_expires_at>? AND u.status='ACTIVE'`).bind(tokenHash, t).first();
  if (!row) throw new ApiError(401, "INVALID_SESSION", "Session expired or invalid");
  env.DB.prepare("UPDATE sessions SET last_used_at=? WHERE id=?").bind(t, row.session_id).run().catch(() => {
  });
  return { userId: row.user_id, sessionId: row.session_id };
}
__name(bearer, "bearer");
async function membership(env, userId, shopId, roles) {
  const m = await env.DB.prepare("SELECT role FROM shop_staff WHERE shop_id=? AND user_id=? AND is_active=1").bind(shopId, userId).first();
  if (!m || roles && !roles.includes(m.role)) throw new ApiError(403, "FORBIDDEN", "You do not have permission for this shop");
  return m;
}
__name(membership, "membership");
async function rateLimit(env, key, limit, windowSeconds) {
  const w = Math.floor(now() / windowSeconds) * windowSeconds;
  await env.DB.prepare(`INSERT INTO auth_rate_limits(key,window_start,count) VALUES(?,?,1) ON CONFLICT(key,window_start) DO UPDATE SET count=count+1`).bind(key, w).run();
  const r = await env.DB.prepare("SELECT count FROM auth_rate_limits WHERE key=? AND window_start=?").bind(key, w).first();
  if ((r?.count ?? 0) > limit) throw new ApiError(429, "RATE_LIMITED", "Try again later");
}
__name(rateLimit, "rateLimit");
function pin(v) {
  const p = String(v ?? "");
  if (!/^(?:\d{4}|\d{6})$/.test(p)) throw new ApiError(400, "INVALID_PIN_FORMAT", "PIN must contain exactly 4 or 6 digits");
  return p;
}
__name(pin, "pin");
function b64(bytes) {
  let x = "";
  for (const v of bytes) x += String.fromCharCode(v);
  return btoa(x);
}
__name(b64, "b64");
function unb64(s) {
  return Uint8Array.from(atob(s), (c) => c.charCodeAt(0));
}
__name(unb64, "unb64");
async function derivePin(pinValue, salt, pepper, iterations) {
  const material = await crypto.subtle.importKey("raw", enc.encode(`${pinValue}:${pepper}`), "PBKDF2", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits({ name: "PBKDF2", hash: "SHA-256", salt: salt.buffer, iterations }, material, 256);
  return b64(new Uint8Array(bits));
}
__name(derivePin, "derivePin");
function safeEqual(a, b) {
  if (a.length !== b.length) return false;
  let d = 0;
  for (let i = 0; i < a.length; i++) d |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return d === 0;
}
__name(safeEqual, "safeEqual");
function distanceM(lat1, lng1, lat2, lng2) {
  const R = 6371e3, rad = /* @__PURE__ */ __name((d) => d * Math.PI / 180, "rad"), dLat = rad(lat2 - lat1), dLng = rad(lng2 - lng1);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(a)));
}
__name(distanceM, "distanceM");
var orderTransitions = { PENDING: ["ACCEPTED", "REJECTED", "CANCELLED"], ACCEPTED: ["PACKING", "CANCELLED"], PACKING: ["READY", "CANCELLED"], READY: ["OUT_FOR_DELIVERY", "DELIVERED"], OUT_FOR_DELIVERY: ["DELIVERED"], DELIVERED: [], REJECTED: [], CANCELLED: [] };
async function issueSession(env, userId, device) {
  const access = `ld_a_${id()}${id()}`, refresh2 = `ld_r_${id()}${id()}`, t = now(), sid = id();
  await env.DB.prepare("INSERT INTO sessions(id,user_id,access_hash,refresh_hash,access_expires_at,refresh_expires_at,device_name,created_at,last_used_at) VALUES(?,?,?,?,?,?,?,?,?)").bind(sid, userId, await hash(access), await hash(refresh2), t + Number(env.ACCESS_TTL_SECONDS || 900), t + Number(env.REFRESH_TTL_SECONDS || 2592e3), device?.slice(0, 100) ?? null, t, t).run();
  return { accessToken: access, refreshToken: refresh2, accessExpiresAt: t + Number(env.ACCESS_TTL_SECONDS || 900) };
}
__name(issueSession, "issueSession");
async function register(req, env) {
  const b = await body(req), p = phone(b.phone), secret = pin(b.pin), confirm = pin(b.confirmPin);
  if (secret !== confirm) throw new ApiError(400, "PIN_MISMATCH", "PIN confirmation does not match");
  const ip = req.headers.get("cf-connecting-ip") ?? "unknown";
  await rateLimit(env, `register-ip:${await hash(ip)}`, 10, 3600);
  const existing = await env.DB.prepare("SELECT id FROM users WHERE phone_e164=?").bind(p).first();
  if (existing) throw new ApiError(409, "MOBILE_ALREADY_REGISTERED", "Mobile number is already registered");
  const salt = crypto.getRandomValues(new Uint8Array(16)), iterations = pinIterations(env), uid = id(), t = now(), customer = b.role === "CUSTOMER" ? 1 : 0, shopkeeper = b.role === "SHOPKEEPER" ? 1 : 0;
  if (!customer && !shopkeeper) throw new ApiError(400, "INVALID_ROLE", "Choose CUSTOMER or SHOPKEEPER");
  const pinHash = await derivePin(secret, salt, env.PIN_PEPPER, iterations);
  await env.DB.batch([env.DB.prepare("INSERT INTO users(id,phone_e164,pin_hash,pin_salt,pin_iterations,created_at,updated_at) VALUES(?,?,?,?,?,?,?)").bind(uid, p, pinHash, b64(salt), iterations, t, t), env.DB.prepare("INSERT INTO profiles(user_id,is_customer,is_shopkeeper,updated_at) VALUES(?,?,?,?)").bind(uid, customer, shopkeeper, t)]);
  return json({ userId: uid, ...await issueSession(env, uid, typeof b.deviceName === "string" ? b.deviceName : void 0) }, 201);
}
__name(register, "register");
async function login(req, env) {
  const b = await body(req), p = phone(b.phone), secret = pin(b.pin), ip = req.headers.get("cf-connecting-ip") ?? "unknown";
  await rateLimit(env, `login-ip:${await hash(ip)}`, 40, 900);
  const u = await env.DB.prepare("SELECT id,pin_hash,pin_salt,pin_iterations,status,failed_login_count,locked_until FROM users WHERE phone_e164=?").bind(p).first();
  const t = now();
  if (u?.locked_until && u.locked_until > t) throw new ApiError(429, "ACCOUNT_TEMPORARILY_LOCKED", `Try again after ${u.locked_until}`);
  let candidate = null;
  try {
    candidate = u ? await derivePin(secret, unb64(u.pin_salt), env.PIN_PEPPER, u.pin_iterations) : await derivePin(secret, new Uint8Array(16), env.PIN_PEPPER, pinIterations(env));
  } catch {
    candidate = null;
  }
  if (!u || u.status !== "ACTIVE" || !candidate || !safeEqual(candidate, u.pin_hash)) {
    if (u) {
      const failures = u.failed_login_count + 1, locked = failures >= 5 ? t + Math.min(3600, 300 * Math.pow(2, Math.floor((failures - 5) / 2))) : null;
      await env.DB.prepare("UPDATE users SET failed_login_count=?,locked_until=?,updated_at=? WHERE id=?").bind(failures, locked, t, u.id).run();
    }
    throw new ApiError(401, "INVALID_CREDENTIALS", "Mobile number or PIN is incorrect");
  }
  await env.DB.prepare("UPDATE users SET failed_login_count=0,locked_until=NULL,updated_at=? WHERE id=?").bind(t, u.id).run();
  return json({ userId: u.id, ...await issueSession(env, u.id, typeof b.deviceName === "string" ? b.deviceName : void 0) });
}
__name(login, "login");
async function refresh(req, env) {
  const b = await body(req), r = str(b.refreshToken, "refreshToken", 20, 200), h = await hash(r), t = now();
  const s = await env.DB.prepare(`SELECT s.id,s.user_id FROM sessions s JOIN users u ON u.id=s.user_id WHERE refresh_hash=? AND revoked_at IS NULL AND refresh_expires_at>? AND u.status='ACTIVE'`).bind(h, t).first();
  if (!s) throw new ApiError(401, "INVALID_REFRESH", "Refresh token invalid");
  const changed = await env.DB.prepare("UPDATE sessions SET revoked_at=? WHERE id=? AND revoked_at IS NULL").bind(t, s.id).run();
  if (!changed.meta.changes) throw new ApiError(401, "INVALID_REFRESH", "Refresh token invalid");
  return json(await issueSession(env, s.user_id));
}
__name(refresh, "refresh");
async function profile(req, env, a) {
  if (req.method === "GET") {
    const p = await env.DB.prepare("SELECT user_id,display_name,preferred_language,is_customer,is_shopkeeper,version FROM profiles WHERE user_id=?").bind(a.userId).first();
    return json(p);
  }
  const b = await body(req), name = str(b.displayName, "displayName", 1, 100), lang = b.preferredLanguage === "hi" ? "hi" : "en", customer = b.isCustomer === true ? 1 : 0, shopkeeper = b.isShopkeeper === true ? 1 : 0, t = now();
  await env.DB.prepare("UPDATE profiles SET display_name=?,preferred_language=?,is_customer=MAX(is_customer,?),is_shopkeeper=MAX(is_shopkeeper,?),version=version+1,updated_at=? WHERE user_id=?").bind(name, lang, customer, shopkeeper, t, a.userId).run();
  if (customer) await env.DB.prepare(`INSERT INTO customers(id,user_id,name,mobile_e164,created_at,updated_at) SELECT ?,u.id,?,u.phone_e164,?,? FROM users u WHERE u.id=? ON CONFLICT(user_id) DO UPDATE SET name=excluded.name,updated_at=excluded.updated_at`).bind(id(), name, t, t, a.userId).run();
  return profile(new Request(req.url), env, a);
}
__name(profile, "profile");
async function createShop(req, env, a) {
  const b = await body(req), mutation = str(b.mutationId, "mutationId", 36, 36), requestHash = await hash(JSON.stringify(b));
  const old = await env.DB.prepare("SELECT response_json,request_hash FROM sync_operations WHERE user_id=? AND mutation_id=?").bind(a.userId, mutation).first();
  if (old) {
    if (old.request_hash !== requestHash) throw new ApiError(409, "IDEMPOTENCY_MISMATCH", "Mutation ID was already used");
    return json(JSON.parse(old.response_json), 200);
  }
  const sid = id(), sub = id(), t = now(), lat = Number(b.latitude), lng = Number(b.longitude);
  if (!Number.isFinite(lat) || lat < -90 || lat > 90 || !Number.isFinite(lng) || lng < -180 || lng > 180) throw new ApiError(400, "VALIDATION_ERROR", "Invalid location");
  const pincode = str(b.pincode, "pincode", 6, 6);
  if (!/^[1-9]\d{5}$/.test(pincode)) throw new ApiError(400, "VALIDATION_ERROR", "Invalid pincode");
  const out = { id: sid, status: "ACTIVE", subscription: { status: "TRIAL", trialEndsAt: t + 2592e3 } };
  await env.DB.batch([env.DB.prepare(`INSERT INTO shops(id,name,owner_name,address_line,locality,city,pincode,category,latitude,longitude,description,opens_at,closes_at,status,public_slug,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,'ACTIVE',?,?,?)`).bind(sid, str(b.name, "name", 2, 120), str(b.ownerName, "ownerName", 2, 100), str(b.addressLine, "addressLine", 3, 300), str(b.locality, "locality", 2, 100), str(b.city, "city", 2, 100), pincode, str(b.category, "category", 2, 80), lat, lng, typeof b.description === "string" ? b.description.slice(0, 500) : null, typeof b.opensAt === "string" ? b.opensAt : null, typeof b.closesAt === "string" ? b.closesAt : null, `shop-${sid.replaceAll("-", "").slice(0, 16)}`, t, t), env.DB.prepare("INSERT INTO shop_staff(shop_id,user_id,role,created_at) VALUES(?,?,?,?)").bind(sid, a.userId, "OWNER", t), env.DB.prepare("INSERT INTO shop_delivery_settings(shop_id,updated_at) VALUES(?,?)").bind(sid, t), env.DB.prepare(`INSERT INTO subscriptions(id,shop_id,status,trial_started_at,trial_ends_at,created_at,updated_at) VALUES(?,?, 'TRIAL',?,?,?,?)`).bind(sub, sid, t, t + 2592e3, t, t), env.DB.prepare("UPDATE profiles SET is_shopkeeper=1,version=version+1,updated_at=? WHERE user_id=?").bind(t, a.userId), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "CREATE_SHOP", sid, requestHash, JSON.stringify(out), t)]);
  return json(out, 201);
}
__name(createShop, "createShop");
async function myShops(env, a) {
  const r = await env.DB.prepare(`SELECT s.id,s.name,s.status,s.city,s.locality,ss.role,sub.status subscription_status,sub.trial_ends_at FROM shops s JOIN shop_staff ss ON ss.shop_id=s.id LEFT JOIN subscriptions sub ON sub.shop_id=s.id WHERE ss.user_id=? AND ss.is_active=1 ORDER BY s.created_at`).bind(a.userId).all();
  return json({ items: r.results });
}
__name(myShops, "myShops");
async function privateShop(env, a, shopId) {
  await membership(env, a.userId, shopId);
  const s = await env.DB.prepare(`SELECT s.*,d.delivery_enabled,d.radius_m,d.minimum_order_paise,d.delivery_fee_paise,d.free_delivery_threshold_paise,d.estimated_minutes FROM shops s JOIN shop_delivery_settings d ON d.shop_id=s.id WHERE s.id=?`).bind(shopId).first();
  if (!s) throw new ApiError(404, "NOT_FOUND", "Shop not found");
  return json(s);
}
__name(privateShop, "privateShop");
function uuid(v, name = "id") {
  const x = String(v ?? "");
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(x)) throw new ApiError(400, "VALIDATION_ERROR", `Invalid ${name}`);
  return x;
}
__name(uuid, "uuid");
function int(v, name, min = 0, max = 2e9) {
  if (typeof v !== "number" || !Number.isSafeInteger(v) || v < min || v > max) throw new ApiError(400, "VALIDATION_ERROR", `Invalid ${name}`);
  return v;
}
__name(int, "int");
function optional(v, max) {
  if (v == null || v === "") return null;
  if (typeof v !== "string" || v.trim().length > max) throw new ApiError(400, "VALIDATION_ERROR", "Invalid optional text");
  return v.trim();
}
__name(optional, "optional");
function barcode(v) {
  if (v == null || v === "") return null;
  const x = String(v).replace(/[\s-]/g, "").toUpperCase();
  if (!/^[0-9A-Z]{6,32}$/.test(x)) throw new ApiError(400, "INVALID_BARCODE", "Barcode must contain 6\u201332 letters or digits");
  return x;
}
__name(barcode, "barcode");
async function receipt(env, a, mutation, requestHash) {
  const old = await env.DB.prepare("SELECT response_json,request_hash FROM sync_operations WHERE user_id=? AND mutation_id=?").bind(a.userId, mutation).first();
  if (old && old.request_hash !== requestHash) throw new ApiError(409, "IDEMPOTENCY_MISMATCH", "Mutation ID was already used");
  return old?.response_json ? JSON.parse(old.response_json) : null;
}
__name(receipt, "receipt");
function productSelect(where) {
  return `SELECT sp.id shop_product_id,sp.shop_id,sp.product_id,p.barcode,p.name,p.brand,p.unit,p.description,sp.selling_price_paise,sp.stock_quantity_milli,sp.reserved_quantity_milli,sp.minimum_stock_milli,sp.is_available,sp.is_archived,sp.image_key,sp.image_mime_type,sp.image_size_bytes,sp.version,sp.updated_at,CASE WHEN sp.stock_quantity_milli<=sp.minimum_stock_milli THEN 1 ELSE 0 END is_low_stock FROM shop_products sp JOIN products p ON p.id=sp.product_id WHERE ${where}`;
}
__name(productSelect, "productSelect");
async function listProducts(req, env, a, shopId) {
  await membership(env, a.userId, shopId);
  const u = new URL(req.url), q = (u.searchParams.get("q") ?? "").trim(), includeArchived = u.searchParams.get("includeArchived") === "true";
  const r = await env.DB.prepare(productSelect(`sp.shop_id=? AND (?=1 OR sp.is_archived=0) AND (?='' OR p.name LIKE ? OR COALESCE(p.brand,'') LIKE ? OR COALESCE(p.barcode,'') LIKE ?) ORDER BY p.name LIMIT 200`)).bind(shopId, includeArchived ? 1 : 0, q, `%${q}%`, `%${q}%`, `%${q}%`).all();
  return json({ items: r.results });
}
__name(listProducts, "listProducts");
async function getProduct(env, a, shopId, shopProductId) {
  await membership(env, a.userId, shopId);
  const x = await env.DB.prepare(productSelect("sp.shop_id=? AND sp.id=?")).bind(shopId, shopProductId).first();
  if (!x) throw new ApiError(404, "NOT_FOUND", "Product not found");
  return json(x);
}
__name(getProduct, "getProduct");
async function createProduct(req, env, a, shopId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER", "STAFF"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  let name = str(b.name, "name", 1, 160), brand = optional(b.brand, 100), code = barcode(b.barcode), unit = str(b.unit, "unit", 1, 40), description = optional(b.description, 1e3);
  const price = int(b.sellingPricePaise, "sellingPricePaise"), opening = int(b.openingStockMilli, "openingStockMilli"), minimum = int(b.minimumStockMilli, "minimumStockMilli"), available = b.isAvailable === false ? 0 : 1, t = now();
  let productId = typeof b.productId === "string" ? uuid(b.productId, "productId") : null;
  if (productId) {
    if (!await env.DB.prepare("SELECT id FROM products WHERE id=?").bind(productId).first()) throw new ApiError(404, "GLOBAL_PRODUCT_NOT_FOUND", "Global product not found");
  } else if (code) {
    productId = (await env.DB.prepare("SELECT id FROM products WHERE barcode=?").bind(code).first())?.id ?? null;
  }
  if (productId) {
    const canonical = await env.DB.prepare("SELECT name,brand,barcode,unit,description FROM products WHERE id=?").bind(productId).first();
    name = canonical.name;
    brand = canonical.brand;
    code = canonical.barcode;
    unit = canonical.unit;
    description = canonical.description;
  }
  if (productId && await env.DB.prepare("SELECT id FROM shop_products WHERE shop_id=? AND product_id=?").bind(shopId, productId).first()) throw new ApiError(409, "SHOP_PRODUCT_EXISTS", "This product is already attached to the shop");
  const globalId = productId ?? id(), shopProductId = id(), inventoryId = id();
  const result = { shopProductId, shopId, productId: globalId, name, brand, barcode: code, unit, description, sellingPricePaise: price, stockQuantityMilli: opening, reservedQuantityMilli: 0, minimumStockMilli: minimum, isAvailable: available === 1, isArchived: false, version: 1, inventoryTransactionId: opening > 0 ? inventoryId : null };
  const statements = [];
  if (!productId) statements.push(env.DB.prepare("INSERT INTO products(id,barcode,name,brand,unit,description,created_by_user_id,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)").bind(globalId, code, name, brand, unit, description, a.userId, t, t));
  statements.push(env.DB.prepare("INSERT INTO shop_products(id,shop_id,product_id,selling_price_paise,stock_quantity_milli,minimum_stock_milli,is_available,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)").bind(shopProductId, shopId, globalId, price, opening, minimum, available, t, t));
  if (opening > 0) statements.push(env.DB.prepare(`INSERT INTO inventory_transactions(id,shop_id,shop_product_id,type,quantity_delta_milli,stock_after_milli,mutation_id,actor_user_id,created_at) VALUES(?,?,?,'OPENING_STOCK',?,?,?,?,?)`).bind(inventoryId, shopId, shopProductId, opening, opening, mutation, a.userId, t));
  statements.push(env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "CREATE_PRODUCT", shopId, requestHash, JSON.stringify(result), t));
  try {
    await env.DB.batch(statements);
  } catch (e) {
    throw new ApiError(409, "PRODUCT_CONFLICT", "Barcode or shop product already exists");
  }
  return json(result, 201);
}
__name(createProduct, "createProduct");
async function updateProduct(req, env, a, shopId, shopProductId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const current = await env.DB.prepare(`SELECT sp.*,p.name,p.brand,p.barcode,p.unit,p.description,p.created_by_user_id,(SELECT count(*) FROM shop_products x WHERE x.product_id=sp.product_id) attachment_count FROM shop_products sp JOIN products p ON p.id=sp.product_id WHERE sp.id=? AND sp.shop_id=?`).bind(shopProductId, shopId).first();
  if (!current) throw new ApiError(404, "NOT_FOUND", "Product not found");
  const version = int(b.version, "version", 1), price = int(b.sellingPricePaise, "sellingPricePaise"), minimum = int(b.minimumStockMilli, "minimumStockMilli"), available = b.isAvailable === false ? 0 : 1, name = str(b.name, "name", 1, 160), brand = optional(b.brand, 100), code = barcode(b.barcode), unit = str(b.unit, "unit", 1, 40), description = optional(b.description, 1e3);
  if (current.version !== version) throw new ApiError(409, "VERSION_CONFLICT", "Product has changed; refresh before editing");
  const globalChanged = name !== current.name || brand !== current.brand || code !== current.barcode || unit !== current.unit || description !== current.description;
  if (globalChanged && (current.created_by_user_id !== a.userId || current.attachment_count > 1)) throw new ApiError(403, "GLOBAL_PRODUCT_LOCKED", "Shared global product identity cannot be edited");
  if (code && await env.DB.prepare("SELECT id FROM products WHERE barcode=? AND id<>?").bind(code, current.product_id).first()) throw new ApiError(409, "BARCODE_EXISTS", "Barcode already exists");
  const t = now(), result = { shopProductId, shopId, productId: current.product_id, name, brand, barcode: code, unit, description, sellingPricePaise: price, stockQuantityMilli: current.stock_quantity_milli, reservedQuantityMilli: current.reserved_quantity_milli, minimumStockMilli: minimum, isAvailable: available === 1, isArchived: current.is_archived === 1, version: version + 1 };
  const statements = [env.DB.prepare("UPDATE shop_products SET selling_price_paise=?,minimum_stock_milli=?,is_available=?,version=version+1,updated_at=? WHERE id=? AND shop_id=? AND version=?").bind(price, minimum, available, t, shopProductId, shopId, version)];
  if (globalChanged) statements.push(env.DB.prepare("UPDATE products SET barcode=?,name=?,brand=?,unit=?,description=?,version=version+1,updated_at=? WHERE id=?").bind(code, name, brand, unit, description, t, current.product_id));
  statements.push(env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "UPDATE_PRODUCT", shopId, requestHash, JSON.stringify(result), t));
  await env.DB.batch(statements);
  return json(result);
}
__name(updateProduct, "updateProduct");
async function archiveProduct(req, env, a, shopId, shopProductId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const current = await env.DB.prepare("SELECT version FROM shop_products WHERE id=? AND shop_id=?").bind(shopProductId, shopId).first();
  if (!current) throw new ApiError(404, "NOT_FOUND", "Product not found");
  const result = { shopProductId, isArchived: true, isAvailable: false, version: current.version + 1 }, t = now();
  await env.DB.batch([env.DB.prepare("UPDATE shop_products SET is_archived=1,is_available=0,version=version+1,updated_at=? WHERE id=? AND shop_id=?").bind(t, shopProductId, shopId), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "ARCHIVE_PRODUCT", shopId, requestHash, JSON.stringify(result), t)]);
  return json(result);
}
__name(archiveProduct, "archiveProduct");
async function lookupBarcode(req, env, a) {
  void a;
  const b = await body(req), code = barcode(b.barcode);
  if (!code) throw new ApiError(400, "INVALID_BARCODE", "Barcode is required");
  const p = await env.DB.prepare("SELECT id,barcode,name,brand,unit,description,version FROM products WHERE barcode=?").bind(code).first();
  return p ? json({ found: true, product: p }) : json({ found: false, barcode: code });
}
__name(lookupBarcode, "lookupBarcode");
async function updateShopProfile(req, env, a, shopId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const version = int(b.version, "version", 1), current = await env.DB.prepare("SELECT version,public_slug FROM shops WHERE id=?").bind(shopId).first();
  if (!current) throw new ApiError(404, "NOT_FOUND", "Shop not found");
  if (current.version !== version) throw new ApiError(409, "VERSION_CONFLICT", "Shop has changed; refresh before editing");
  const pincode = str(b.pincode, "pincode", 6, 6);
  if (!/^[1-9]\d{5}$/.test(pincode)) throw new ApiError(400, "VALIDATION_ERROR", "Invalid pincode");
  const t = now(), slug = current.public_slug ?? `shop-${shopId.replaceAll("-", "").slice(0, 16)}`, result = { id: shopId, name: str(b.name, "name", 2, 120), ownerName: str(b.ownerName, "ownerName", 2, 100), phone: optional(b.phone, 20), addressLine: str(b.addressLine, "addressLine", 3, 300), locality: str(b.locality, "locality", 2, 100), city: str(b.city, "city", 2, 100), pincode, description: optional(b.description, 500), isPublished: b.isPublished === true, publicSlug: slug, version: version + 1 };
  await env.DB.batch([env.DB.prepare("UPDATE shops SET name=?,owner_name=?,phone_e164=?,address_line=?,locality=?,city=?,pincode=?,description=?,is_published=?,public_slug=?,version=version+1,updated_at=? WHERE id=? AND version=?").bind(result.name, result.ownerName, result.phone, result.addressLine, result.locality, result.city, result.pincode, result.description, result.isPublished ? 1 : 0, slug, t, shopId, version), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "UPDATE_SHOP_PROFILE", shopId, requestHash, JSON.stringify(result), t)]);
  return json(result);
}
__name(updateShopProfile, "updateShopProfile");
async function uploadProductImage(req, env, a, shopId, shopProductId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER", "STAFF"]);
  if (!env.MEDIA) throw new ApiError(503, "MEDIA_UNAVAILABLE", "Media storage is not configured");
  const mutation = uuid(req.headers.get("idempotency-key"), "Idempotency-Key"), mime = (req.headers.get("content-type") ?? "").toLowerCase();
  if (!["image/jpeg", "image/png", "image/webp"].includes(mime)) throw new ApiError(415, "INVALID_IMAGE_TYPE", "Only JPEG, PNG, or WebP is allowed");
  const bytes = await req.arrayBuffer();
  if (bytes.byteLength < 1 || bytes.byteLength > 5 * 1024 * 1024) throw new ApiError(413, "INVALID_IMAGE_SIZE", "Image must be between 1 byte and 5 MB");
  const sp = await env.DB.prepare("SELECT id,image_key,version FROM shop_products WHERE id=? AND shop_id=? AND is_archived=0").bind(shopProductId, shopId).first();
  if (!sp) throw new ApiError(404, "NOT_FOUND", "Product not found");
  const requestHash = await hash(`${shopId}:${shopProductId}:${mime}:${bytes.byteLength}:${await hash(String.fromCharCode(...new Uint8Array(bytes.slice(0, Math.min(bytes.byteLength, 4096)))))}`), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const ext = mime === "image/jpeg" ? "jpg" : mime === "image/png" ? "png" : "webp", key = `shops/${shopId}/products/${shopProductId}/${mutation}.${ext}`;
  await env.MEDIA.put(key, bytes, { httpMetadata: { contentType: mime }, customMetadata: { shopId, shopProductId } });
  const result = { shopProductId, imageKey: key, mimeType: mime, sizeBytes: bytes.byteLength, version: sp.version + 1 }, t = now();
  await env.DB.batch([env.DB.prepare("UPDATE shop_products SET image_key=?,image_mime_type=?,image_size_bytes=?,version=version+1,updated_at=? WHERE id=? AND shop_id=?").bind(key, mime, bytes.byteLength, t, shopProductId, shopId), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "PRODUCT_IMAGE", shopId, requestHash, JSON.stringify(result), t)]);
  if (sp.image_key && sp.image_key !== key) await env.MEDIA.delete(sp.image_key).catch(() => {
  });
  return json(result, 201);
}
__name(uploadProductImage, "uploadProductImage");
async function getProductImage(env, a, shopId, shopProductId) {
  await membership(env, a.userId, shopId);
  if (!env.MEDIA) throw new ApiError(503, "MEDIA_UNAVAILABLE", "Media storage is not configured");
  const sp = await env.DB.prepare("SELECT image_key,image_mime_type FROM shop_products WHERE id=? AND shop_id=? AND is_archived=0").bind(shopProductId, shopId).first();
  if (!sp) throw new ApiError(404, "NOT_FOUND", "Product not found");
  if (!sp.image_key) throw new ApiError(404, "IMAGE_NOT_FOUND", "Product has no image");
  const object = await env.MEDIA.get(sp.image_key);
  if (!object) throw new ApiError(404, "IMAGE_NOT_FOUND", "Image object not found");
  return new Response(object.body, { headers: { "content-type": sp.image_mime_type ?? "application/octet-stream", "cache-control": "private, max-age=300" } });
}
__name(getProductImage, "getProductImage");
async function removeProductImage(req, env, a, shopId, shopProductId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER", "STAFF"]);
  if (!env.MEDIA) throw new ApiError(503, "MEDIA_UNAVAILABLE", "Media storage is not configured");
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), expected = optional(b.expectedImageKey, 500), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const sp = await env.DB.prepare("SELECT image_key,version FROM shop_products WHERE id=? AND shop_id=? AND is_archived=0").bind(shopProductId, shopId).first();
  if (!sp) throw new ApiError(404, "NOT_FOUND", "Product not found");
  if (expected !== sp.image_key) throw new ApiError(409, "IMAGE_CONFLICT", "Product image changed; refresh before removing");
  const result = { shopProductId, imageKey: null, mimeType: null, sizeBytes: null, version: sp.version + 1 }, t = now();
  await env.DB.batch([env.DB.prepare("UPDATE shop_products SET image_key=NULL,image_mime_type=NULL,image_size_bytes=NULL,version=version+1,updated_at=? WHERE id=? AND shop_id=? AND image_key IS ?").bind(t, shopProductId, shopId, sp.image_key), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "REMOVE_PRODUCT_IMAGE", shopId, requestHash, JSON.stringify(result), t)]);
  if (sp.image_key) await env.MEDIA.delete(sp.image_key).catch(() => {
  });
  return json(result);
}
__name(removeProductImage, "removeProductImage");
function optionalPhone(v) {
  if (v == null || v === "") return null;
  return phone(v);
}
__name(optionalPhone, "optionalPhone");
function customerSelect(where) {
  return `SELECT sc.id shop_customer_id,sc.shop_id,sc.customer_id,COALESCE(sc.display_name_override,c.name) display_name,sc.normalized_phone,sc.notes,sc.credit_enabled,sc.credit_limit_paise,sc.current_balance_paise,CASE WHEN sc.credit_enabled=1 AND sc.credit_limit_paise IS NOT NULL THEN MAX(0,sc.credit_limit_paise-sc.current_balance_paise) ELSE NULL END available_credit_paise,sc.is_archived,sc.version,sc.updated_at FROM shop_customers sc JOIN customers c ON c.id=sc.customer_id WHERE ${where}`;
}
__name(customerSelect, "customerSelect");
async function listCustomers(req, env, a, shopId) {
  await membership(env, a.userId, shopId);
  const u = new URL(req.url), q = (u.searchParams.get("q") ?? "").trim(), limit = Math.min(100, Math.max(1, Number(u.searchParams.get("limit") ?? 50) || 50)), offset = Math.max(0, Number(u.searchParams.get("offset") ?? 0) || 0), due = u.searchParams.get("due");
  const r = await env.DB.prepare(customerSelect(`sc.shop_id=? AND sc.is_archived=0 AND (?='' OR COALESCE(sc.display_name_override,c.name) LIKE ? OR COALESCE(sc.normalized_phone,'') LIKE ?) AND (?='' OR (?='due' AND sc.current_balance_paise>0) OR (?='clear' AND sc.current_balance_paise=0)) ORDER BY COALESCE(sc.display_name_override,c.name),sc.id LIMIT ? OFFSET ?`)).bind(shopId, q, `%${q}%`, `%${q}%`, due ?? "", due ?? "", due ?? "", limit, offset).all();
  return json({ items: r.results, nextOffset: r.results.length === limit ? offset + limit : null });
}
__name(listCustomers, "listCustomers");
async function createCustomer(req, env, a, shopId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER", "STAFF"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const name = str(b.name, "name", 1, 120), mobile = optionalPhone(b.mobile), creditEnabled = b.creditEnabled === true, limit = b.creditLimitPaise == null ? null : int(b.creditLimitPaise, "creditLimitPaise"), customerId = id(), relationshipId = id(), t = now(), result = { shopCustomerId: relationshipId, shopId, customerId, displayName: name, normalizedPhone: mobile, notes: optional(b.notes, 500), creditEnabled, creditLimitPaise: limit, currentBalancePaise: 0, availableCreditPaise: creditEnabled && limit != null ? limit : null, isArchived: false, version: 1 };
  if (mobile && await env.DB.prepare("SELECT id FROM shop_customers WHERE shop_id=? AND normalized_phone=?").bind(shopId, mobile).first()) throw new ApiError(409, "CUSTOMER_PHONE_EXISTS", "This mobile number is already in this shop");
  await env.DB.batch([env.DB.prepare("INSERT INTO customers(id,name,mobile_e164,normalized_phone,created_at,updated_at) VALUES(?,?,?,?,?,?)").bind(customerId, name, mobile, mobile, t, t), env.DB.prepare("INSERT INTO shop_customers(id,shop_id,customer_id,normalized_phone,display_name_override,notes,credit_enabled,credit_limit_paise,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)").bind(relationshipId, shopId, customerId, mobile, name, result.notes, creditEnabled ? 1 : 0, limit, t, t), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "CUSTOMER_CREATE", shopId, requestHash, JSON.stringify(result), t)]);
  return json(result, 201);
}
__name(createCustomer, "createCustomer");
async function getCustomer(env, a, shopId, relationshipId) {
  await membership(env, a.userId, shopId);
  const c = await env.DB.prepare(customerSelect("sc.shop_id=? AND sc.id=?")).bind(shopId, relationshipId).first();
  if (!c) throw new ApiError(404, "NOT_FOUND", "Customer not found");
  const totals = await env.DB.prepare(`SELECT COALESCE(SUM(CASE WHEN type IN ('CREDIT_SALE','ADJUSTMENT_CREDIT') THEN amount_paise ELSE 0 END),0) total_credit_paise,COALESCE(SUM(CASE WHEN type IN ('PAYMENT','ADJUSTMENT_DEBIT') THEN amount_paise ELSE 0 END),0) total_payments_paise FROM credit_transactions WHERE shop_id=? AND shop_customer_id=?`).bind(shopId, relationshipId).first();
  const sales = await env.DB.prepare("SELECT id,total_paise,payment_type,credit_amount_paise,created_at FROM sales WHERE shop_id=? AND shop_customer_id=? ORDER BY created_at DESC LIMIT 20").bind(shopId, relationshipId).all();
  return json({ ...c, ...totals, recent_sales: sales.results });
}
__name(getCustomer, "getCustomer");
async function updateCustomer(req, env, a, shopId, relationshipId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const current = await env.DB.prepare("SELECT version,current_balance_paise FROM shop_customers WHERE id=? AND shop_id=? AND is_archived=0").bind(relationshipId, shopId).first();
  if (!current) throw new ApiError(404, "NOT_FOUND", "Customer not found");
  const version = int(b.version, "version", 1);
  if (version !== current.version) throw new ApiError(409, "VERSION_CONFLICT", "Customer changed; refresh");
  const name = str(b.name, "name", 1, 120), mobile = optionalPhone(b.mobile), enabled = b.creditEnabled === true, limit = b.creditLimitPaise == null ? null : int(b.creditLimitPaise, "creditLimitPaise");
  if (limit != null && current.current_balance_paise > limit) throw new ApiError(409, "CREDIT_LIMIT_BELOW_BALANCE", "Limit cannot be below current balance");
  if (mobile && await env.DB.prepare("SELECT id FROM shop_customers WHERE shop_id=? AND normalized_phone=? AND id<>?").bind(shopId, mobile, relationshipId).first()) throw new ApiError(409, "CUSTOMER_PHONE_EXISTS", "This mobile number is already in this shop");
  const t = now(), result = { shopCustomerId: relationshipId, shopId, displayName: name, normalizedPhone: mobile, notes: optional(b.notes, 500), creditEnabled: enabled, creditLimitPaise: limit, currentBalancePaise: current.current_balance_paise, availableCreditPaise: enabled && limit != null ? Math.max(0, limit - current.current_balance_paise) : null, isArchived: false, version: version + 1 };
  await env.DB.batch([env.DB.prepare("UPDATE shop_customers SET normalized_phone=?,display_name_override=?,notes=?,credit_enabled=?,credit_limit_paise=?,version=version+1,updated_at=? WHERE id=? AND shop_id=? AND version=?").bind(mobile, name, result.notes, enabled ? 1 : 0, limit, t, relationshipId, shopId, version), env.DB.prepare("UPDATE customers SET name=?,normalized_phone=?,mobile_e164=?,updated_at=? WHERE id=(SELECT customer_id FROM shop_customers WHERE id=? AND shop_id=?)").bind(name, mobile, mobile, t, relationshipId, shopId), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "CUSTOMER_UPDATE", shopId, requestHash, JSON.stringify(result), t)]);
  return json(result);
}
__name(updateCustomer, "updateCustomer");
async function archiveCustomer(req, env, a, shopId, relationshipId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const c = await env.DB.prepare("SELECT id FROM shop_customers WHERE id=? AND shop_id=?").bind(relationshipId, shopId).first();
  if (!c) throw new ApiError(404, "NOT_FOUND", "Customer not found");
  const result = { shopCustomerId: relationshipId, isArchived: true }, t = now();
  await env.DB.batch([env.DB.prepare("UPDATE shop_customers SET is_archived=1,version=version+1,updated_at=? WHERE id=? AND shop_id=?").bind(t, relationshipId, shopId), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "CUSTOMER_ARCHIVE", shopId, requestHash, JSON.stringify(result), t)]);
  return json(result);
}
__name(archiveCustomer, "archiveCustomer");
var creditTypes = /* @__PURE__ */ new Set(["CREDIT_SALE", "PAYMENT", "ADJUSTMENT_CREDIT", "ADJUSTMENT_DEBIT", "REVERSAL"]);
async function mutateCredit(req, env, a, shopId, relationshipId, routeType) {
  const member = await membership(env, a.userId, shopId, ["OWNER", "MANAGER", "STAFF"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  let type = routeType === "credit" ? "CREDIT_SALE" : routeType === "payment" ? "PAYMENT" : str(b.type, "type", 1, 30).toUpperCase();
  if (!creditTypes.has(type)) throw new ApiError(400, "INVALID_CREDIT_TYPE", "Invalid credit type");
  if (type.startsWith("ADJUSTMENT") && member.role === "STAFF") throw new ApiError(403, "FORBIDDEN", "Only owner or manager can adjust credit");
  const amount = int(b.amountPaise, "amountPaise", 1), c = await env.DB.prepare("SELECT current_balance_paise,credit_enabled,credit_limit_paise FROM shop_customers WHERE id=? AND shop_id=? AND is_archived=0").bind(relationshipId, shopId).first();
  if (!c) throw new ApiError(404, "NOT_FOUND", "Customer not found");
  if (!c.credit_enabled && type !== "PAYMENT") throw new ApiError(409, "CREDIT_DISABLED", "Credit is disabled for this customer");
  const positive = type === "CREDIT_SALE" || type === "ADJUSTMENT_CREDIT", newBalance = c.current_balance_paise + (positive ? amount : -amount);
  if (newBalance < 0) throw new ApiError(409, "PAYMENT_EXCEEDS_BALANCE", "Payment exceeds current balance");
  if (c.credit_limit_paise != null && newBalance > c.credit_limit_paise) throw new ApiError(409, "CREDIT_LIMIT_EXCEEDED", "Credit limit exceeded");
  const txId = id(), t = now(), result = { transactionId: txId, shopId, shopCustomerId: relationshipId, type, amountPaise: amount, balanceAfterPaise: newBalance, referenceType: optional(b.referenceType, 40), referenceId: optional(b.referenceId, 100), note: optional(b.note, 500), mutationId: mutation, createdAt: t };
  try {
    await env.DB.batch([env.DB.prepare("UPDATE shop_customers SET current_balance_paise=current_balance_paise+?,version=version+1,updated_at=? WHERE id=? AND shop_id=?").bind(positive ? amount : -amount, t, relationshipId, shopId), env.DB.prepare("INSERT INTO credit_transactions(id,shop_id,shop_customer_id,type,amount_paise,balance_after_paise,reference_type,reference_id,note,mutation_id,actor_user_id,created_at) SELECT ?,?,?,?, ?,current_balance_paise,?,?,?,?,?,? FROM shop_customers WHERE id=? AND shop_id=?").bind(txId, shopId, relationshipId, type, amount, result.referenceType, result.referenceId, result.note, mutation, a.userId, t, relationshipId, shopId), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, `CREDIT_${type}`, shopId, requestHash, JSON.stringify(result), t)]);
  } catch {
    throw new ApiError(409, "CREDIT_CONFLICT", "Balance changed; refresh and retry");
  }
  return json(result, 201);
}
__name(mutateCredit, "mutateCredit");
async function statement(req, env, a, shopId, relationshipId) {
  await membership(env, a.userId, shopId);
  if (!await env.DB.prepare("SELECT id FROM shop_customers WHERE id=? AND shop_id=?").bind(relationshipId, shopId).first()) throw new ApiError(404, "NOT_FOUND", "Customer not found");
  const u = new URL(req.url), limit = Math.min(100, Math.max(1, Number(u.searchParams.get("limit") ?? 50) || 50)), offset = Math.max(0, Number(u.searchParams.get("offset") ?? 0) || 0);
  const r = await env.DB.prepare("SELECT id transaction_id,type,amount_paise,balance_after_paise,reference_type,reference_id,note,mutation_id,created_at FROM credit_transactions WHERE shop_id=? AND shop_customer_id=? ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?").bind(shopId, relationshipId, limit, offset).all();
  return json({ items: r.results, nextOffset: r.results.length === limit ? offset + limit : null });
}
__name(statement, "statement");
var stockOutTypes = /* @__PURE__ */ new Set(["SALE", "DAMAGE", "EXPIRED", "LOSS", "ADJUSTMENT", "RETURN_OUT"]);
async function mutateStock(req, env, a, shopId, shopProductId, direction) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER", "STAFF"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const qty = int(b.quantityMilli, "quantityMilli", 1), type = direction === "IN" ? "PURCHASE" : str(b.type, "type", 1, 30).toUpperCase();
  if (direction === "OUT" && !stockOutTypes.has(type)) throw new ApiError(400, "INVALID_TRANSACTION_TYPE", "Invalid stock-out reason");
  const sp = await env.DB.prepare("SELECT stock_quantity_milli,reserved_quantity_milli FROM shop_products WHERE id=? AND shop_id=? AND is_archived=0").bind(shopProductId, shopId).first();
  if (!sp) throw new ApiError(404, "NOT_FOUND", "Product not found");
  const delta = direction === "IN" ? qty : -qty, newStock = sp.stock_quantity_milli + delta;
  if (newStock < sp.reserved_quantity_milli) throw new ApiError(409, "INSUFFICIENT_STOCK", "Not enough available stock");
  const t = now(), txId = id(), result = { transactionId: txId, shopId, shopProductId, type, quantityDeltaMilli: delta, stockQuantityMilli: newStock, mutationId: mutation, createdAt: t };
  try {
    await env.DB.batch([env.DB.prepare("UPDATE shop_products SET stock_quantity_milli=stock_quantity_milli+?,version=version+1,updated_at=? WHERE id=? AND shop_id=?").bind(delta, t, shopProductId, shopId), env.DB.prepare("INSERT INTO inventory_transactions(id,shop_id,shop_product_id,type,quantity_delta_milli,stock_after_milli,reference_type,reference_id,note,mutation_id,actor_user_id,created_at) SELECT ?,?,?,?, ?,stock_quantity_milli,?,?,?,?,?,? FROM shop_products WHERE id=? AND shop_id=?").bind(txId, shopId, shopProductId, type, delta, optional(b.referenceType, 40), optional(b.referenceId, 100), optional(b.note, 500), mutation, a.userId, t, shopProductId, shopId), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, `STOCK_${direction}`, shopId, requestHash, JSON.stringify(result), t)]);
  } catch {
    throw new ApiError(409, "INVENTORY_CONFLICT", "Stock changed; refresh and retry");
  }
  return json(result, 201);
}
__name(mutateStock, "mutateStock");
async function inventoryHistory(req, env, a, shopId, shopProductId) {
  await membership(env, a.userId, shopId);
  if (!await env.DB.prepare("SELECT id FROM shop_products WHERE id=? AND shop_id=?").bind(shopProductId, shopId).first()) throw new ApiError(404, "NOT_FOUND", "Product not found");
  const r = await env.DB.prepare("SELECT id,type,quantity_delta_milli,stock_after_milli,reference_type,reference_id,note,mutation_id,created_at FROM inventory_transactions WHERE shop_id=? AND shop_product_id=? ORDER BY created_at DESC,id DESC LIMIT 200").bind(shopId, shopProductId).all();
  return json({ items: r.results });
}
__name(inventoryHistory, "inventoryHistory");
async function createSale(req, env, a, shopId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER", "STAFF"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  if (!Array.isArray(b.items) || b.items.length < 1 || b.items.length > 100) throw new ApiError(400, "VALIDATION_ERROR", "Sale requires 1\u2013100 items");
  const quantities = /* @__PURE__ */ new Map();
  for (const raw of b.items) {
    const productId = uuid(raw.shopProductId, "shopProductId"), qty = int(raw.quantityMilli, "quantityMilli", 1);
    quantities.set(productId, (quantities.get(productId) ?? 0) + qty);
  }
  const ids = [...quantities.keys()], placeholders = ids.map(() => "?").join(","), rows = (await env.DB.prepare(`SELECT sp.id,sp.stock_quantity_milli,sp.reserved_quantity_milli,sp.selling_price_paise,p.name,p.unit FROM shop_products sp JOIN products p ON p.id=sp.product_id WHERE sp.shop_id=? AND sp.is_archived=0 AND sp.is_available=1 AND sp.id IN (${placeholders})`).bind(shopId, ...ids).all()).results;
  if (rows.length !== ids.length) throw new ApiError(400, "INVALID_SALE_ITEM", "A product is unavailable or belongs to another shop");
  let subtotal = 0;
  for (const row of rows) {
    const qty = quantities.get(row.id);
    if (row.stock_quantity_milli - row.reserved_quantity_milli < qty) throw new ApiError(409, "INSUFFICIENT_STOCK", `${row.name} has insufficient stock`);
    subtotal += Math.round(row.selling_price_paise * qty / 1e3);
    if (!Number.isSafeInteger(subtotal)) throw new ApiError(400, "TOTAL_TOO_LARGE", "Sale total is too large");
  }
  let customerId = null, shopCustomerId = null, creditCustomer = null;
  const paymentType = b.paymentType === "CREDIT" ? "CREDIT" : "PAID";
  if (typeof b.shopCustomerId === "string") {
    shopCustomerId = uuid(b.shopCustomerId, "shopCustomerId");
    creditCustomer = await env.DB.prepare("SELECT customer_id,current_balance_paise,credit_enabled,credit_limit_paise FROM shop_customers WHERE id=? AND shop_id=? AND is_archived=0").bind(shopCustomerId, shopId).first();
    if (!creditCustomer) throw new ApiError(400, "INVALID_CUSTOMER", "Customer does not belong to this shop");
    customerId = creditCustomer.customer_id;
  }
  if (paymentType === "CREDIT") {
    if (!shopCustomerId || !creditCustomer) throw new ApiError(400, "CUSTOMER_REQUIRED", "Credit sale requires a customer");
    if (!creditCustomer.credit_enabled) throw new ApiError(409, "CREDIT_DISABLED", "Credit is disabled");
    if (creditCustomer.credit_limit_paise != null && creditCustomer.current_balance_paise + subtotal > creditCustomer.credit_limit_paise) throw new ApiError(409, "CREDIT_LIMIT_EXCEEDED", "Credit limit exceeded");
  }
  const saleId = id(), t = now(), items = rows.map((row) => ({ id: id(), shopProductId: row.id, productName: row.name, unit: row.unit, unitPricePaise: row.selling_price_paise, quantityMilli: quantities.get(row.id), lineTotalPaise: Math.round(row.selling_price_paise * quantities.get(row.id) / 1e3) })), result = { saleId, shopId, subtotalPaise: subtotal, totalPaise: subtotal, customerId, shopCustomerId, paymentType, creditAmountPaise: paymentType === "CREDIT" ? subtotal : 0, status: "COMPLETED", mutationId: mutation, createdAt: t, items };
  const statements = [env.DB.prepare("INSERT INTO sales(id,shop_id,subtotal_paise,total_paise,customer_id,shop_customer_id,payment_type,credit_amount_paise,mutation_id,client_created_at,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)").bind(saleId, shopId, subtotal, subtotal, customerId, shopCustomerId, paymentType, paymentType === "CREDIT" ? subtotal : 0, mutation, typeof b.clientCreatedAt === "number" ? Math.floor(b.clientCreatedAt) : null, t)];
  for (const item of items) {
    const stockAfter = rows.find((x) => x.id === item.shopProductId).stock_quantity_milli - item.quantityMilli;
    statements.push(env.DB.prepare("UPDATE shop_products SET stock_quantity_milli=stock_quantity_milli-?,version=version+1,updated_at=? WHERE id=? AND shop_id=?").bind(item.quantityMilli, t, item.shopProductId, shopId), env.DB.prepare("INSERT INTO sale_items(id,sale_id,shop_id,shop_product_id,product_name_snapshot,unit_snapshot,unit_price_paise,quantity_milli,line_total_paise,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)").bind(item.id, saleId, shopId, item.shopProductId, item.productName, item.unit, item.unitPricePaise, item.quantityMilli, item.lineTotalPaise, t), env.DB.prepare(`INSERT INTO inventory_transactions(id,shop_id,shop_product_id,type,quantity_delta_milli,stock_after_milli,reference_type,reference_id,mutation_id,actor_user_id,created_at) SELECT ?,?,?, 'SALE',?,stock_quantity_milli,'SALE',?,?,?,? FROM shop_products WHERE id=? AND shop_id=?`).bind(id(), shopId, item.shopProductId, -item.quantityMilli, saleId, `${mutation}:${item.shopProductId}`, a.userId, t, item.shopProductId, shopId));
  }
  if (paymentType === "CREDIT") {
    const creditTx = id();
    statements.push(env.DB.prepare("UPDATE shop_customers SET current_balance_paise=current_balance_paise+?,version=version+1,updated_at=? WHERE id=? AND shop_id=?").bind(subtotal, t, shopCustomerId, shopId), env.DB.prepare("INSERT INTO credit_transactions(id,shop_id,shop_customer_id,type,amount_paise,balance_after_paise,reference_type,reference_id,note,mutation_id,actor_user_id,created_at) SELECT ?,?,?, 'CREDIT_SALE',?,current_balance_paise,'SALE',?,NULL,?,?,? FROM shop_customers WHERE id=? AND shop_id=?").bind(creditTx, shopId, shopCustomerId, subtotal, saleId, mutation, a.userId, t, shopCustomerId, shopId));
  }
  statements.push(env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "QUICK_SALE", shopId, requestHash, JSON.stringify(result), t));
  try {
    await env.DB.batch(statements);
  } catch {
    throw new ApiError(409, "SALE_CONFLICT", "Stock changed; no items were sold");
  }
  return json(result, 201);
}
__name(createSale, "createSale");
async function listSales(req, env, a, shopId, saleId) {
  await membership(env, a.userId, shopId);
  if (saleId) {
    const sale = await env.DB.prepare("SELECT * FROM sales WHERE id=? AND shop_id=?").bind(saleId, shopId).first();
    if (!sale) throw new ApiError(404, "NOT_FOUND", "Sale not found");
    const items = await env.DB.prepare("SELECT * FROM sale_items WHERE sale_id=? AND shop_id=? ORDER BY id").bind(saleId, shopId).all();
    return json({ ...sale, items: items.results });
  }
  const r = await env.DB.prepare("SELECT s.id,s.subtotal_paise,s.total_paise,s.customer_id,s.status,s.created_at,count(si.id) item_count FROM sales s JOIN sale_items si ON si.sale_id=s.id WHERE s.shop_id=? GROUP BY s.id ORDER BY s.created_at DESC LIMIT 100").bind(shopId).all();
  return json({ items: r.results });
}
__name(listSales, "listSales");
async function lowStock(env, a, shopId) {
  await membership(env, a.userId, shopId);
  const r = await env.DB.prepare(`SELECT sp.id shop_product_id,p.name,p.unit,sp.stock_quantity_milli,sp.minimum_stock_milli,MAX(0,sp.minimum_stock_milli-sp.stock_quantity_milli) shortage_milli FROM shop_products sp JOIN products p ON p.id=sp.product_id WHERE sp.shop_id=? AND sp.is_archived=0 AND sp.stock_quantity_milli<=sp.minimum_stock_milli ORDER BY shortage_milli DESC,sp.stock_quantity_milli`).bind(shopId).all();
  return json({ items: r.results });
}
__name(lowStock, "lowStock");
async function listPurchaseItems(env, a, shopId) {
  await membership(env, a.userId, shopId);
  const r = await env.DB.prepare("SELECT pl.*,p.name,p.unit FROM purchase_list_items pl JOIN shop_products sp ON sp.id=pl.shop_product_id JOIN products p ON p.id=sp.product_id WHERE pl.shop_id=? ORDER BY pl.is_purchased,pl.updated_at DESC").bind(shopId).all();
  return json({ items: r.results });
}
__name(listPurchaseItems, "listPurchaseItems");
async function addPurchaseItem(req, env, a, shopId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER", "STAFF"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const spid = uuid(b.shopProductId, "shopProductId"), qty = int(b.quantityMilli, "quantityMilli", 1);
  if (!await env.DB.prepare("SELECT id FROM shop_products WHERE id=? AND shop_id=? AND is_archived=0").bind(spid, shopId).first()) throw new ApiError(404, "NOT_FOUND", "Product not found");
  const itemId = id(), t = now(), result = { id: itemId, shopId, shopProductId: spid, quantityMilli: qty, note: optional(b.note, 500), isPurchased: false, version: 1 };
  try {
    await env.DB.batch([env.DB.prepare("INSERT INTO purchase_list_items(id,shop_id,shop_product_id,quantity_milli,note,created_at,updated_at) VALUES(?,?,?,?,?,?,?)").bind(itemId, shopId, spid, qty, result.note, t, t), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "ADD_PURCHASE_ITEM", shopId, requestHash, JSON.stringify(result), t)]);
  } catch {
    throw new ApiError(409, "PURCHASE_ITEM_EXISTS", "Product is already on the purchase list");
  }
  return json(result, 201);
}
__name(addPurchaseItem, "addPurchaseItem");
async function updatePurchaseItem(req, env, a, shopId, itemId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER", "STAFF"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const current = await env.DB.prepare("SELECT version,shop_product_id FROM purchase_list_items WHERE id=? AND shop_id=?").bind(itemId, shopId).first();
  if (!current) throw new ApiError(404, "NOT_FOUND", "Purchase item not found");
  const version = int(b.version, "version", 1);
  if (version !== current.version) throw new ApiError(409, "VERSION_CONFLICT", "Purchase item changed");
  const result = { id: itemId, shopId, shopProductId: current.shop_product_id, quantityMilli: int(b.quantityMilli, "quantityMilli", 1), note: optional(b.note, 500), isPurchased: b.isPurchased === true, version: version + 1 }, t = now();
  await env.DB.batch([env.DB.prepare("UPDATE purchase_list_items SET quantity_milli=?,note=?,is_purchased=?,version=version+1,updated_at=? WHERE id=? AND shop_id=? AND version=?").bind(result.quantityMilli, result.note, result.isPurchased ? 1 : 0, t, itemId, shopId, version), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "UPDATE_PURCHASE_ITEM", shopId, requestHash, JSON.stringify(result), t)]);
  return json(result);
}
__name(updatePurchaseItem, "updatePurchaseItem");
async function deletePurchaseItem(req, env, a, shopId, itemId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER", "STAFF"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  if (!await env.DB.prepare("SELECT id FROM purchase_list_items WHERE id=? AND shop_id=?").bind(itemId, shopId).first()) throw new ApiError(404, "NOT_FOUND", "Purchase item not found");
  const result = { id: itemId, deleted: true }, t = now();
  await env.DB.batch([env.DB.prepare("DELETE FROM purchase_list_items WHERE id=? AND shop_id=?").bind(itemId, shopId), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "DELETE_PURCHASE_ITEM", shopId, requestHash, JSON.stringify(result), t)]);
  return json(result);
}
__name(deletePurchaseItem, "deletePurchaseItem");
async function createAddress(req, env, a) {
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const pincode = str(b.pincode, "pincode", 6, 6);
  if (!/^[1-9]\d{5}$/.test(pincode)) throw new ApiError(400, "VALIDATION_ERROR", "Invalid pincode");
  const t = now(), aid = id(), result = { id: aid, label: optional(b.label, 60), recipientName: str(b.recipientName, "recipientName", 2, 100), mobile: optionalPhone(b.mobile), addressLine: str(b.addressLine, "addressLine", 3, 300), locality: str(b.locality, "locality", 2, 100), city: str(b.city, "city", 2, 100), pincode, latitude: typeof b.latitude === "number" ? b.latitude : null, longitude: typeof b.longitude === "number" ? b.longitude : null };
  await env.DB.batch([env.DB.prepare("INSERT INTO addresses(id,user_id,label,recipient_name,mobile_e164,address_line,locality,city,pincode,latitude,longitude,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)").bind(aid, a.userId, result.label, result.recipientName, result.mobile, result.addressLine, result.locality, result.city, result.pincode, result.latitude, result.longitude, t, t), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "ADDRESS_CREATE", null, requestHash, JSON.stringify(result), t)]);
  return json(result, 201);
}
__name(createAddress, "createAddress");
async function listAddresses(env, a) {
  const r = await env.DB.prepare("SELECT id,label,recipient_name,mobile_e164,address_line,locality,city,pincode,latitude,longitude,is_default FROM addresses WHERE user_id=? ORDER BY created_at").bind(a.userId).all();
  return json({ items: r.results });
}
__name(listAddresses, "listAddresses");
async function marketShops(req, env, a) {
  void a;
  const u = new URL(req.url);
  const rawLat = u.searchParams.get("latitude") ?? u.searchParams.get("lat"), rawLng = u.searchParams.get("longitude") ?? u.searchParams.get("lng");
  const lat = rawLat == null ? NaN : Number(rawLat), lng = rawLng == null ? NaN : Number(rawLng), radius = Math.min(2e4, Math.max(100, Number(u.searchParams.get("radiusM") ?? 5e3) || 5e3)), q = (u.searchParams.get("q") ?? "").trim();
  const conds = [], binds = [];
  if (q !== "") {
    conds.push("(s.name LIKE ? OR s.category LIKE ? OR s.locality LIKE ? OR s.city LIKE ?)");
    const like = `%${q}%`;
    binds.push(like, like, like, like);
  }
  const where = conds.length ? ` AND ${conds.join(" AND ")}` : "";
  const rows = (await env.DB.prepare(`SELECT s.id,s.name,s.category,s.locality,s.city,s.pincode,s.latitude,s.longitude,s.public_slug,s.opens_at,s.closes_at,d.delivery_enabled,d.radius_m,d.minimum_order_paise,d.delivery_fee_paise,d.free_delivery_threshold_paise,d.estimated_minutes FROM shops s JOIN shop_delivery_settings d ON d.shop_id=s.id WHERE s.status='ACTIVE' AND s.is_published=1 AND s.accepts_online_orders=1${where} LIMIT 200`).bind(...binds).all()).results;
  const items = rows.map((r) => ({ ...r, distance_m: Number.isFinite(lat) && Number.isFinite(lng) ? Math.round(distanceM(lat, lng, r.latitude, r.longitude)) : null })).filter((r) => r.distance_m === null || r.distance_m <= radius).sort((x, y) => (x.distance_m ?? 1e9) - (y.distance_m ?? 1e9));
  return json({ items });
}
__name(marketShops, "marketShops");
async function marketShopPage(env, slug) {
  const shop = await env.DB.prepare(`SELECT s.id,s.name,s.owner_name,s.category,s.address_line,s.locality,s.city,s.pincode,s.description,s.opens_at,s.closes_at,s.latitude,s.longitude,s.public_slug,d.delivery_enabled,d.radius_m,d.minimum_order_paise,d.delivery_fee_paise,d.free_delivery_threshold_paise,d.estimated_minutes FROM shops s JOIN shop_delivery_settings d ON d.shop_id=s.id WHERE s.public_slug=? AND s.status='ACTIVE' AND s.is_published=1`).bind(slug).first();
  if (!shop) throw new ApiError(404, "NOT_FOUND", "Shop not found");
  const items = (await env.DB.prepare(productSelect("sp.shop_id=? AND sp.is_archived=0 AND sp.is_available=1 ORDER BY p.name LIMIT 500")).bind(shop.id).all()).results;
  return json({ shop, items });
}
__name(marketShopPage, "marketShopPage");
async function createOrder(req, env, a) {
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const shopId = uuid(b.shopId, "shopId");
  const shop = await env.DB.prepare(`SELECT s.id,s.status,s.is_published,s.accepts_online_orders,s.latitude,s.longitude,d.delivery_enabled,d.radius_m,d.minimum_order_paise,d.delivery_fee_paise,d.free_delivery_threshold_paise FROM shops s JOIN shop_delivery_settings d ON d.shop_id=s.id WHERE s.id=?`).bind(shopId).first();
  if (!shop) throw new ApiError(404, "NOT_FOUND", "Shop not found");
  if (shop.status !== "ACTIVE" || shop.is_published !== 1 || shop.accepts_online_orders !== 1) throw new ApiError(409, "SHOP_NOT_ACCEPTING", "Shop is not accepting online orders");
  if (!Array.isArray(b.items) || b.items.length < 1 || b.items.length > 100) throw new ApiError(400, "VALIDATION_ERROR", "Order requires 1\u2013100 items");
  const fulfillment = b.fulfillmentType === "PICKUP" ? "PICKUP" : "DELIVERY";
  if (fulfillment === "DELIVERY" && shop.delivery_enabled !== 1) throw new ApiError(409, "DELIVERY_UNAVAILABLE", "Delivery is not available for this shop");
  const quantities = /* @__PURE__ */ new Map();
  for (const raw of b.items) {
    const pid = uuid(raw.shopProductId, "shopProductId"), qty = int(raw.quantityMilli, "quantityMilli", 1);
    quantities.set(pid, (quantities.get(pid) ?? 0) + qty);
  }
  const ids = [...quantities.keys()], ph = ids.map(() => "?").join(","), rows = (await env.DB.prepare(`SELECT sp.id,sp.stock_quantity_milli,sp.reserved_quantity_milli,sp.selling_price_paise,p.name,p.unit FROM shop_products sp JOIN products p ON p.id=sp.product_id WHERE sp.shop_id=? AND sp.is_archived=0 AND sp.is_available=1 AND sp.id IN (${ph})`).bind(shopId, ...ids).all()).results;
  if (rows.length !== ids.length) throw new ApiError(400, "INVALID_ORDER_ITEM", "A product is unavailable or belongs to another shop");
  let subtotal = 0;
  for (const row of rows) {
    const qty = quantities.get(row.id);
    if (row.stock_quantity_milli - row.reserved_quantity_milli < qty) throw new ApiError(409, "INSUFFICIENT_STOCK", `${row.name} has insufficient stock`);
    subtotal += Math.round(row.selling_price_paise * qty / 1e3);
    if (!Number.isSafeInteger(subtotal)) throw new ApiError(400, "TOTAL_TOO_LARGE", "Order total is too large");
  }
  if (subtotal < Number(shop.minimum_order_paise)) throw new ApiError(409, "MINIMUM_ORDER_NOT_MET", "Order is below the shop minimum");
  let addressId = null, recipient = null, mobile = null, addrLine = null, locality = null, city = null, pincode = null, addrLat = null, addrLng = null, deliveryFee = 0;
  if (fulfillment === "DELIVERY") {
    if (typeof b.addressId === "string") {
      const aid = uuid(b.addressId, "addressId");
      const addr = await env.DB.prepare("SELECT * FROM addresses WHERE id=? AND user_id=?").bind(aid, a.userId).first();
      if (!addr) throw new ApiError(404, "ADDRESS_NOT_FOUND", "Address not found");
      addressId = aid;
      recipient = addr.recipient_name;
      mobile = addr.mobile_e164;
      addrLine = addr.address_line;
      locality = addr.locality;
      city = addr.city;
      pincode = addr.pincode;
      addrLat = addr.latitude ?? null;
      addrLng = addr.longitude ?? null;
      if (Number.isFinite(addrLat) && Number.isFinite(addrLng)) {
        const dist = distanceM(addrLat, addrLng, shop.latitude, shop.longitude);
        if (dist > Number(shop.radius_m)) throw new ApiError(409, "OUT_OF_DELIVERY_RANGE", "Address is outside the delivery radius");
      }
    }
    deliveryFee = Number(shop.delivery_fee_paise);
    if (shop.free_delivery_threshold_paise != null && subtotal >= Number(shop.free_delivery_threshold_paise)) deliveryFee = 0;
  }
  const total = subtotal + deliveryFee, t = now(), orderId = id();
  const itemRows = rows.map((row) => ({ id: id(), shopProductId: row.id, name: row.name, unit: row.unit, price: row.selling_price_paise, qty: quantities.get(row.id) }));
  const result = { orderId, shopId, status: "PENDING", fulfillmentType: fulfillment, addressId, subtotalPaise: subtotal, deliveryFeePaise: deliveryFee, totalPaise: total, note: optional(b.note, 500), mutationId: mutation, createdAt: t, items: itemRows.map((it) => ({ shopProductId: it.shopProductId, productName: it.name, unit: it.unit, unitPricePaise: it.price, quantityMilli: it.qty, lineTotalPaise: Math.round(it.price * it.qty / 1e3) })) };
  const statements = [];
  for (const it of itemRows) statements.push(env.DB.prepare("UPDATE shop_products SET reserved_quantity_milli=reserved_quantity_milli+?,version=version+1,updated_at=? WHERE id=? AND shop_id=?").bind(it.qty, t, it.shopProductId, shopId));
  statements.push(env.DB.prepare(`INSERT INTO orders(id,shop_id,customer_user_id,status,fulfillment_type,address_id,recipient_name,mobile_e164,address_line,locality,city,pincode,latitude,longitude,subtotal_paise,delivery_fee_paise,total_paise,note,mutation_id,created_at,updated_at) VALUES(?,?,?, 'PENDING',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`).bind(orderId, shopId, a.userId, fulfillment, addressId, recipient, mobile, addrLine, locality, city, pincode, addrLat, addrLng, subtotal, deliveryFee, total, result.note, mutation, t, t));
  for (const it of itemRows) statements.push(env.DB.prepare("INSERT INTO order_items(id,order_id,shop_id,shop_product_id,product_name_snapshot,unit_snapshot,unit_price_paise,quantity_milli,line_total_paise,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)").bind(it.id, orderId, shopId, it.shopProductId, it.name, it.unit, it.price, it.qty, Math.round(it.price * it.qty / 1e3), t));
  statements.push(env.DB.prepare("INSERT INTO order_status_history(id,order_id,status,actor_user_id,note,created_at) VALUES(?,?,?,?,?,?)").bind(id(), orderId, "PENDING", a.userId, null, t));
  statements.push(env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "CREATE_ORDER", shopId, requestHash, JSON.stringify(result), t));
  try {
    await env.DB.batch(statements);
  } catch {
    throw new ApiError(409, "ORDER_CONFLICT", "Stock changed; order not placed");
  }
  return json(result, 201);
}
__name(createOrder, "createOrder");
async function listOrders(env, a) {
  const r = await env.DB.prepare(`SELECT o.id,o.shop_id,o.status,o.fulfillment_type,o.subtotal_paise,o.delivery_fee_paise,o.total_paise,o.created_at,s.name shop_name FROM orders o JOIN shops s ON s.id=o.shop_id WHERE o.customer_user_id=? ORDER BY o.created_at DESC LIMIT 100`).bind(a.userId).all();
  return json({ items: r.results });
}
__name(listOrders, "listOrders");
async function getOrder(env, a, orderId) {
  const o = await env.DB.prepare("SELECT * FROM orders WHERE id=?").bind(orderId).first();
  if (!o) throw new ApiError(404, "NOT_FOUND", "Order not found");
  if (o.customer_user_id !== a.userId) await membership(env, a.userId, o.shop_id);
  const items = await env.DB.prepare("SELECT id,shop_product_id,product_name_snapshot,unit_snapshot,unit_price_paise,quantity_milli,line_total_paise FROM order_items WHERE order_id=? ORDER BY id").bind(orderId).all();
  const history = await env.DB.prepare("SELECT status,actor_user_id,note,created_at FROM order_status_history WHERE order_id=? ORDER BY created_at,id").bind(orderId).all();
  return json({ ...o, items: items.results, history: history.results });
}
__name(getOrder, "getOrder");
async function cancelOrder(req, env, a, orderId) {
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const o = await env.DB.prepare("SELECT * FROM orders WHERE id=? AND customer_user_id=?").bind(orderId, a.userId).first();
  if (!o) throw new ApiError(404, "NOT_FOUND", "Order not found");
  if (!["PENDING", "ACCEPTED"].includes(o.status)) throw new ApiError(409, "CANNOT_CANCEL", "Order can no longer be cancelled");
  const t = now(), items = (await env.DB.prepare("SELECT shop_product_id,quantity_milli FROM order_items WHERE order_id=?").bind(orderId).all()).results, statements = [];
  for (const it of items) statements.push(env.DB.prepare("UPDATE shop_products SET reserved_quantity_milli=MAX(0,reserved_quantity_milli-?),version=version+1,updated_at=? WHERE id=? AND shop_id=?").bind(it.quantity_milli, t, it.shop_product_id, o.shop_id));
  statements.push(env.DB.prepare("UPDATE orders SET status='CANCELLED',version=version+1,updated_at=? WHERE id=? AND status IN ('PENDING','ACCEPTED')").bind(t, orderId), env.DB.prepare("INSERT INTO order_status_history(id,order_id,status,actor_user_id,note,created_at) VALUES(?,?,?,?,?,?)").bind(id(), orderId, "CANCELLED", a.userId, null, t), env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, "CANCEL_ORDER", o.shop_id, requestHash, JSON.stringify({ orderId, status: "CANCELLED" }), t));
  await env.DB.batch(statements);
  return json({ orderId, status: "CANCELLED" });
}
__name(cancelOrder, "cancelOrder");
async function shopOrders(req, env, a, shopId) {
  await membership(env, a.userId, shopId);
  const u = new URL(req.url), status = (u.searchParams.get("status") ?? "").toUpperCase();
  const r = await env.DB.prepare(`SELECT o.id,o.status,o.fulfillment_type,o.subtotal_paise,o.delivery_fee_paise,o.total_paise,o.created_at,o.customer_user_id FROM orders o WHERE o.shop_id=? AND (?='' OR o.status=?) ORDER BY o.created_at DESC LIMIT 100`).bind(shopId, status, status).all();
  return json({ items: r.results });
}
__name(shopOrders, "shopOrders");
async function updateOrderStatus(req, env, a, shopId, orderId) {
  await membership(env, a.userId, shopId, ["OWNER", "MANAGER", "STAFF"]);
  const b = await body(req), mutation = uuid(b.mutationId, "mutationId"), requestHash = await hash(JSON.stringify(b)), prior = await receipt(env, a, mutation, requestHash);
  if (prior) return json(prior);
  const target = str(b.status, "status", 1, 30).toUpperCase();
  const o = await env.DB.prepare("SELECT * FROM orders WHERE id=? AND shop_id=?").bind(orderId, shopId).first();
  if (!o) throw new ApiError(404, "NOT_FOUND", "Order not found");
  if (!(orderTransitions[o.status] ?? []).includes(target)) throw new ApiError(409, "INVALID_TRANSITION", `Cannot move order from ${o.status} to ${target}`);
  const t = now(), statements = [env.DB.prepare("UPDATE orders SET status=?,version=version+1,updated_at=? WHERE id=? AND shop_id=? AND status=?").bind(target, t, orderId, shopId, o.status), env.DB.prepare("INSERT INTO order_status_history(id,order_id,status,actor_user_id,note,created_at) VALUES(?,?,?,?,?,?)").bind(id(), orderId, target, a.userId, optional(b.note, 500), t)];
  if (target === "DELIVERED") {
    const items = (await env.DB.prepare("SELECT id,shop_product_id,quantity_milli FROM order_items WHERE order_id=?").bind(orderId).all()).results;
    for (const it of items) {
      statements.push(env.DB.prepare("UPDATE shop_products SET stock_quantity_milli=stock_quantity_milli-?,reserved_quantity_milli=MAX(0,reserved_quantity_milli-?),version=version+1,updated_at=? WHERE id=? AND shop_id=?").bind(it.quantity_milli, it.quantity_milli, t, it.shop_product_id, shopId), env.DB.prepare("INSERT INTO inventory_transactions(id,shop_id,shop_product_id,type,quantity_delta_milli,stock_after_milli,reference_type,reference_id,mutation_id,actor_user_id,created_at) SELECT ?,?,?, 'SALE',?,stock_quantity_milli,'ORDER',?,?,?,? FROM shop_products WHERE id=? AND shop_id=?").bind(id(), shopId, it.shop_product_id, -it.quantity_milli, orderId, `${mutation}:${it.id}`, a.userId, t, it.shop_product_id, shopId));
    }
  } else if (target === "REJECTED" || target === "CANCELLED") {
    const items = (await env.DB.prepare("SELECT shop_product_id,quantity_milli FROM order_items WHERE order_id=?").bind(orderId).all()).results;
    for (const it of items) statements.push(env.DB.prepare("UPDATE shop_products SET reserved_quantity_milli=MAX(0,reserved_quantity_milli-?),version=version+1,updated_at=? WHERE id=? AND shop_id=?").bind(it.quantity_milli, t, it.shop_product_id, shopId));
  }
  statements.push(env.DB.prepare("INSERT INTO sync_operations(user_id,mutation_id,operation,shop_id,request_hash,response_json,created_at) VALUES(?,?,?,?,?,?,?)").bind(a.userId, mutation, `ORDER_${target}`, shopId, requestHash, JSON.stringify({ orderId, status: target }), t));
  try {
    await env.DB.batch(statements);
  } catch {
    throw new ApiError(409, "ORDER_CONFLICT", "Order changed; refresh and retry");
  }
  return json({ orderId, status: target });
}
__name(updateOrderStatus, "updateOrderStatus");
async function logout(req, env, a) {
  await env.DB.prepare("UPDATE sessions SET revoked_at=? WHERE id=? AND user_id=?").bind(now(), a.sessionId, a.userId).run();
  return new Response(null, { status: 204 });
}
__name(logout, "logout");
var src_default = { async fetch(req, env) {
  try {
    const u = new URL(req.url), p = u.pathname;
    if (req.method === "GET" && p === "/health") return json({ ok: true });
    if (req.method === "POST" && p === "/v1/auth/register") return await register(req, env);
    if (req.method === "POST" && p === "/v1/auth/login") return await login(req, env);
    if (req.method === "POST" && p === "/v1/auth/refresh") return await refresh(req, env);
    const a = await bearer(req, env);
    if (p === "/v1/profile" && (req.method === "GET" || req.method === "PUT")) return await profile(req, env, a);
    if (p === "/v1/shops" && req.method === "POST") return await createShop(req, env, a);
    if (p === "/v1/shops/mine" && req.method === "GET") return await myShops(env, a);
    if (p === "/v1/products/barcode/lookup" && req.method === "POST") return await lookupBarcode(req, env, a);
    const collection = p.match(/^\/v1\/shops\/([^/]+)\/products$/);
    if (collection && req.method === "GET") return await listProducts(req, env, a, collection[1]);
    if (collection && req.method === "POST") return await createProduct(req, env, a, collection[1]);
    const item = p.match(/^\/v1\/shops\/([^/]+)\/products\/([^/]+)$/);
    if (item && req.method === "GET") return await getProduct(env, a, item[1], item[2]);
    if (item && req.method === "PUT") return await updateProduct(req, env, a, item[1], item[2]);
    if (item && req.method === "DELETE") return await archiveProduct(req, env, a, item[1], item[2]);
    const image = p.match(/^\/v1\/shops\/([^/]+)\/products\/([^/]+)\/image$/);
    if (image && req.method === "GET") return await getProductImage(env, a, image[1], image[2]);
    if (image && req.method === "POST") return await uploadProductImage(req, env, a, image[1], image[2]);
    if (image && req.method === "DELETE") return await removeProductImage(req, env, a, image[1], image[2]);
    const customer = p.match(/^\/v1\/shops\/([^/]+)\/customers(?:\/([^/]+))?(?:\/(statement|credit|payment|adjustment))?$/);
    if (customer && req.method === "GET" && !customer[2]) return await listCustomers(req, env, a, customer[1]);
    if (customer && req.method === "POST" && !customer[2]) return await createCustomer(req, env, a, customer[1]);
    if (customer && req.method === "GET" && customer[2] && !customer[3]) return await getCustomer(env, a, customer[1], customer[2]);
    if (customer && req.method === "PUT" && customer[2] && !customer[3]) return await updateCustomer(req, env, a, customer[1], customer[2]);
    if (customer && req.method === "DELETE" && customer[2] && !customer[3]) return await archiveCustomer(req, env, a, customer[1], customer[2]);
    if (customer && req.method === "GET" && customer[3] === "statement") return await statement(req, env, a, customer[1], customer[2]);
    if (customer && req.method === "POST" && ["credit", "payment", "adjustment"].includes(customer[3] ?? "")) return await mutateCredit(req, env, a, customer[1], customer[2], customer[3]);
    const stock = p.match(/^\/v1\/shops\/([^/]+)\/products\/([^/]+)\/(stock-in|stock-out)$/);
    if (stock && req.method === "POST") return await mutateStock(req, env, a, stock[1], stock[2], stock[3] === "stock-in" ? "IN" : "OUT");
    const inventory = p.match(/^\/v1\/shops\/([^/]+)\/products\/([^/]+)\/inventory$/);
    if (inventory && req.method === "GET") return await inventoryHistory(req, env, a, inventory[1], inventory[2]);
    const sales = p.match(/^\/v1\/shops\/([^/]+)\/sales(?:\/([^/]+))?$/);
    if (sales && req.method === "POST" && !sales[2]) return await createSale(req, env, a, sales[1]);
    if (sales && req.method === "GET") return await listSales(req, env, a, sales[1], sales[2]);
    const low = p.match(/^\/v1\/shops\/([^/]+)\/low-stock$/);
    if (low && req.method === "GET") return await lowStock(env, a, low[1]);
    const purchase = p.match(/^\/v1\/shops\/([^/]+)\/purchase-list(?:\/([^/]+))?$/);
    if (purchase && req.method === "GET" && !purchase[2]) return await listPurchaseItems(env, a, purchase[1]);
    if (purchase && req.method === "POST" && !purchase[2]) return await addPurchaseItem(req, env, a, purchase[1]);
    if (purchase && req.method === "PUT" && purchase[2]) return await updatePurchaseItem(req, env, a, purchase[1], purchase[2]);
    if (purchase && req.method === "DELETE" && purchase[2]) return await deletePurchaseItem(req, env, a, purchase[1], purchase[2]);
    const shopProfile = p.match(/^\/v1\/shops\/([^/]+)\/profile$/);
    if (shopProfile && req.method === "GET") return await privateShop(env, a, shopProfile[1]);
    if (shopProfile && req.method === "PUT") return await updateShopProfile(req, env, a, shopProfile[1]);
    const m = p.match(/^\/v1\/shops\/([^/]+)\/private$/);
    if (m && req.method === "GET") return await privateShop(env, a, m[1]);
    if (p === "/v1/auth/logout" && req.method === "POST") return await logout(req, env, a);
    if (p === "/v1/addresses" && req.method === "POST") return await createAddress(req, env, a);
    if (p === "/v1/addresses" && req.method === "GET") return await listAddresses(env, a);
    if (p === "/v1/market/shops" && req.method === "GET") return await marketShops(req, env, a);
    const mkt = p.match(/^\/v1\/market\/shops\/([^/]+)$/);
    if (mkt && req.method === "GET") return await marketShopPage(env, decodeURIComponent(mkt[1]));
    if (p === "/v1/orders" && req.method === "POST") return await createOrder(req, env, a);
    if (p === "/v1/orders" && req.method === "GET") return await listOrders(env, a);
    const ord = p.match(/^\/v1\/orders\/([^/]+)(?:\/(cancel))?$/);
    if (ord && req.method === "GET" && !ord[2]) return await getOrder(env, a, ord[1]);
    if (ord && req.method === "POST" && ord[2] === "cancel") return await cancelOrder(req, env, a, ord[1]);
    const so = p.match(/^\/v1\/shops\/([^/]+)\/orders(?:\/([^/]+))?(?:\/(status))?$/);
    if (so && req.method === "GET" && !so[2]) return await shopOrders(req, env, a, so[1]);
    if (so && req.method === "POST" && so[2] && so[3] === "status") return await updateOrderStatus(req, env, a, so[1], so[2]);
    throw new ApiError(404, "NOT_FOUND", "Endpoint not found");
  } catch (e) {
    const x = e instanceof ApiError ? e : new ApiError(500, "INTERNAL_ERROR", "Unexpected server error");
    return json({ error: { code: x.code, message: x.message } }, x.status);
  }
} };

// node_modules/wrangler/templates/middleware/middleware-ensure-req-body-drained.ts
var drainBody = /* @__PURE__ */ __name(async (request, env, _ctx, middlewareCtx) => {
  try {
    return await middlewareCtx.next(request, env);
  } finally {
    try {
      if (request.body !== null && !request.bodyUsed) {
        const reader = request.body.getReader();
        while (!(await reader.read()).done) {
        }
      }
    } catch (e) {
      console.error("Failed to drain the unused request body.", e);
    }
  }
}, "drainBody");
var middleware_ensure_req_body_drained_default = drainBody;

// node_modules/wrangler/templates/middleware/middleware-miniflare3-json-error.ts
function reduceError(e) {
  return {
    name: e?.name,
    message: e?.message ?? String(e),
    stack: e?.stack,
    cause: e?.cause === void 0 ? void 0 : reduceError(e.cause)
  };
}
__name(reduceError, "reduceError");
var jsonError = /* @__PURE__ */ __name(async (request, env, _ctx, middlewareCtx) => {
  try {
    return await middlewareCtx.next(request, env);
  } catch (e) {
    const error = reduceError(e);
    return Response.json(error, {
      status: 500,
      headers: { "MF-Experimental-Error-Stack": "true" }
    });
  }
}, "jsonError");
var middleware_miniflare3_json_error_default = jsonError;

// .wrangler/tmp/bundle-s7J5gk/middleware-insertion-facade.js
var __INTERNAL_WRANGLER_MIDDLEWARE__ = [
  middleware_ensure_req_body_drained_default,
  middleware_miniflare3_json_error_default
];
var middleware_insertion_facade_default = src_default;

// node_modules/wrangler/templates/middleware/common.ts
var __facade_middleware__ = [];
function __facade_register__(...args) {
  __facade_middleware__.push(...args.flat());
}
__name(__facade_register__, "__facade_register__");
function __facade_invokeChain__(request, env, ctx, dispatch, middlewareChain) {
  const [head, ...tail] = middlewareChain;
  const middlewareCtx = {
    dispatch,
    next(newRequest, newEnv) {
      return __facade_invokeChain__(newRequest, newEnv, ctx, dispatch, tail);
    }
  };
  return head(request, env, ctx, middlewareCtx);
}
__name(__facade_invokeChain__, "__facade_invokeChain__");
function __facade_invoke__(request, env, ctx, dispatch, finalMiddleware) {
  return __facade_invokeChain__(request, env, ctx, dispatch, [
    ...__facade_middleware__,
    finalMiddleware
  ]);
}
__name(__facade_invoke__, "__facade_invoke__");

// .wrangler/tmp/bundle-s7J5gk/middleware-loader.entry.ts
var __Facade_ScheduledController__ = class ___Facade_ScheduledController__ {
  constructor(scheduledTime, cron, noRetry) {
    this.scheduledTime = scheduledTime;
    this.cron = cron;
    this.#noRetry = noRetry;
  }
  static {
    __name(this, "__Facade_ScheduledController__");
  }
  #noRetry;
  noRetry() {
    if (!(this instanceof ___Facade_ScheduledController__)) {
      throw new TypeError("Illegal invocation");
    }
    this.#noRetry();
  }
};
function wrapExportedHandler(worker) {
  if (__INTERNAL_WRANGLER_MIDDLEWARE__ === void 0 || __INTERNAL_WRANGLER_MIDDLEWARE__.length === 0) {
    return worker;
  }
  for (const middleware of __INTERNAL_WRANGLER_MIDDLEWARE__) {
    __facade_register__(middleware);
  }
  const fetchDispatcher = /* @__PURE__ */ __name(function(request, env, ctx) {
    if (worker.fetch === void 0) {
      throw new Error("Handler does not export a fetch() function.");
    }
    return worker.fetch(request, env, ctx);
  }, "fetchDispatcher");
  return {
    ...worker,
    fetch(request, env, ctx) {
      const dispatcher = /* @__PURE__ */ __name(function(type, init) {
        if (type === "scheduled" && worker.scheduled !== void 0) {
          const controller = new __Facade_ScheduledController__(
            Date.now(),
            init.cron ?? "",
            () => {
            }
          );
          return worker.scheduled(controller, env, ctx);
        }
      }, "dispatcher");
      return __facade_invoke__(request, env, ctx, dispatcher, fetchDispatcher);
    }
  };
}
__name(wrapExportedHandler, "wrapExportedHandler");
function wrapWorkerEntrypoint(klass) {
  if (__INTERNAL_WRANGLER_MIDDLEWARE__ === void 0 || __INTERNAL_WRANGLER_MIDDLEWARE__.length === 0) {
    return klass;
  }
  for (const middleware of __INTERNAL_WRANGLER_MIDDLEWARE__) {
    __facade_register__(middleware);
  }
  return class extends klass {
    #fetchDispatcher = /* @__PURE__ */ __name((request, env, ctx) => {
      this.env = env;
      this.ctx = ctx;
      if (super.fetch === void 0) {
        throw new Error("Entrypoint class does not define a fetch() function.");
      }
      return super.fetch(request);
    }, "#fetchDispatcher");
    #dispatcher = /* @__PURE__ */ __name((type, init) => {
      if (type === "scheduled" && super.scheduled !== void 0) {
        const controller = new __Facade_ScheduledController__(
          Date.now(),
          init.cron ?? "",
          () => {
          }
        );
        return super.scheduled(controller);
      }
    }, "#dispatcher");
    fetch(request) {
      return __facade_invoke__(
        request,
        this.env,
        this.ctx,
        this.#dispatcher,
        this.#fetchDispatcher
      );
    }
  };
}
__name(wrapWorkerEntrypoint, "wrapWorkerEntrypoint");
var WRAPPED_ENTRY;
if (typeof middleware_insertion_facade_default === "object") {
  WRAPPED_ENTRY = wrapExportedHandler(middleware_insertion_facade_default);
} else if (typeof middleware_insertion_facade_default === "function") {
  WRAPPED_ENTRY = wrapWorkerEntrypoint(middleware_insertion_facade_default);
}
var middleware_loader_entry_default = WRAPPED_ENTRY;
export {
  __INTERNAL_WRANGLER_MIDDLEWARE__,
  middleware_loader_entry_default as default
};
//# sourceMappingURL=index.js.map
