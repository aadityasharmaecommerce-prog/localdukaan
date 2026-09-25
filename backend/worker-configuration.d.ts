interface Env {
 DB: D1Database; MEDIA?: R2Bucket; NOTIFICATION_QUEUE?: Queue;
 ENVIRONMENT: string; ACCESS_TTL_SECONDS: string; REFRESH_TTL_SECONDS: string;
 PIN_PEPPER:string; PIN_ITERATIONS?:string;
}
declare module '*.sql?raw' { const sql: string; export default sql; }
