package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.catalog.Product
import com.pushwoosh.pushmart.core.KeyValueStore
import com.pushwoosh.pushmart.sdk.PushwooshGateway
import java.util.Locale
import java.util.concurrent.CopyOnWriteArraySet

data class CartLine(val product: Product, val quantity: Int)

class CartStore(private val prefs: KeyValueStore, private val gateway: PushwooshGateway) {
    private val quantities = LinkedHashMap<String, Int>()
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    init {
        for (entry in prefs.getStringList(KEY)) {
            val parts = entry.split(":")
            if (parts.size != 2) continue
            val quantity = parts[1].toIntOrNull() ?: continue
            if (Catalog.byId(parts[0]) == null || quantity <= 0) continue
            quantities[parts[0]] = quantity
        }
    }

    val items: Map<String, Int>
        get() = quantities.toMap()

    val count: Int
        get() = quantities.values.sum()

    val lines: List<CartLine>
        get() =
            quantities
                .mapNotNull { (id, qty) -> Catalog.byId(id)?.let { CartLine(it, qty) } }
                .sortedBy { it.product.name }

    val totalCents: Int
        get() = lines.sumOf { it.product.priceCents * it.quantity }

    val totalText: String
        get() = String.format(Locale.US, "$%.2f", totalCents / 100.0)

    fun add(productId: String) {
        if (Catalog.byId(productId) == null) return
        quantities[productId] = (quantities[productId] ?: 0) + 1
        sync()
    }

    fun setQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) quantities.remove(productId) else quantities[productId] = quantity
        sync()
    }

    fun clear() {
        quantities.clear()
        sync()
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun sync() {
        prefs.putStringList(KEY, quantities.map { (id, qty) -> "$id:$qty" })
        gateway.setBadgeNumber(count)
        listeners.forEach { it() }
    }

    companion object {
        const val KEY = "pushmart.cart"
    }
}
