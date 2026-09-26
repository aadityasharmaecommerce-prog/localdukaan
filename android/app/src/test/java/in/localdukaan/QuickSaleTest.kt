package `in`.localdukaan
import `in`.localdukaan.core.database.*
import `in`.localdukaan.core.model.*
import org.junit.Assert.*
import org.junit.Test
class QuickSaleTest {
 private fun row(id:String="sp1",price:Long=34900,stock:Long=10000,cost:Long=30000)=ProductRow(ShopProductEntity(id,"shop","p-$id",price,stock,0,2000,true,false,null,1,"SYNCED",0,costPricePaise=cost),ProductEntity("p-$id","8901234567890","Milk",null,"piece",null,0))
 @Test fun repeatedScansAggregateOneLine(){val s=QuickSaleState().scan(row()).scan(row()).scan(row());assertEquals(1,s.lines.size);assertEquals(3000,s.lines["sp1"]!!.quantityMilli)}
 @Test fun calculatesIntegerPaiseTotals(){val s=QuickSaleState().scan(row(price=34900)).scan(row(price=34900));assertEquals(69800,s.subtotalPaise)}
 @Test fun quantityChangesAndRemovalWork(){val s=QuickSaleState().scan(row()).setQuantity("sp1",5000);assertEquals(5000,s.lines["sp1"]!!.quantityMilli);assertTrue(s.setQuantity("sp1",0).lines.isEmpty())}
 @Test fun warnsWhenLocalStockIsInsufficient(){val s=QuickSaleState().scan(row(stock=1000)).scan(row(stock=1000));assertTrue(s.hasLocalStockWarning(listOf(row(stock=1000))))}
 @Test fun scannerGateDebouncesSameFrameButAllowsLaterOrDifferent(){assertFalse(ScannerGate.accept("123456","123456",1000,1500));assertTrue(ScannerGate.accept("123456","123456",1000,1900));assertTrue(ScannerGate.accept("654321","123456",1000,1100))}
 @Test fun walkInIsDefaultAndCashIsDefault(){val s=QuickSaleState().scan(row());assertTrue(s.walkIn);assertEquals(PaymentMethod.CASH,s.method);assertTrue(s.canComplete);assertFalse(s.udhaariRequiresCustomer)}
 @Test fun udhaariWithoutCustomerIsBlocked(){var s=QuickSaleState().scan(row());s=s.selectMethod(PaymentMethod.CREDIT);assertTrue(s.udhaariRequiresCustomer);assertFalse(s.canComplete);s=s.identify("Rahul","9876543210");assertFalse(s.udhaariRequiresCustomer);assertTrue(s.canComplete)}
 @Test fun udhaariClearedWhenBackToWalkIn(){val s=QuickSaleState().scan(row()).selectMethod(PaymentMethod.CREDIT).identify("Rahul","9876543210").walkInCustomer();assertTrue(s.udhaariRequiresCustomer);assertFalse(s.canComplete)}
 @Test fun cashAndUpiWorkWithWalkIn(){val cash=QuickSaleState().scan(row()).selectMethod(PaymentMethod.CASH);assertTrue(cash.canComplete);val upi=QuickSaleState().scan(row()).selectMethod(PaymentMethod.UPI);assertTrue(upi.canComplete)}
 @Test fun discountReducesTotalButNeverBelowZero(){val s=QuickSaleState().scan(row(price=5000));assertEquals(3000L,s.setDiscount(2000).totalPaise);assertEquals(0L,s.setDiscount(99999).totalPaise)}
 @Test fun costSnapshotDrivesProfitEstimate(){val s=QuickSaleState().scan(row(price=5000,cost=3000));assertEquals(2000L,s.lines.values.first().lineTotalPaise-s.lines.values.first().lineCostPaise)}
 @Test fun scanCarriesCostPrice(){val s=QuickSaleState().scan(row(cost=12345));assertEquals(12345L,s.lines.values.first().costPricePaise)}
 @Test fun parleGTwoPiecesProfitBreakdown(){val s=QuickSaleState().scan(row(id="pg",price=1000,cost=500)).scan(row(id="pg",price=1000,cost=500));assertEquals(2000L,s.subtotalPaise);assertEquals(1000L,s.estimatedProfitPaise);assertEquals(1000L,s.profitableCostPaise);assertTrue(s.allLinesCosted);assertEquals(1000L,s.subtotalPaise-s.profitableCostPaise)}
 @Test fun zeroCostLinesExcludedFromProfit(){val s=QuickSaleState().scan(row(id="a",price=1000,cost=0)).scan(row(id="b",price=1000,cost=500));assertEquals(500L,s.estimatedProfitPaise);assertFalse(s.allLinesCosted)}
 @Test fun identifyTrimsAndBlankMeansWalkIn(){val s=QuickSaleState().identify("  Rahul  "," 9876543210 ");assertEquals("Rahul",s.customerName);assertEquals("9876543210",s.customerPhone);assertFalse(s.walkIn);val w=QuickSaleState().identify("","");assertTrue(w.walkIn)}
}
