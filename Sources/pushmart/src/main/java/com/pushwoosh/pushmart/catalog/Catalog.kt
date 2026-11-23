package com.pushwoosh.pushmart.catalog

object Catalog {
    val categories = listOf("All", "Outerwear", "Tops", "Jeans", "Dresses", "Skirts")
    val preferenceCategories = listOf("Outerwear", "Tops", "Jeans", "Dresses", "Skirts")

    val all: List<Product> =
        listOf(
            Product(
                id = "outwear_01",
                name = "Textured Coat",
                brand = "PushMart",
                category = "Outerwear",
                priceCents = 12999,
                tintArgb = 0xFFB08968.toInt(),
                blurb =
                    "Coat in a textured weave with front pockets, 3/4-length sleeves with sewn-in turn-ups, a vent at the back and no buttons. Unlined."),
            Product(
                id = "outwear_02",
                name = "Linen Trenchcoat",
                brand = "PushMart",
                category = "Outerwear",
                priceCents = 15999,
                tintArgb = 0xFFB08968.toInt(),
                blurb =
                    "Trenchcoat in linen with decorative tied tabs at the cuffs, a slit in the sides, side pockets, no buttons and a detachable tie belt at the waist. Unlined."),
            Product(
                id = "outwear_03",
                name = "Lyocell Trenchcoat",
                brand = "PushMart",
                category = "Outerwear",
                priceCents = 14999,
                tintArgb = 0xFFB08968.toInt(),
                blurb =
                    "Coat in washed twill made from lyocell with a low stand-up collar, detached yoke at the back and a diagonal zip and storm flap with a press-stud at the front. Angled flap pockets at the sides, a tie belt at the waist and a single back vent. Unlined."),
            Product(
                id = "outwear_04",
                name = "Twill Jacket",
                brand = "PushMart",
                category = "Outerwear",
                priceCents = 12499,
                tintArgb = 0xFFB08968.toInt(),
                blurb =
                    "Jacket in soft, washed cotton twill with pockets at the front and side vents at the back. Unlined."),
            Product(
                id = "outwear_05",
                name = "Short Coat",
                brand = "PushMart",
                category = "Outerwear",
                priceCents = 17999,
                tintArgb = 0xFFB08968.toInt(),
                blurb =
                    "Short coat in a textured weave with a contrasting colour section to the hem, long sleeves, side pockets and no buttons. Lined."),
            Product(
                id = "tops_01",
                name = "Women's Stretch Slim Fit Polo",
                brand = "PushMart",
                category = "Tops",
                priceCents = 2999,
                tintArgb = 0xFFFF8A3D.toInt(),
                blurb =
                    "Our iconic polo style made from stretch cotton piqué gets a streamlined update with a slim fit. Wear it with dark-rinse denim or skinny black trousers for instant chic."),
            Product(
                id = "tops_02",
                name = "Women's Linen Tee Shirt",
                brand = "PushMart",
                category = "Tops",
                priceCents = 2799,
                tintArgb = 0xFFFF8A3D.toInt(),
                blurb =
                    "Casual style and linen blend flamme jersey construction make this cool tee a sure shot. Pair it with skinny jeans from the collection for a kickin' summer look."),
            Product(
                id = "tops_03",
                name = "Women's Jersey V-neck Tee Shirt",
                brand = "PushMart",
                category = "Tops",
                priceCents = 2599,
                tintArgb = 0xFFFF8A3D.toInt(),
                blurb =
                    "Some pieces are simply essential, and this V-neck tee in innovative ultra-soft cotton jersey is one of those. Stock up on your choice of colors to maximize your options."),
            Product(
                id = "tops_05",
                name = "Women's Classic Fit Tee Shirt",
                brand = "PushMart",
                category = "Tops",
                priceCents = 3499,
                tintArgb = 0xFFFF8A3D.toInt(),
                blurb = "So fresh and comfortable, you'll find a myriad of ways to wear it."),
            Product(
                id = "jeans_01",
                name = "Skinny Low Ankle Jeans",
                brand = "PushMart",
                category = "Jeans",
                priceCents = 5999,
                tintArgb = 0xFF4A6FA5.toInt(),
                blurb =
                    "5-pocket, low-rise ankle-length jeans in washed superstretch denim with skinny legs with a zip at the hems."),
            Product(
                id = "jeans_02",
                name = "Skinny Ankle Trashed Jeans",
                brand = "PushMart",
                category = "Jeans",
                priceCents = 5499,
                tintArgb = 0xFF4A6FA5.toInt(),
                blurb =
                    "5-pocket ankle-length jeans in washed stretch denim with hard-worn details, a regular waist and skinny legs with frayed edges around the hems."),
            Product(
                id = "jeans_03",
                name = "Straight High Ankle Jeans",
                brand = "PushMart",
                category = "Jeans",
                priceCents = 6499,
                tintArgb = 0xFF4A6FA5.toInt(),
                blurb =
                    "5-pocket ankle-length jeans in washed stretch denim with a high waist and straight legs."),
            Product(
                id = "jeans_04",
                name = "Slim Regular Ankle Jeans",
                brand = "PushMart",
                category = "Jeans",
                priceCents = 6999,
                tintArgb = 0xFF4A6FA5.toInt(),
                blurb =
                    "5-pocket ankle-length jeans in soft, washed superstretch denim with a regular waist and slim legs."),
            Product(
                id = "jeans_05",
                name = "Skinny High Ankle Ripped Jeans",
                brand = "PushMart",
                category = "Jeans",
                priceCents = 7999,
                tintArgb = 0xFF4A6FA5.toInt(),
                blurb =
                    "Ankle-length jeans in washed stretch denim with a high waist, hard-worn details and ultra-slim legs."),
            Product(
                id = "dress_01",
                name = "Ruffle Hem Shift Dress in Ponte",
                brand = "PushMart",
                category = "Dresses",
                priceCents = 4499,
                tintArgb = 0xFFFF5A5F.toInt(),
                blurb =
                    "• Ponte fabric • Round neckline • Ruffle hemline • Regular fit- true to size • Machine wash • 72% Viscose, 24% Polyester, 4% Elastane"),
            Product(
                id = "dress_02",
                name = "Frill Front Mini Shirt Dress",
                brand = "PushMart",
                category = "Dresses",
                priceCents = 4999,
                tintArgb = 0xFFFF5A5F.toInt(),
                blurb =
                    "• Lightweight cotton • High neckline • Point collar • Neck tie • Frill detail to front • Regular fit - true to size • Machine wash • 100% Cotton"),
            Product(
                id = "dress_03",
                name = "Strappy Tiered Maxi Dress",
                brand = "PushMart",
                category = "Dresses",
                priceCents = 5999,
                tintArgb = 0xFFFF5A5F.toInt(),
                blurb =
                    "• Lightweight knitted fabric • V-neckline • Tiered design • Slim straps • Cross-over back • Ruffled trims • Self-tie waist belt • Machine wash • 52% Polyester, 48% Cotton"),
            Product(
                id = "dress_04",
                name = "Plait Halter Sundress",
                brand = "PushMart",
                category = "Dresses",
                priceCents = 3999,
                tintArgb = 0xFFFF5A5F.toInt(),
                blurb =
                    "• Woven cotton • Halter neckline • Triangle cups • Plait-style straps • Stretch waist • Regular fit - true to size • Machine wash • 100% Cotton"),
            Product(
                id = "dress_05",
                name = "Premium Embroidered Sundress",
                brand = "PushMart",
                category = "Dresses",
                priceCents = 6999,
                tintArgb = 0xFFFF5A5F.toInt(),
                blurb =
                    "• Crinkled woven fabric • Floral embroidery • Square neckline • Crochet straps • Lace-up back • Regular fit - true to size • Machine wash • 100% Viscose"),
            Product(
                id = "skirt_01",
                name = "Mesh Midi Skirt",
                brand = "PushMart",
                category = "Skirts",
                priceCents = 3999,
                tintArgb = 0xFFAF7BFF.toInt(),
                blurb =
                    "• Lightweight woven • High waist • Mesh panel • Stretch waistband • Regular fit- true to size • Machine wash • 100% Polyester"),
            Product(
                id = "skirt_02",
                name = "High Waisted Denim Skirt",
                brand = "PushMart",
                category = "Skirts",
                priceCents = 4499,
                tintArgb = 0xFFAF7BFF.toInt(),
                blurb =
                    "• Pure-cotton denim • High-rise waistline • Button placket • Two pockets • Kick split • Regular fit - true to size • Machine wash • 100% Cotton"),
            Product(
                id = "skirt_03",
                name = "Tulle Prom Skirt with Multi Layers",
                brand = "PushMart",
                category = "Skirts",
                priceCents = 6499,
                tintArgb = 0xFFAF7BFF.toInt(),
                blurb =
                    "• Soft-touch woven fabric • Mesh overlay • Layered design for extreme volume • High fitted waistband • Zip back fastening • Hook and eye closure • Regular fit - true to size • Machine wash/Hand wash/Dry clean • 100% Polyester"),
            Product(
                id = "skirt_04",
                name = "Midi Pencil Skirt in Jersey",
                brand = "PushMart",
                category = "Skirts",
                priceCents = 3599,
                tintArgb = 0xFFAF7BFF.toInt(),
                blurb =
                    "• Soft-touch stretch jersey • Stretch waistband • Flattering high rise • Kick split • Close cut bodycon fit • Machine wash • 95% Cotton, 5% Elastane"),
            Product(
                id = "skirt_05",
                name = "Midi Skater Skirt in Jersey",
                brand = "PushMart",
                category = "Skirts",
                priceCents = 3799,
                tintArgb = 0xFFAF7BFF.toInt(),
                blurb =
                    "• Lightweight soft-touch jersey • Elasticated waistband • High-rise waist • Soft pleating • Regular fit - true to size • Machine wash • 95% Viscose, 5% Elastane"),
        )

    val featured: List<Product>
        get() =
            listOf("dress_04", "outwear_02", "tops_02", "jeans_01", "skirt_03", "dress_01")
                .mapNotNull { byId(it) }

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
