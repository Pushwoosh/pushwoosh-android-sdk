package com.pushwoosh.pushmart.catalog

import java.util.Locale

data class Product(
    val id: String,
    val name: String,
    val brand: String,
    val category: String,
    val priceCents: Int,
    val tintArgb: Int,
    val blurb: String,
    val badge: String? = null,
) {
    val priceText: String
        get() =
            if (priceCents % 100 == 0) "$" + priceCents / 100
            else String.format(Locale.US, "$%.2f", priceCents / 100.0)

    val priceDollars: Int
        get() = priceCents / 100

    val imageName: String
        get() = "product_$id"

    // Same hash as the iOS sample so ratings and review counts match across platforms.
    private val seed: Int
        get() = id.fold(0L) { acc, c -> (acc * 31 + c.code) and 0x7fffffffL }.toInt()

    val rating: Double
        get() = 4.3 + (seed % 7) / 10.0

    val reviewsText: String
        get() {
            val count = 120 + seed % 2400
            return if (count >= 1000) String.format(Locale.US, "%.1fk reviews", count / 1000.0)
            else "$count reviews"
        }

    val optionTitle: String
        get() =
            when (category) {
                "Sneakers" -> "Size"
                "Watches" -> "Case"
                else -> "Color"
            }

    val optionValues: List<String>
        get() =
            when (category) {
                "Sneakers" -> listOf("US 7", "US 8", "US 9", "US 10", "US 11")
                "Watches" -> listOf("41 mm", "45 mm")
                else -> listOf("Midnight", "Sand", "Coral")
            }
}
