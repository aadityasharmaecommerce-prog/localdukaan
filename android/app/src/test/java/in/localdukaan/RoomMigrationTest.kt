package `in`.localdukaan
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import `in`.localdukaan.core.database.LocalDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomMigrationTest {
 @Test fun migrateV1ToV2PreservesDataAndCreatesCatalogueTables()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>();val name="migration-test.db";context.deleteDatabase(name)
  val helper=FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object:SupportSQLiteOpenHelper.Callback(1){override fun onCreate(db:SupportSQLiteDatabase){
   db.execSQL("CREATE TABLE IF NOT EXISTS local_profile (userId TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, language TEXT NOT NULL, customer INTEGER NOT NULL, shopkeeper INTEGER NOT NULL, synced INTEGER NOT NULL)")
   db.execSQL("CREATE TABLE IF NOT EXISTS local_shop (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, city TEXT NOT NULL, locality TEXT NOT NULL, status TEXT NOT NULL, synced INTEGER NOT NULL)")
   db.execSQL("CREATE TABLE IF NOT EXISTS outbox (mutationId TEXT NOT NULL PRIMARY KEY, operation TEXT NOT NULL, payload TEXT NOT NULL, state TEXT NOT NULL, createdAt INTEGER NOT NULL)")
   db.execSQL("INSERT INTO local_shop VALUES('shop-1','Existing Shop','Ghaziabad','Indirapuram','ACTIVE',1)")
   db.execSQL("INSERT INTO local_profile VALUES('user-1','Owner','en',0,1,1)")
  };override fun onUpgrade(db:SupportSQLiteDatabase,oldVersion:Int,newVersion:Int){}}).build());helper.writableDatabase;helper.close()
  val room=Room.databaseBuilder(context,LocalDatabase::class.java,name).addMigrations(LocalDatabase.MIGRATION_1_2,LocalDatabase.MIGRATION_2_3,LocalDatabase.MIGRATION_3_4,LocalDatabase.MIGRATION_4_5,LocalDatabase.MIGRATION_5_6).allowMainThreadQueries().build()
  val shops=room.dao().shops().first();assertEquals(1,shops.size);assertEquals("Existing Shop",shops.single().name);assertEquals("",shops.single().ownerName)
  val tables=room.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('products','shop_products','inventory_transactions')").use{c->buildSet{while(c.moveToNext())add(c.getString(0))}}
  assertEquals(setOf("products","shop_products","inventory_transactions"),tables);assertTrue(room.dao().products("shop-1").first().isEmpty());room.close()
 }
 @Test fun catalogueReadsFromRoomAndPendingImageOutboxSurvivesOffline()=runBlocking {val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).allowMainThreadQueries().build();val dao=db.dao();val now=System.currentTimeMillis();dao.putProduct(`in`.localdukaan.core.database.ProductEntity("p1",null,"Offline Milk",null,"litre",null,now));dao.putShopProduct(`in`.localdukaan.core.database.ShopProductEntity("sp1","shop1","p1",6500,5000,0,10000,true,false,"local-file:/private/pending",1L,"PENDING",now));dao.enqueue(`in`.localdukaan.core.database.Outbox("m1","UPLOAD_PRODUCT_IMAGE","{\"filePath\":\"/private/pending\"}"));val rows=dao.products("shop1").first();assertEquals("Offline Milk",rows.single().product.name);assertTrue(rows.single().lowStock);assertEquals("PENDING",rows.single().shopProduct.syncState);assertEquals(1,dao.pending().size);dao.putSale(`in`.localdukaan.core.database.SaleEntity("sale-local","shop1",1000L,1,"PENDING","sale-mutation",now));dao.putSaleItems(listOf(`in`.localdukaan.core.database.SaleItemEntity("line1","sale-local","sp1","Offline Milk","litre",1000L,1000L,1000L)));dao.enqueue(`in`.localdukaan.core.database.Outbox("sale-mutation","QUICK_SALE","{}"));assertEquals("PENDING",dao.sales("shop1").first().single().status);assertEquals(2,dao.pending().size);dao.putCustomer(`in`.localdukaan.core.database.CustomerEntity("c1","Rahul","919800000001",now));dao.putShopCustomer(`in`.localdukaan.core.database.ShopCustomerEntity("sc1","shop1","c1","Rahul","919800000001","Regular",true,50000L,10000L,false,1L,"SYNCED",now));dao.putCredit(`in`.localdukaan.core.database.CreditTransactionEntity("ct1","shop1","sc1","CREDIT_SALE",10000L,10000L,"SALE","sale-local",null,"cm1","PENDING",now));dao.enqueue(`in`.localdukaan.core.database.Outbox("cm1","CREDIT_SALE","{}"));assertEquals(40000L,dao.customers("shop1").first().single().availableCreditPaise);assertEquals("PENDING",dao.creditStatement("shop1","sc1").first().single().status);assertEquals(3,dao.pending().size);db.close()}
}
