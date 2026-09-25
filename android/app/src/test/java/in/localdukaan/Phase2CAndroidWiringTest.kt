package `in`.localdukaan
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import `in`.localdukaan.core.database.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Phase2CAndroidWiringTest {
 private fun db():LocalDatabase=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),LocalDatabase::class.java).allowMainThreadQueries().build()
 private fun customer(id:String="sc1",balance:Long=0,limit:Long?=50000,enabled:Boolean=true,archived:Boolean=false,version:Long=1)=ShopCustomerEntity(id,"shop1","c1","Rahul","919800000001","Regular",enabled,limit,balance,archived,version,"SYNCED",System.currentTimeMillis())

 @Test fun offlineCreditUpdatesBalanceQueuesOutboxAndShowsAvailableCredit()=runBlocking{
  val db=db();val dao=db.dao();val now=System.currentTimeMillis()
  dao.putCustomer(CustomerEntity("c1","Rahul","919800000001",now));dao.putShopCustomer(customer())
  val balanceAfter=10000L
  dao.putCredit(CreditTransactionEntity("local-credit-m1","shop1","sc1","CREDIT_SALE",10000L,balanceAfter,"SALE","local-sale-m1",null,"m1","PENDING",now))
  dao.setCreditBalance("sc1",balanceAfter,"PENDING",2L)
  dao.enqueue(Outbox("m1","CREDIT","{\"mutationId\":\"m1\",\"shopCustomerId\":\"sc1\",\"amountPaise\":10000,\"type\":\"CREDIT_SALE\"}"))
  val row=dao.customers("shop1").first().single()
  assertEquals(10000L,row.currentBalancePaise);assertEquals(40000L,row.availableCreditPaise);assertEquals("PENDING",row.syncState)
  assertEquals("PENDING",dao.creditStatement("shop1","sc1").first().single().status)
  assertEquals(1,dao.pending().size)
  db.close()
 }

 @Test fun permanentRejectionMarksCreditTransactionRejected()=runBlocking{
  val db=db();val dao=db.dao();val now=System.currentTimeMillis()
  dao.putCustomer(CustomerEntity("c1","Rahul",null,now));dao.putShopCustomer(customer())
  dao.putCredit(CreditTransactionEntity("local-credit-m2","shop1","sc1","PAYMENT",5000L,5000L,null,null,null,"m2","PENDING",now))
  dao.enqueue(Outbox("m2","CREDIT","{}"))
  dao.rejectCredit("m2")
  assertEquals("REJECTED",dao.creditStatement("shop1","sc1").first().single().status)
  db.close()
 }

 @Test fun archivedCustomerIsHiddenFromActiveList()=runBlocking{
  val db=db();val dao=db.dao()
  dao.putCustomer(CustomerEntity("c1","Rahul",null,System.currentTimeMillis()));dao.putShopCustomer(customer())
  assertEquals(1,dao.customers("shop1").first().size)
  dao.archiveCustomer("sc1")
  assertTrue(dao.customers("shop1").first().isEmpty())
  db.close()
 }

 @Test fun setCreditBalanceOnlyKeepsVersionForServerTruth()=runBlocking{
  val db=db();val dao=db.dao()
  dao.putCustomer(CustomerEntity("c1","Rahul",null,System.currentTimeMillis()));dao.putShopCustomer(customer(version=3))
  dao.setCreditBalanceOnly("sc1",25000L,"SYNCED")
  val row=dao.shopCustomer("sc1")!!
  assertEquals(25000L,row.currentBalancePaise);assertEquals(3L,row.version);assertEquals("SYNCED",row.syncState)
  db.close()
 }

 @Test fun reconcilingLocalCustomerRewritesPendingOutboxAndRemovesPlaceholders()=runBlocking{
  val db=db();val dao=db.dao()
  dao.putCustomer(CustomerEntity("local-customer-m3","New",null,System.currentTimeMillis()));dao.putShopCustomer(customer(id="local-shopcustomer-m3"))
  dao.enqueue(Outbox("m4","CREDIT","{\"shopCustomerId\":\"local-shopcustomer-m3\",\"amountPaise\":100}"))
  dao.replacePendingId("local-shopcustomer-m3","server-sc-9")
  assertTrue(dao.pending().single().payload.contains("server-sc-9"))
  dao.deleteShopCustomer("local-shopcustomer-m3");dao.deleteCustomer("local-customer-m3")
  assertNull(dao.shopCustomer("local-shopcustomer-m3"))
  db.close()
 }
}
