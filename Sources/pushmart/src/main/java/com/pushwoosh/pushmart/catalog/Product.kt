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
) {
    val priceText: String
        get() =
            if (priceCents % 100 == 0) "$" + priceCents / 100
            else String.format(Locale.US, "$%.2f", priceCents / 100.0)

    val imageName: String
        get() = "product_$id"

    val sizes: List<String>
        get() = listOf("XS", "S", "M", "L", "XL")
}
