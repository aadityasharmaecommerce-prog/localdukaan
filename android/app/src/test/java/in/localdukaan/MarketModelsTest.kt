package `in`.localdukaan
import `in`.localdukaan.core.model.*
import org.junit.Assert.*
import org.junit.Test
class MarketModelsTest {
 private fun product(id:String="sp1",price:Long=1000)=MarketProduct(id,"Item",null,"piece",price,10000)
 @Test fun cartAggregatesRepeatedAddsIntoOneLine(){val c=Cart().add(product()).add(product());assertEquals(1,c.lines.size);assertEquals(2000,c.lines.single().quantityMilli);assertEquals(2000,c.subtotalPaise)}
 @Test fun cartQuantityChangeAndRemovalWork(){val c=Cart().add(product()).setQuantity("sp1",3000);assertEquals(3000,c.lines.single().quantityMilli);assertTrue(c.setQuantity("sp1",0).isEmpty)}
 @Test fun statusTransitionsMatchServerRules(){assertTrue(OrderStatusFlow.canTransition("PENDING","ACCEPTED"));assertTrue(OrderStatusFlow.canTransition("READY","DELIVERED"));assertFalse(OrderStatusFlow.canTransition("PENDING","DELIVERED"));assertFalse(OrderStatusFlow.canTransition("DELIVERED","CANCELLED"));assertTrue(OrderStatusFlow.isTerminal("DELIVERED"));assertFalse(OrderStatusFlow.isTerminal("PENDING"));assertEquals(setOf("ACCEPTED","REJECTED","CANCELLED"),OrderStatusFlow.next("PENDING"))}
}
