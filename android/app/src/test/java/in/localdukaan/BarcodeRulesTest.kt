package `in`.localdukaan
import `in`.localdukaan.core.model.BarcodeRules
import org.junit.Assert.*
import org.junit.Test
class BarcodeRulesTest {
 @Test fun acceptsRealGroceryAndCosmeticSymbols(){
  assertTrue(BarcodeRules.checkDigitOk("8901719134845")) // Parle-G EAN-13
  assertTrue(BarcodeRules.checkDigitOk("96385074"))      // EAN-8
  assertTrue(BarcodeRules.checkDigitOk("036000291452"))  // UPC-A
  assertTrue(BarcodeRules.checkDigitOk("00036000291452"))// same pack as GTIN-14
 }
 @Test fun rejectsHalfReadFrames(){
  assertFalse(BarcodeRules.checkDigitOk("8901000000005"))
  assertFalse(BarcodeRules.checkDigitOk("890171913484"))
  assertFalse(BarcodeRules.checkDigitOk("12345"))
  assertFalse(BarcodeRules.checkDigitOk(""))
  assertFalse(BarcodeRules.checkDigitOk(null))
 }
 @Test fun givesEveryEquivalentFormOfTheNumber(){
  assertEquals(listOf("036000291452","0036000291452","00036000291452"),BarcodeRules.candidates(" 036-000 291452 "))
  assertEquals(listOf("8901719134845","08901719134845"),BarcodeRules.candidates("8901719134845"))
  assertEquals(listOf("00036000291452","0036000291452"),BarcodeRules.candidates("00036000291452"))
 }
 @Test fun stripsJunkFrom2dSymbols(){
  assertEquals("8901719134845",BarcodeRules.extractProductCode("https://fssai.example.in/f?gtin=8901719134845"))
  assertEquals("8901719134845",BarcodeRules.extractProductCode("010890171913484521ABC"))
  assertNull(BarcodeRules.extractProductCode("https://example.com/no-code-here"))
 }
 @Test fun onlySendsCatalogueWorthyValues(){
  assertTrue(BarcodeRules.isAcceptable("8901719134845"))
  assertTrue(BarcodeRules.isAcceptable("AB1234"))
  assertTrue(BarcodeRules.isAcceptable("01234567")) // UPC-E, invalid as EAN-8 but a real symbol
  assertFalse(BarcodeRules.isAcceptable("https://example.com"))
  assertFalse(BarcodeRules.isAcceptable(null))
  assertFalse(BarcodeRules.isAcceptable("1234"))
 }
}
