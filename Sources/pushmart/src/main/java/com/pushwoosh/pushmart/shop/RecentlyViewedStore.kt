package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.catalog.Product
import com.pushwoosh.pushmart.core.KeyValueStore
import java.util.concurrent.CopyOnWriteArraySet

/** The last products the shopper opened, newest first, as iOS `RecentlyViewedStore`. */
class RecentlyViewedStore(private val prefs: KeyValueStore) {
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    val products: List<Product>
        get() = prefs.getStringList(KEY).mapNotNull { Catalog.byId(it) }

    fun record(productId: String) {
        val updated = listOf(productId) + prefs.getStringList(KEY).filter { it != productId }
        prefs.putStringList(KEY, updated.take(LIMIT))
        listeners.forEach { it() }
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    companion object {
        const val KEY = "pushmart.recentlyViewed"
        private const val LIMIT = 8
    }
}
