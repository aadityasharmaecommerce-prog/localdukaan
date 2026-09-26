package `in`.localdukaan

import `in`.localdukaan.core.database.ProductEntity
import `in`.localdukaan.core.database.ProductRow
import `in`.localdukaan.core.database.ShopProductEntity
import `in`.localdukaan.core.model.VoiceCmd
import `in`.localdukaan.core.model.VoiceParser
import org.junit.Assert.*
import org.junit.Test

/** Voice parser — bina AI, sirf rules. Mic se pehle ye pakka. */
class VoiceCommandTest {
 private fun row(id: String, name: String, brand: String? = null, price: Long = 1000L, barcode: String? = null): ProductRow {
  val now = System.currentTimeMillis()
  return ProductRow(
   ShopProductEntity(id, "shop1", "p-$id", price, 5000, 0, 1000, true, false, null, 1, "SYNCED", now),
   ProductEntity("p-$id", barcode, name, brand, "packet", null, now)
  )
 }
 private val catalogue = listOf(
  row("sp1", "Parle-G Gold", "Parle", 1000L, "8901000000007"),
  row("sp2", "Parle Hide & Seek", "Parle", 3000L),
  row("sp3", "Amul Taaza Milk", "Amul", 6500L),
  row("sp4", "Tata Salt", "Tata", 2800L)
 )

 @Test fun digitQuantityWithSaleVerb() {
  val cmd = VoiceParser.parse("2 Parle G sale", catalogue) as VoiceCmd.Sale
  assertEquals("sp1", cmd.option.productId)
  assertEquals(2000L, cmd.option.quantityMilli)
 }

 @Test fun hindiQuantityAndTrailingVerb() {
  val doWala = VoiceParser.parse("do parle g bech do", catalogue) as VoiceCmd.Sale
  assertEquals("sp1", doWala.option.productId)
  assertEquals(2000L, doWala.option.quantityMilli)
  val ekWala = VoiceParser.parse("parle g de do", catalogue) as VoiceCmd.Sale
  assertEquals(1000L, ekWala.option.quantityMilli)
 }

 @Test fun defaultQuantityIsOne() {
  val cmd = VoiceParser.parse("Tata salt sale", catalogue) as VoiceCmd.Sale
  assertEquals("sp4", cmd.option.productId)
  assertEquals(1000L, cmd.option.quantityMilli)
 }

 @Test fun halfQuantity() {
  val cmd = VoiceParser.parse("aadha parle g sale", catalogue) as VoiceCmd.Sale
  assertEquals(500L, cmd.option.quantityMilli)
 }

 @Test fun ambiguousNameGivesChoices() {
  val cmd = VoiceParser.parse("parle sale", catalogue) as VoiceCmd.SaleChoice
  assertTrue(cmd.options.size >= 2)
  assertTrue(cmd.options.any { it.productId == "sp1" })
 }

 @Test fun unknownProductIsUnknown() {
  val cmd = VoiceParser.parse("xyz helicopter sale", catalogue)
  assertTrue(cmd is VoiceCmd.Unknown)
 }

 @Test fun profitQuestion() {
  assertTrue(VoiceParser.parse("aaj ka profit kitna", catalogue) is VoiceCmd.AskProfit)
  assertTrue(VoiceParser.parse("aaj kitna kamaya", catalogue) is VoiceCmd.AskProfit)
 }

 @Test fun stockQuestionFindsProduct() {
  val cmd = VoiceParser.parse("parle g ka stock kitna bacha", catalogue) as VoiceCmd.AskStock
  assertEquals("sp1", cmd.productId)
 }

 @Test fun totalDuesVsCustomerDues() {
  assertTrue(VoiceParser.parse("kul udhaar kitna hai", catalogue) is VoiceCmd.AskDues)
  val c = VoiceParser.parse("rahul ka udhaar batao", catalogue) as VoiceCmd.FindCustomer
  assertEquals("rahul", c.query)
 }

 @Test fun reportsNavigation() {
  assertTrue(VoiceParser.parse("report kholo", catalogue) is VoiceCmd.OpenReports)
 }

 @Test fun yesAndNo() {
  assertTrue(VoiceParser.parse("haan", catalogue) is VoiceCmd.Yes)
  assertTrue(VoiceParser.parse("haan bech do", catalogue) is VoiceCmd.Yes)
  assertTrue(VoiceParser.parse("naa ruko", catalogue) is VoiceCmd.No)
  assertTrue(VoiceParser.parse("cancel", catalogue) is VoiceCmd.No)
 }

 @Test fun emptyHeardIsUnknown() {
  assertTrue(VoiceParser.parse("  ", catalogue) is VoiceCmd.Unknown)
 }
}
