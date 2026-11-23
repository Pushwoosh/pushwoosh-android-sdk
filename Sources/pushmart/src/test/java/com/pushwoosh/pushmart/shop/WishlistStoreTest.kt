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
    private val coat = Catalog.byId("outwear_01")!!
    private val tee = Catalog.byId("tops_02")!!

    @Test
    fun toggleAddsToFrontAndAppendsWishlistTag() {
        wishlist.toggle(tee)
        wishlist.toggle(coat)
        assertEquals(listOf("outwear_01", "tops_02"), wishlist.ids)
        assertEquals(listOf("outwear_01", "tops_02"), prefs.getStringList("pushmart.wishlist"))
        assertTrue(wishlist.contains("outwear_01"))
        assertEquals(
            listOf(
                TagOp.AppendList("wishlist", listOf("tops_02")),
                TagOp.AppendList("wishlist", listOf("outwear_01"))),
            gateway.tags)
        assertTrue(gateway.events.isEmpty())
    }

    @Test
    fun toggleAgainRemovesFromWishlistTag() {
        wishlist.toggle(coat)
        wishlist.toggle(coat)
        assertFalse(wishlist.contains("outwear_01"))
        assertEquals(TagOp.RemoveFromList("wishlist", listOf("outwear_01")), gateway.tags.last())
        assertTrue(gateway.events.isEmpty())
    }

    @Test
    fun productsSkipIdsMissingFromCatalog() {
        prefs.putStringList("pushmart.wishlist", listOf("aeroknit", "dress_04", "chrono"))
        assertEquals(listOf("dress_04"), WishlistStore(prefs, gateway).products.map { it.id })
    }
}
