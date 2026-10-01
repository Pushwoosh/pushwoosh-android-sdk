package com.pushwoosh.pushmart.catalog

object Catalog {
    val categories = listOf("All", "Sneakers", "Audio", "Watches", "Bags", "Tech")
    val preferenceCategories = listOf("Sneakers", "Audio", "Watches", "Bags", "Tech")

    val all: List<Product> =
        listOf(
            Product(
                id = "aeroknit",
                name = "AeroKnit Runner",
                brand = "PushMart Sport",
                category = "Sneakers",
                priceCents = 12900,
                tintArgb = 0xFFFF5A5F.toInt(),
                blurb =
                    "Feather-light knit runners with a responsive foam sole built for all-day city miles.",
                badge = "New"),
            Product(
                id = "pulsebuds",
                name = "Pulse Buds",
                brand = "PushMart Audio",
                category = "Audio",
                priceCents = 18900,
                tintArgb = 0xFFFF8A3D.toInt(),
                blurb = "In-ear earphones with deep, punchy bass and a tangle-free cable."),
            Product(
                id = "chrono",
                name = "Chrono S3",
                brand = "PushMart Time",
                category = "Watches",
                priceCents = 24900,
                tintArgb = 0xFF5AC8FA.toInt(),
                blurb =
                    "A slim chronograph with a sapphire crystal, rose-gold case and Italian leather strap.",
                badge = "Drop"),
            Product(
                id = "voyager",
                name = "Voyager Rattan",
                brand = "PushMart Goods",
                category = "Bags",
                priceCents = 15900,
                tintArgb = 0xFFAF7BFF.toInt(),
                blurb = "Hand-woven rattan crossbody bag with a vegan-leather strap."),
            Product(
                id = "beacon",
                name = "Beacon Speaker",
                brand = "PushMart Audio",
                category = "Audio",
                priceCents = 9900,
                tintArgb = 0xFF2BD98A.toInt(),
                blurb = "Room-filling 360° sound in a rugged, splash-proof body."),
            Product(
                id = "glide",
                name = "Glide Low",
                brand = "PushMart Sport",
                category = "Sneakers",
                priceCents = 10900,
                tintArgb = 0xFFFFC24B.toInt(),
                blurb = "Minimal court-inspired silhouette in a clean monochrome leather."),
            Product(
                id = "orbit",
                name = "Orbit Cable",
                brand = "PushMart Tech",
                category = "Tech",
                priceCents = 4900,
                tintArgb = 0xFFFF4D5E.toInt(),
                blurb = "Braided fast-charge USB cable that coils away neatly in your bag.",
                badge = "Deal"),
            Product(
                id = "lumen",
                name = "Lumen Frames",
                brand = "PushMart Tech",
                category = "Tech",
                priceCents = 21900,
                tintArgb = 0xFF64D2FF.toInt(),
                blurb = "Lightweight tortoiseshell frames with blue-light filtering lenses."),
            Product(
                id = "trail",
                name = "Trail Pack 24L",
                brand = "PushMart Goods",
                category = "Bags",
                priceCents = 13900,
                tintArgb = 0xFF30D158.toInt(),
                blurb = "Weatherproof daypack with a ventilated back panel and rain cover."),
            Product(
                id = "solo",
                name = "Solo Field Watch",
                brand = "PushMart Time",
                category = "Watches",
                priceCents = 17900,
                tintArgb = 0xFFFF9F0A.toInt(),
                blurb = "Analog field watch with a sapphire crystal and 100m water resistance."),
        )

    val featured: List<Product>
        get() = all.take(6)

    private val index: Map<String, Product> = all.associateBy { it.id }

    fun byId(id: String): Product? = index[id]

    fun inCategory(category: String): List<Product> =
        if (category == "All") all else all.filter { it.category == category }

    fun search(category: String, query: String): List<Product> {
        val needle = query.trim().lowercase()
        val pool = inCategory(category)
        if (needle.isEmpty()) return pool
        return pool.filter {
            it.name.lowercase().contains(needle) || it.brand.lowercase().contains(needle)
        }
    }

    fun related(product: Product): List<Product> {
        val same = all.filter { it.category == product.category && it.id != product.id }
        val others = all.filter { it.category != product.category }
        return (same + others).take(4)
    }
}
