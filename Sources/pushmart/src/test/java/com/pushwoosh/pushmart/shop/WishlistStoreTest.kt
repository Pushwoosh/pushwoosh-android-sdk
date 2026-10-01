package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.core.InMemoryKeyValueStore
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
import com.pushwoosh.pushmart.sdk.TagOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WishlistStoreTest {
    private val prefs = InMemoryKeyValueStore()
    private val gateway = FakePushwooshGateway()
    private val wishlist = WishlistStore(prefs, gateway)
    private val aeroknit = Catalog.byId("aeroknit")!!
    private val orbit = Catalog.byId("orbit")!!

    @Test
    fun toggleAddsToFrontAppendsTagAndPostsEvent() {
        wishlist.toggle(orbit)
        wishlist.toggle(aeroknit)
        assertEquals(listOf("aeroknit", "orbit"), wishlist.ids)
        assertEquals(listOf("aeroknit", "orbit"), prefs.getStringList("pushmart.wishlist"))
        assertTrue(wishlist.contains("aeroknit"))
        assertEquals(
            listOf(
                TagOp.AppendList("wishlist", listOf("orbit")),
                TagOp.AppendList("wishlist", listOf("aeroknit"))),
            gateway.tags)
        assertEquals(
            listOf(
                "wishlist_add" to mapOf<String, Any>("productId" to "orbit", "price" to 49),
                "wishlist_add" to mapOf<String, Any>("productId" to "aeroknit", "price" to 129)),
            gateway.events)
    }

    @Test
    fun toggleAgainRemovesWithoutEvent() {
        wishlist.toggle(aeroknit)
        wishlist.toggle(aeroknit)
        assertFalse(wishlist.contains("aeroknit"))
        assertEquals(TagOp.RemoveFromList("wishlist", listOf("aeroknit")), gateway.tags.last())
        assertEquals(1, gateway.events.size)
    }

    @Test
    fun productsResolveFromCatalog() {
        wishlist.toggle(aeroknit)
        assertEquals(listOf(aeroknit), wishlist.products)
    }
}
