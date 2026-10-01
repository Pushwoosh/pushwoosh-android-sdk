package com.pushwoosh.pushmart.routing

import com.pushwoosh.pushmart.catalog.Catalog
import java.net.URI
import java.net.URISyntaxException

object DeepLinkRouter {
    private const val SCHEME = "pushwoosh"

    fun fromUri(uri: String): Route? {
        val parsed =
            try {
                URI(uri)
            } catch (e: URISyntaxException) {
                return null
            }
        if (!SCHEME.equals(parsed.scheme, ignoreCase = true)) return null
        val host = parsed.host?.lowercase() ?: return null
        val segments = parsed.path.orEmpty().split("/").filter { it.isNotEmpty() }
        return when (host) {
            "sale" -> Route.Sale
            "product" -> segments.firstOrNull()?.let { productRoute(it) }
            "product1" -> productRoute("aeroknit")
            "product2" -> productRoute("chrono")
            else -> null
        }
    }

    fun fromCustomData(data: Map<String, Any?>): Route? {
        val voucher = data["voucher"] as? String
        if (!voucher.isNullOrEmpty()) return Route.Voucher(voucher)
        val productId = data["product_id"] as? String
        if (productId != null)
            productRoute(productId)?.let {
                return it
            }
        if (data["sale"] != null) return Route.Sale
        return null
    }

    private fun productRoute(id: String): Route? = Catalog.byId(id)?.let { Route.Product(it.id) }
}
