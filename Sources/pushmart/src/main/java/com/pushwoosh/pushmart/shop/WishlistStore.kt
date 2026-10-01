package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.catalog.Product
import com.pushwoosh.pushmart.core.KeyValueStore
import com.pushwoosh.pushmart.sdk.PushwooshGateway
import com.pushwoosh.pushmart.sdk.TagOp
import java.util.concurrent.CopyOnWriteArraySet

class WishlistStore(private val prefs: KeyValueStore, private val gateway: PushwooshGateway) {
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    val ids: List<String>
        get() = prefs.getStringList(KEY)

    val products: List<Product>
        get() = ids.mapNotNull { Catalog.byId(it) }

    fun contains(productId: String): Boolean = ids.contains(productId)

    fun toggle(product: Product) {
        val current = ids
        val adding = !current.contains(product.id)
        val updated =
            if (adding) listOf(product.id) + current else current.filter { it != product.id }
        prefs.putStringList(KEY, updated)
        val op =
            if (adding) TagOp.AppendList(TAG, listOf(product.id))
            else TagOp.RemoveFromList(TAG, listOf(product.id))
        gateway.setTags(listOf(op))
        if (adding) {
            gateway.postEvent(
                "wishlist_add", mapOf("productId" to product.id, "price" to product.priceDollars))
        }
        listeners.forEach { it() }
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    companion object {
        const val KEY = "pushmart.wishlist"
        const val TAG = "wishlist"
    }
}
