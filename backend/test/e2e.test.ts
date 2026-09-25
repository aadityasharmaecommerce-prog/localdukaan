import { env, createExecutionContext, waitOnExecutionContext, applyD1Migrations } from 'cloudflare:test';
import { beforeAll, describe, expect, it } from 'vitest';
import worker from '../src/index';
import sql from '../migrations/0001_phase1.sql?raw';
import catalogueSql from '../migrations/0002_catalogue.sql?raw';
import salesSql from '../migrations/0003_inventory_sales.sql?raw';
import creditSql from '../migrations/0004_customers_credit.sql?raw';
import marketSql from '../migrations/0005_marketplace.sql?raw';
import imagesSql from '../migrations/0006_product_images.sql?raw';
import seedSql from '../migrations/0007_seed_india_products.sql?raw';
import seed2Sql from '../migrations/0008_seed_india_products_more.sql?raw';
import oneShopSql from '../migrations/0009_one_shop_per_owner.sql?raw';
import posSql from '../migrations/0010_pos_upi_discount.sql?raw';
const migration={name:'0001_phase1.sql',queries:sql.split(';').map(x=>x.trim()).filter(Boolean)};
const catalogueMigration={name:'0002_catalogue.sql',queries:catalogueSql.split(';').map(x=>x.trim()).filter(Boolean)};
const salesMigration={name:'0003_inventory_sales.sql',queries:salesSql.split(';').map(x=>x.trim()).filter(Boolean)};
const creditMigration={name:'0004_customers_credit.sql',queries:creditSql.split(';').map(x=>x.trim()).filter(Boolean)};
const marketMigration={name:'0005_marketplace.sql',queries:marketSql.split(';').map(x=>x.trim()).filter(Boolean)};
const imagesMigration={name:'0006_product_images.sql',queries:imagesSql.split(';').map(x=>x.trim()).filter(Boolean)};
const seedMigration={name:'0007_seed_india_products.sql',queries:seedSql.split(';').map(x=>x.trim()).filter(Boolean)};
const seed2Migration={name:'0008_seed_india_products_more.sql',queries:seed2Sql.split(';').map(x=>x.trim()).filter(Boolean)};
const oneShopMigration={name:'0009_one_shop_per_owner.sql',queries:oneShopSql.split(';').map(x=>x.trim()).filter(Boolean)};
const posMigration={name:'0010_pos_upi_discount.sql',queries:posSql.split(';').map(x=>x.trim()).filter(Boolean)};
declare module 'cloudflare:test' { interface ProvidedEnv { DB:D1Database; MEDIA:R2Bucket; ENVIRONMENT:string; ACCESS_TTL_SECONDS:string; REFRESH_TTL_SECONDS:string; PIN_PEPPER:string; PIN_ITERATIONS:string } }
const call=async(path:string,init:RequestInit={})=>{const req=new Request(`https://api.test${path}`,{...init,headers:{'content-type':'application/json','cf-connecting-ip':'10.9.9.9',...(init.headers||{})}}),ctx=createExecutionContext();const r=await worker.fetch(req,env);await waitOnExecutionContext(ctx);return r};
const auth=(t:string)=>({authorization:`Bearer ${t}`});
const must=async(r:Response,want:number,label:string)=>{const b=await r.json() as any;if(r.status!==want)throw new Error(`E2E ${label}: got ${r.status} want ${want} body=${JSON.stringify(b)}`);return b};
beforeAll(async()=>{await applyD1Migrations(env.DB,[migration,catalogueMigration,salesMigration,creditMigration,marketMigration,imagesMigration,seedMigration,seed2Migration,oneShopMigration,posMigration]);(env as any).ENVIRONMENT='test';(env as any).PIN_PEPPER='e2e-pepper';(env as any).PIN_ITERATIONS='100000';(env as any).ACCESS_TTL_SECONDS='900';(env as any).REFRESH_TTL_SECONDS='2592000'});

