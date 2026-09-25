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
const call=async(path:string,init:RequestInit={})=>{const req=new Request(`https://api.test${path}`,{...init,headers:{'content-type':'application/json','cf-connecting-ip':'10.7.7.7',...(init.headers||{})}}),ctx=createExecutionContext();const r=await worker.fetch(req,env);await waitOnExecutionContext(ctx);return r};
const auth=(t:string)=>({authorization:`Bearer ${t}`});
beforeAll(async()=>{await applyD1Migrations(env.DB,[migration,catalogueMigration,salesMigration,creditMigration,marketMigration,imagesMigration,seedMigration,seed2Migration,oneShopMigration,posMigration]);(env as any).ENVIRONMENT='test';(env as any).PIN_PEPPER='pos-pepper';(env as any).PIN_ITERATIONS='100000';(env as any).ACCESS_TTL_SECONDS='900';(env as any).REFRESH_TTL_SECONDS='2592000'});

async function setup(phone:string){
  const reg=await call('/v1/auth/register',{method:'POST',body:JSON.stringify({phone,pin:'1234',confirmPin:'1234',role:'SHOPKEEPER',deviceName:'test'})});
  const owner=await reg.json() as any;
  const shopR=await call('/v1/shops',{method:'POST',headers:auth(owner.accessToken),body:JSON.stringify({mutationId:crypto.randomUUID(),name:'POS Shop',ownerName:'Owner',addressLine:'1 Road',locality:'Market',city:'Delhi',pincode:'110001',category:'Kirana',latitude:28.6,longitude:77.2})});
  const shop=await shopR.json() as any;
  const pR=await call(`/v1/shops/${shop.id}/products`,{method:'POST',headers:auth(owner.accessToken),body:JSON.stringify({mutationId:crypto.randomUUID(),name:'Surf',barcode:'8907000000001',unit:'piece',sellingPricePaise:10000,costPricePaise:8000,openingStockMilli:10000,minimumStockMilli:1000})});
  const p=await pR.json() as any;
  return {token:owner.accessToken as string,shop,p};
}
const sell=async(x:Awaited<ReturnType<typeof setup>>,body:any)=>{const r=await call(`/v1/shops/${x.shop.id}/sales`,{method:'POST',headers:auth(x.token),body:JSON.stringify({mutationId:crypto.randomUUID(),items:[{shopProductId:x.p.shopProductId,quantityMilli:1000}],...body})});return {status:r.status,data:await r.json() as any}};

describe('POS payment methods and discounts',()=>{
 it('accepts a UPI walk-in sale and stores payment_method',async()=>{
  const x=await setup('+919977000001');
  const s=await sell(x,{paymentType:'PAID',paymentMethod:'UPI'});
  expect(s.status).toBe(201);
  expect(s.data.paymentMethod).toBe('UPI');
  expect(s.data.totalPaise).toBe(10000);
  const row=await env.DB.prepare('SELECT payment_method,total_paise FROM sales WHERE id=?').bind(s.data.saleId).first<any>();
  expect(row.payment_method).toBe('UPI');
 });
 it('defaults to CASH when paymentMethod is absent (backward compatible)',async()=>{
  const x=await setup('+919977000002');
  const s=await sell(x,{});
  expect(s.status).toBe(201);
  const row=await env.DB.prepare('SELECT payment_method FROM sales WHERE id=?').bind(s.data.saleId).first<any>();
  expect(row.payment_method).toBe('CASH');
 });
 it('applies a discount to the total and rejects oversized discounts',async()=>{
  const x=await setup('+919977000003');
  const s=await sell(x,{discountPaise:2000});
  expect(s.status).toBe(201);
  expect(s.data.subtotalPaise).toBe(10000);
  expect(s.data.totalPaise).toBe(8000);
  expect(s.data.discountPaise).toBe(2000);
  const bad=await sell(x,{discountPaise:99999});
  expect(bad.status).toBe(400);
  expect(bad.data.error.code).toBe('INVALID_DISCOUNT');
 });
 it('credits the discounted amount for udhaari sales',async()=>{
  const x=await setup('+919977000004');
  const c=await call(`/v1/shops/${x.shop.id}/customers`,{method:'POST',headers:auth(x.token),body:JSON.stringify({mutationId:crypto.randomUUID(),name:'Rahul',mobile:'+919977000099',creditEnabled:true,creditLimitPaise:100000})});
  const cust=await c.json() as any;
  const s=await sell(x,{paymentType:'CREDIT',paymentMethod:'CREDIT',shopCustomerId:cust.shopCustomerId,discountPaise:1000});
  expect(s.status).toBe(201);
  expect(s.data.totalPaise).toBe(9000);
  const bal=await env.DB.prepare('SELECT current_balance_paise FROM shop_customers WHERE id=?').bind(cust.shopCustomerId).first<any>();
  expect(bal.current_balance_paise).toBe(9000);
 });
});
