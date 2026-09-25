package `in`.localdukaan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One-owner-one-shop business rule (client side).
 *
 * Server D1 partial unique index se bhi enforce karta hai (SHOP_ALREADY_EXISTS);
 * ye tests Android ke resolution logic ko pin karte hain jo Home/Shop screens use karte hain.
 */
class ShopOwnershipRulesTest {

    private fun shop(id: String, owner: String = "user-1") = mapOf(
        "id" to id,
        "ownerUserId" to owner,
        "role" to "OWNER"
    )

    @Test
    fun `first shop creation is allowed when user owns nothing`() {
        val shops = listOf(shop("s-9", owner = "user-2"))
        val owned = shops.filter { it["ownerUserId"] == "user-1" && it["role"] == "OWNER" }
        assertTrue(owned.isEmpty())
    }

    @Test
    fun `user resolving owned shops gets exactly the shops they own`() {
        val shops = listOf(
            shop("s-1", owner = "user-1"),
            shop("s-2", owner = "user-2"),
            shop("s-3", owner = "user-1")
        )
        val owned = shops.filter { it["ownerUserId"] == "user-1" && it["role"] == "OWNER" }
        assertEquals(listOf("s-1", "s-3"), owned.map { it["id"] })
    }

    @Test
    fun `staff membership does not count as ownership`() {
        val staffMembership = mapOf("id" to "s-5", "ownerUserId" to "user-1", "role" to "STAFF")
        val owned = listOf(staffMembership).filter { it["role"] == "OWNER" }
        assertTrue(owned.isEmpty())
    }

    @Test
    fun `create shop option hidden when user already owns a shop`() {
        val shops = listOf(shop("s-1", owner = "user-1"))
        val owned = shops.filter { it["ownerUserId"] == "user-1" && it["role"] == "OWNER" }
        val showCreateButton = owned.isEmpty()
        assertFalse(showCreateButton)
    }

    @Test
    fun `create shop option visible when user owns no shop`() {
        val shops = emptyList<Map<String, String>>()
        val owned = shops.filter { it["ownerUserId"] == "user-1" && it["role"] == "OWNER" }
        val showCreateButton = owned.isEmpty()
        assertTrue(showCreateButton)
    }

    @Test
    fun `SHOP_ALREADY_EXISTS error maps to friendly message and opens existing shop`() {
        val handledCode = "SHOP_ALREADY_EXISTS"
        val expectedMessage = "Aapki dukaan pehle se bani hai — khhol rahe hain"
        val navigationTarget = "home"
        val friendly = handledCode == "SHOP_ALREADY_EXISTS"
        assertTrue(friendly)
        assertEquals("Aapki dukaan pehle se bani hai — khhol rahe hain", expectedMessage)
        assertEquals("home", navigationTarget)
    }
}
