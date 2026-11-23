package com.pushwoosh.pushmart.routing

import com.pushwoosh.pushmart.catalog.Catalog
import java.math.RoundingMode
import java.net.URI
import java.net.URISyntaxException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object DeepLinkRouter {
    private const val SCHEME = "pushwoosh"
    private const val ADD_PRODUCT_SCHEME = "pushon-add-product"
    private const val OPEN_TAB_SCHEME = "pushon-open-tab"
    private val INTEGER_PREFIX = Regex("""^\s*([+-]?\d+)""")

    fun fromUri(uri: String): Route? {
        val parsed =
            try {
                URI(uri)
            } catch (e: URISyntaxException) {
                return null
            }
        return when (parsed.scheme?.lowercase()) {
            SCHEME -> pushwooshRoute(parsed)
            // getHost() is null for ids with "_", such as dress_04.
            ADD_PRODUCT_SCHEME ->
                parsed.rawAuthority?.let { Catalog.byId(it) }?.let { Route.AddToBag(it.id) }
            OPEN_TAB_SCHEME ->
                if ("cart".equals(parsed.host, ignoreCase = true)) Route.Cart else null
            else -> null
        }
    }

    /**
     * Routes push custom data. PushOn keys under `custom` come first: `voucher {discount}`, then
     * `specialOffer {product_id}` (an unknown id routes nowhere), then `items_in_basket`. Without
     * them the top-level `voucher`, `product_id` and `sale` keys apply, in that order.
     */
    fun fromCustomData(data: Map<String, Any?>): Route? {
        val custom = data["custom"] as? Map<*, *>
        if (custom != null) {
            val voucher = custom["voucher"] as? Map<*, *>
            discount(voucher?.get("discount"))?.let {
                return Route.Voucher("-$it%")
            }
            val offer = custom["specialOffer"]
            if (offer is Map<*, *>)
                return (offer["product_id"] as? String)?.let { productRoute(it) }
            if (custom.containsKey("items_in_basket")) return Route.Cart
        }
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

    private fun pushwooshRoute(parsed: URI): Route? {
        val host = parsed.host?.lowercase() ?: return null
        val segments = parsed.path.orEmpty().split("/").filter { it.isNotEmpty() }
        return when (host) {
            "sale" -> Route.Sale
            "product" -> segments.firstOrNull()?.let { productRoute(it) }
            "product1" -> productRoute("outwear_01")
            "product2" -> productRoute("outwear_02")
            else -> null
        }
    }

    /** As iOS PushOn: any number, or a string with a positive integer value, as whole percent. */
    private fun discount(value: Any?): String? {
        val number: Number =
            when (value) {
                is Number -> value
                is String -> integerValue(value).takeIf { it > 0 } ?: return null
                else -> return null
            }
        val format = DecimalFormat("0", DecimalFormatSymbols(Locale.US))
        format.maximumIntegerDigits = 3
        format.roundingMode = RoundingMode.HALF_EVEN
        return format.format(number)
    }

    /**
     * As NSString.integerValue: leading whitespace, then `[+-]?\d+`, saturating on overflow;
     * else 0.
     */
    private fun integerValue(value: String): Long {
        val digits = INTEGER_PREFIX.find(value)?.groupValues?.get(1) ?: return 0
        return digits.toLongOrNull()
            ?: if (digits.startsWith("-")) Long.MIN_VALUE else Long.MAX_VALUE
    }

    private fun productRoute(id: String): Route? = Catalog.byId(id)?.let { Route.Product(it.id) }
}
