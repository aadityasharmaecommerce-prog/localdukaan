package `in`.localdukaan
import `in`.localdukaan.core.model.*
import org.junit.Assert.*
import org.junit.Test
class ProductRulesTest {
 private fun valid()=ProductDraft("Milk","Amul"," 890-123456 ","","litre","349.00","10.5","2","Fresh",true)
 @Test fun convertsPriceToIntegerPaise(){assertEquals(34900L,ProductRules.priceToPaise("349.00"));assertEquals(50L,ProductRules.priceToPaise("0.50"))}
 @Test fun rejectsInvalidPrice(){assertNull(ProductRules.priceToPaise("-1"));assertNull(ProductRules.priceToPaise("1.999"));assertNull(ProductRules.priceToPaise("abc"))}
 @Test fun validatesStockAndMinimum(){assertEquals(10500L,ProductRules.quantityToMilli("10.5"));assertNull(ProductRules.quantityToMilli("-1"));assertFalse(ProductRules.validate(valid().copy(minimumStockText="-2")).valid)}
 @Test fun normalizesAndValidatesBarcode(){assertEquals("890123456",ProductRules.normalizeBarcode(" 890-123456 "));assertFalse(ProductRules.validate(valid().copy(barcode="!!")).valid);assertTrue(ProductRules.validate(valid().copy(barcode="")).valid)}
 @Test fun rejectsEmptyName(){assertFalse(ProductRules.validate(valid().copy(name=" ")).valid)}
 @Test fun calculatesLowStockInclusively(){assertTrue(ProductRules.isLowStock(5_000,10_000));assertTrue(ProductRules.isLowStock(10_000,10_000));assertFalse(ProductRules.isLowStock(10_001,10_000))}
}