// Full journey in ONE test: vitest-pool-workers gives each test a fresh D1 clone, so state must live within a single test.
describe('E2E: shopkeeper + customer journey',()=>{
 it('runs the full app journey end to end',async()=>{
  const uid=()=>crypto.randomUUID();
  // 1. Shopkeeper registers exactly like the Android app
  const reg=await call('/v1/auth/register',{method:'POST',body:JSON.stringify({phone:'+919300000001',pin:'1234',confirmPin:'1234',role:'SHOPKEEPER',deviceName:'Android'})});
  const rb=await must(reg,201,'register');expect(rb.accessToken).toMatch(/^ld_a_/);const sk=rb.accessToken;
  // 2. Profile save with app field names
  await must(await call('/v1/profile',{method:'PUT',headers:auth(sk),body:JSON.stringify({displayName:'Prem',preferredLanguage:'en',isCustomer:true,isShopkeeper:true})}),200,'profile');
  // 3. Shop creation: ACTIVE immediately (no approval dead-end) + TRIAL
  const shop=await must(await call('/v1/shops',{method:'POST',headers:auth(sk),body:JSON.stringify({mutationId:uid(),name:'E2E Kirana',ownerName:'Prem',addressLine:'1 Market Road',locality:'Bazaar',city:'Indore',pincode:'452001',category:'GROCERY',latitude:22.7196,longitude:75.8577})}),201,'shop');
  expect(shop.status).toBe('ACTIVE');expect(shop.subscription.status).toBe('TRIAL');
  const shopId=shop.id;
  // 4. Product add
  const prod=await must(await call(`/v1/shops/${shopId}/products`,{method:'POST',headers:auth(sk),body:JSON.stringify({mutationId:uid(),name:'Aata 5kg',unit:'kg',sellingPricePaise:25000,openingStockMilli:20000,minimumStockMilli:5000,isAvailable:true})}),201,'product');
  const spid=prod.shopProductId;expect(prod.stockQuantityMilli).toBe(20000);
  // 5. PAID quick sale: server prices, stock decrement
  const sale=await must(await call(`/v1/shops/${shopId}/sales`,{method:'POST',headers:auth(sk),body:JSON.stringify({mutationId:uid(),paymentType:'PAID',items:[{shopProductId:spid,quantityMilli:2000}]})}),201,'paid-sale');
  expect(sale.paymentType).toBe('PAID');expect(sale.status).toBe('COMPLETED');expect(sale.subtotalPaise).toBe(50000);
  expect((await env.DB.prepare('SELECT stock_quantity_milli s FROM shop_products WHERE id=?').bind(spid).first<any>()).s).toBe(18000);
  // 6. Customer + CREDIT sale (udhari)
  const cust=await must(await call(`/v1/shops/${shopId}/customers`,{method:'POST',headers:auth(sk),body:JSON.stringify({mutationId:uid(),name:'Rahul',creditEnabled:true,creditLimitPaise:100000})}),201,'customer');
  const scid=cust.shopCustomerId;
  const csale=await must(await call(`/v1/shops/${shopId}/sales`,{method:'POST',headers:auth(sk),body:JSON.stringify({mutationId:uid(),paymentType:'CREDIT',shopCustomerId:scid,items:[{shopProductId:spid,quantityMilli:1000}]})}),201,'credit-sale');
  expect(csale.paymentType).toBe('CREDIT');
  expect((await must(await call(`/v1/shops/${shopId}/customers/${scid}`,{headers:auth(sk)}),200,'get-customer')).current_balance_paise).toBe(25000);
  // 7. Publish shop -> marketplace discovery
  const p=await must(await call(`/v1/shops/${shopId}/profile`,{headers:auth(sk)}),200,'shop-profile');
  const up=await must(await call(`/v1/shops/${shopId}/profile`,{method:'PUT',headers:auth(sk),body:JSON.stringify({mutationId:uid(),version:p.version,name:'E2E Kirana',ownerName:'Prem',addressLine:'1 Market Road',locality:'Bazaar',city:'Indore',pincode:'452001',isPublished:true})}),200,'shop-publish');
  expect(up.isPublished).toBe(true);
  const mkb=await must(await call('/v1/market/shops',{headers:auth(sk)}),200,'market-list');
  if(!mkb.items.some((x:any)=>x.id===shopId)){const direct=await env.DB.prepare(`SELECT s.id,s.name,s.category,s.locality,s.city,s.status,s.is_published,s.accepts_online_orders FROM shops s JOIN shop_delivery_settings d ON d.shop_id=s.id WHERE s.id=?`).bind(shopId).first<any>();const emptyQ=await call('/v1/market/shops',{});throw new Error('MARKET_LIST '+JSON.stringify(mkb).slice(0,80)+' ROW '+JSON.stringify(direct)+' QAPI '+JSON.stringify(await (await call('/v1/market/shops?q=E2E',{})).json()).slice(0,150));}
  // 8. Customer registers, saves address, places PICKUP order
  const cu=await must(await call('/v1/auth/register',{method:'POST',body:JSON.stringify({phone:'+919300000002',pin:'1234',confirmPin:'1234',role:'CUSTOMER'})}),201,'customer-register');
  const ct=cu.accessToken;
  const addr=await must(await call('/v1/addresses',{method:'POST',headers:auth(ct),body:JSON.stringify({mutationId:uid(),recipientName:'Anu',mobile:'+919300000002',addressLine:'2 Park Street',locality:'Vijay Nagar',city:'Indore',pincode:'452010'})}),201,'address');
  const order=await must(await call('/v1/orders',{method:'POST',headers:auth(ct),body:JSON.stringify({mutationId:uid(),shopId,fulfillmentType:'PICKUP',items:[{shopProductId:spid,quantityMilli:1000}]})}),201,'order');
  const oid=order.orderId;expect(order.status).toBe('PENDING');expect(order.totalPaise).toBeGreaterThan(0);
  // 9. Shopkeeper accepts then delivers; stock decrements, reservation releases
  await must(await call(`/v1/shops/${shopId}/orders/${oid}/status`,{method:'POST',headers:auth(sk),body:JSON.stringify({mutationId:uid(),status:'ACCEPTED'})}),200,'order-accept');
  await must(await call(`/v1/shops/${shopId}/orders/${oid}/status`,{method:'POST',headers:auth(sk),body:JSON.stringify({mutationId:uid(),status:'PACKING'})}),200,'order-packing');
  await must(await call(`/v1/shops/${shopId}/orders/${oid}/status`,{method:'POST',headers:auth(sk),body:JSON.stringify({mutationId:uid(),status:'READY'})}),200,'order-ready');
  await must(await call(`/v1/shops/${shopId}/orders/${oid}/status`,{method:'POST',headers:auth(sk),body:JSON.stringify({mutationId:uid(),status:'OUT_FOR_DELIVERY'})}),200,'order-ofd');
  await must(await call(`/v1/shops/${shopId}/orders/${oid}/status`,{method:'POST',headers:auth(sk),body:JSON.stringify({mutationId:uid(),status:'DELIVERED'})}),200,'order-deliver');
  const g2=await env.DB.prepare('SELECT stock_quantity_milli s,reserved_quantity_milli r FROM shop_products WHERE id=?').bind(spid).first<any>();
  expect(g2!.s).toBe(16000);expect(g2!.r).toBe(0);
  // 10. Edge cases: oversell 409, wrong PIN 401, duplicate register 409, invalid PIN 400, no-auth 401
  expect((await call(`/v1/shops/${shopId}/sales`,{method:'POST',headers:auth(sk),body:JSON.stringify({mutationId:uid(),paymentType:'PAID',items:[{shopProductId:spid,quantityMilli:999999999}]})})).status).toBe(409);
  expect((await call('/v1/auth/login',{method:'POST',body:JSON.stringify({phone:'+919300000001',pin:'9999'})})).status).toBe(401);
  expect((await call('/v1/auth/register',{method:'POST',body:JSON.stringify({phone:'+919300000001',pin:'1234',confirmPin:'1234',role:'SHOPKEEPER'})})).status).toBe(409);
  expect((await call('/v1/auth/register',{method:'POST',body:JSON.stringify({phone:'+919300000003',pin:'12',confirmPin:'12',role:'CUSTOMER'})})).status).toBe(400);
  expect((await call('/v1/shops/mine')).status).toBe(401);
 });
});
