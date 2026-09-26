package `in`.localdukaan

import `in`.localdukaan.core.database.LocalProfile
import `in`.localdukaan.core.database.LocalShop
import org.junit.Assert.*
import org.junit.Test

/** Drawer header: shopkeeper ko dukaan ka naam, customer ko apna naam — Avni-case regression samet. */
class DrawerIdentityTest {
 private fun keeperProfile(name: String?) = LocalProfile("u1", name ?: "", "hi", false, true, true)
 private fun customerProfile(name: String?) = LocalProfile("u2", name ?: "", "hi", true, false, true)
 private fun shop(id: String, name: String, owner: String = "Avni") =
  LocalShop(id, name, "Ghaziabad", "Indirapuram", "ACTIVE", true, ownerName = owner)

 @Test fun shopkeeperSeesShopNameNotPersonName() {
  val id = drawerIdentity(keeperProfile("Avni"), listOf(shop("s1", "Sharma General Store")), "s1")
  assertEquals("Sharma General Store", id.title)
  assertEquals("SHOPKEEPER", id.roleKey)
  assertEquals("Avni", id.subtitle)
  assertEquals("S", id.initial)
 }

 @Test fun shopkeeperUsesSelectedShopNotFirst() {
  val shops = listOf(shop("s1", "Pehli Dukaan"), shop("s2", "Doosri Dukaan"))
  val id = drawerIdentity(keeperProfile("Avni"), shops, "s2")
  assertEquals("Doosri Dukaan", id.title)
 }

 @Test fun shopkeeperWithoutShopHasNoDuplicateSubtitle() {
  val id = drawerIdentity(keeperProfile("Avni"), emptyList(), null)
  assertEquals("Avni", id.title)
  assertNull(id.subtitle)
 }

 @Test fun customerSeesOwnNameWithNoSubtitle() {
  val id = drawerIdentity(customerProfile("Rahul"), listOf(shop("s1", "Sharma General Store")), null)
  assertEquals("Rahul", id.title)
  assertEquals("CUSTOMER", id.roleKey)
  assertNull(id.subtitle)
  assertEquals("R", id.initial)
 }

 @Test fun blankNameFallsBackToAppName() {
  val id = drawerIdentity(customerProfile("  "), emptyList(), null)
  assertEquals("LocalDukaan", id.title)
  assertEquals("L", id.initial)
 }

 @Test fun ownerSameAsShopNameIsNotDuplicated() {
  val id = drawerIdentity(keeperProfile("Sharma General Store"), listOf(shop("s1", "Sharma General Store", "Sharma General Store")), "s1")
  assertEquals("Sharma General Store", id.title)
  // owner == title == profile naam — teeno same hon to subtitle bilkul nahi
  assertNull(id.subtitle)
 }

 /** Dual-role account: customer-tab login par customer UI — shopkeeper dashboard kabhi nahi. */
 private fun dualProfile(name: String?) = LocalProfile("u3", name ?: "", "hi", true, true, true)

 @Test fun dualRoleCustomerTabShowsCustomerIdentity() {
  val id = drawerIdentity(dualProfile("Avni"), listOf(shop("s1", "Sharma General Store", "Avni")), "s1", "CUSTOMER")
  assertEquals("Avni", id.title)
  assertEquals("CUSTOMER", id.roleKey)
  assertNull(id.subtitle)
 }

 @Test fun dualRoleShopkeeperTabShowsShopIdentity() {
  val id = drawerIdentity(dualProfile("Avni"), listOf(shop("s1", "Sharma General Store", "Avni")), "s1", "SHOPKEEPER")
  assertEquals("Sharma General Store", id.title)
  assertEquals("SHOPKEEPER", id.roleKey)
  assertEquals("Avni", id.subtitle)
 }

 @Test fun noActiveRoleFallsBackToProfileFlags() {
  val id = drawerIdentity(dualProfile("Avni"), listOf(shop("s1", "Sharma General Store", "Avni")), "s1", null)
  assertEquals("SHOPKEEPER", id.roleKey)
 }

 @Test fun uiStateRoleGatingPrefersLoginTab() {
  val dual = dualProfile("Avni")
  assertTrue(UiState(profile = dual, activeRole = "CUSTOMER").isCustomerView)
  assertFalse(UiState(profile = dual, activeRole = "CUSTOMER").isShopkeeper)
  assertTrue(UiState(profile = dual, activeRole = "SHOPKEEPER").isShopkeeper)
  assertFalse(UiState(profile = dual, activeRole = "SHOPKEEPER").isCustomerView)
  // Purane sessions (role save nahi) — purana behavior, no regression
  assertTrue(UiState(profile = dual, activeRole = null).isShopkeeper)
  assertTrue(UiState(profile = customerProfile("R"), activeRole = null).isCustomerView)
 }
}
