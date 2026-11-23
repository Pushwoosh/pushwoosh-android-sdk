package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.catalog.Product
import com.pushwoosh.pushmart.core.KeyValueStore
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.random.Random

data class Order(
    val id: String,
    val productIds: List<String>,
    val totalText: String,
    val placedAtMillis: Long,
) {
    val products: List<Product>
        get() = productIds.mapNotNull(Catalog::byId)
}

/** Orders placed on this device, newest first. Only the newest [LIMIT] are kept. */
class OrderStore(
    private val prefs: KeyValueStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val nextNumber: () -> Int = { Random.nextInt(20500, 30000) },
) {
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    var orders: List<Order> = prefs.getStringList(KEY).mapNotNull(::decode)
        private set

    fun byId(id: String): Order? = orders.firstOrNull { it.id == id }

    fun place(productIds: List<String>, totalText: String): Order {
        var id: String
        do {
            id = "PM-" + nextNumber()
        } while (byId(id) != null)
        val order = Order(id, productIds, totalText, now())
        orders = (listOf(order) + orders).take(LIMIT)
        prefs.putStringList(KEY, orders.map(::encode))
        listeners.forEach { it() }
        return order
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun encode(order: Order): String =
        listOf(order.id, order.placedAtMillis, order.totalText, order.productIds.joinToString(","))
            .joinToString("|")

    private fun decode(entry: String): Order? {
        val parts = entry.split("|", limit = 4)
        if (parts.size != 4) return null
        val placedAt = parts[1].toLongOrNull() ?: return null
        return Order(parts[0], parts[3].split(",").filter { it.isNotEmpty() }, parts[2], placedAt)
    }

    companion object {
        const val KEY = "pushmart.orders"
        const val LIMIT = 20
    }
}
