package `in`.localdukaan
import `in`.localdukaan.core.model.*
import org.junit.Assert.*
import org.junit.Test
class Phase2CCreditTest{
 @Test fun parsesRupeesExactlyWithoutDouble(){assertEquals(12345L,MoneyParser.rupeesToPaise("123.45"));assertEquals(1L,MoneyParser.rupeesToPaise("0.01"));assertNull(MoneyParser.rupeesToPaise("1L.001"));assertNull(MoneyParser.rupeesToPaise("-1L"));assertNull(MoneyParser.rupeesToPaise("NaN"))}
 @Test fun validatesBalanceAndLimit(){assertEquals(4000L,CreditRules.available(10000L,6000L,true));assertTrue(CreditRules.validCredit(4000L,6000L,10000L,true));assertFalse(CreditRules.validCredit(4001L,6000L,10000L,true));assertTrue(CreditRules.validPayment(6000L,6000L));assertFalse(CreditRules.validPayment(6001L,6000L))}
 @Test fun creditQuickSaleRequiresCustomerAndPaidClearsIt(){val empty=QuickSaleState().selectPayment(SalePaymentMode.CREDIT);assertFalse(empty.canSubmit);assertNull(empty.shopCustomerId);val selected=empty.selectPayment(SalePaymentMode.CREDIT,"customer-1L");assertEquals("customer-1L",selected.shopCustomerId);val paid=selected.selectPayment(SalePaymentMode.PAID);assertEquals(SalePaymentMode.PAID,paid.paymentMode);assertNull(paid.shopCustomerId);val methodCleared=paid.selectMethod(PaymentMethod.CASH);assertNull(methodCleared.shopCustomerId)}
 @Test fun rejectedOfflineDraftKeepsVisibleReason(){val pending=CreditDraft("m1","c1","PAYMENT",1000L);val rejected=pending.copy(state="REJECTED",rejectionReason="Payment exceeds current balance");assertEquals("REJECTED",rejected.state);assertNotNull(rejected.rejectionReason);assertEquals(1000L,rejected.amountPaise)}
}
