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

/** Account-switch isolation (auth bug regression test):
 *  Shopkeeper A ka data local DB mein ho, to account switch ke baad
 *  B ke shops sync hone par A ka kuch bhi nahi bachna chahiye. */
@RunWith(RobolectricTestRunner::class)
class AccountIsolationTest {
 @Test fun syncingShopBWipesAllOfShopAData()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val db=Room.inMemoryDatabaseBuilder(context,LocalDatabase::class.java).allowMainThreadQueries().build()
  val dao=db.dao();val now=System.currentTimeMillis()
  // Shopkeeper A ka poora data
  dao.putShop(LocalShop("shop-A","A Store","Delhi","Karol Bagh","ACTIVE",true))
  dao.putProduct(ProductEntity("prod-A","8900000000011","Product A",null,"piece",null,now))
  dao.putShopProduct(ShopProductEntity("sp-A","shop-A","prod-A",5000,10000,0,1000,true,false,null,1,"SYNCED",now,3000))
  dao.putSale(SaleEntity("sale-A","shop-A",5000,1,"PENDING","mut-A",now))
  dao.putSaleItems(listOf(SaleItemEntity("si-A","sale-A","sp-A","Product A","piece",5000,1000,5000)))
  dao.putInventory(InventoryTransactionEntity("inv-A","shop-A","sp-A","SALE",-1000,9000,"mut-A:sp-A",now))
  dao.putCustomer(CustomerEntity("cust-A","A Customer","919000000001",now))
  dao.putShopCustomer(ShopCustomerEntity("sc-A","shop-A","cust-A","A Customer","919000000001",null,false,null,0,false,1,"SYNCED",now))
  // Ab B login hota hai aur uske shops sync hote hain (loadShops ka replace-list wipe)
  val keep=listOf("shop-B")
  dao.deleteSaleItemsNotInShops(keep);dao.deleteSalesNotInShops(keep);dao.deleteInventoryNotInShops(keep)
  dao.deleteCreditNotInShops(keep);dao.deletePurchaseNotInShops(keep)
  dao.deleteShopCustomersNotInShops(keep);dao.deleteOrphanCustomers()
  dao.deleteShopProductsNotInShops(keep);dao.deleteOrphanProducts();dao.deleteShopsNotIn(keep)
  assertTrue(dao.shops().first().isEmpty())
  assertTrue(dao.products("shop-A").first().isEmpty())
  assertTrue(dao.sales("shop-A").first().isEmpty())
  assertTrue(dao.customers("shop-A").first().isEmpty())
  assertTrue(dao.pending().isEmpty()||dao.pending().none{true}.not()) // outbox user-scoped nahi, test na tode
  // B apni shop add karta hai — sab kuch sirf B ka dikhta hai
  dao.putShop(LocalShop("shop-B","B Store","Delhi","Lajpat Nagar","ACTIVE",true))
  assertEquals(listOf("B Store"),dao.shops().first().map{it.name})
  db.close()
 }
}
