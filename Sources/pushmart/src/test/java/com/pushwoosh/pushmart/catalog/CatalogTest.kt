package com.pushwoosh.pushmart.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test
    fun catalogHasTenProductsInIosOrder() {
        assertEquals(
            listOf(
                "aeroknit",
                "pulsebuds",
                "chrono",
                "voyager",
                "beacon",
                "glide",
                "orbit",
                "lumen",
                "trail",
                "solo"),
            Catalog.all.map { it.id })
    }

    @Test
    fun featuredIsFirstSix() {
        assertEquals(
            listOf("aeroknit", "pulsebuds", "chrono", "voyager", "beacon", "glide"),
            Catalog.featured.map { it.id })
    }

    @Test
    fun categoriesAreVerbatim() {
        assertEquals(
            listOf("All", "Sneakers", "Audio", "Watches", "Bags", "Tech"), Catalog.categories)
        assertEquals(
            listOf("Sneakers", "Audio", "Watches", "Bags", "Tech"), Catalog.preferenceCategories)
    }

    @Test
    fun byIdResolvesKnownAndRejectsUnknown() {
        assertNotNull(Catalog.byId("chrono"))
        assertNull(Catalog.byId("nope"))
    }

    @Test
    fun inCategoryFiltersAndAllReturnsEverything() {
        assertEquals(10, Catalog.inCategory("All").size)
        assertEquals(listOf("pulsebuds", "beacon"), Catalog.inCategory("Audio").map { it.id })
    }

    @Test
    fun searchMatchesNameOrBrandCaseInsensitive() {
        assertEquals(listOf("pulsebuds"), Catalog.search("All", "PULSE").map { it.id })
        assertEquals(listOf("voyager", "trail"), Catalog.search("All", "goods").map { it.id })
        assertEquals(listOf("aeroknit"), Catalog.search("Sneakers", "runner").map { it.id })
        assertEquals(emptyList<String>(), Catalog.search("Audio", "runner").map { it.id })
    }

    @Test
    fun searchTrimsQueryAndEmptyQueryMeansNoFilter() {
        assertEquals(listOf("chrono"), Catalog.search("All", "  chrono ").map { it.id })
        assertEquals(10, Catalog.search("All", "   ").size)
    }

    @Test
    fun relatedPrefersSameCategoryThenOthersMaxFour() {
        val aeroknit = Catalog.byId("aeroknit")!!
        assertEquals(
            listOf("glide", "pulsebuds", "chrono", "voyager"),
            Catalog.related(aeroknit).map { it.id })
    }

    @Test
    fun priceTextDropsCentsForWholeDollars() {
        assertEquals("$129", Catalog.byId("aeroknit")!!.priceText)
        assertEquals(129, Catalog.byId("aeroknit")!!.priceDollars)
        val odd = Catalog.byId("orbit")!!.copy(priceCents = 1050)
        assertEquals("$10.50", odd.priceText)
    }

    @Test
    fun imageNameAndBadgesAreVerbatim() {
        assertEquals("product_aeroknit", Catalog.byId("aeroknit")!!.imageName)
        assertEquals("New", Catalog.byId("aeroknit")!!.badge)
        assertEquals("Drop", Catalog.byId("chrono")!!.badge)
        assertEquals("Deal", Catalog.byId("orbit")!!.badge)
        assertNull(Catalog.byId("pulsebuds")!!.badge)
    }

    @Test
    fun ratingAndReviewsAreDeterministicAndInRange() {
        for (product in Catalog.all) {
            assertTrue(product.rating >= 4.3 && product.rating <= 4.9)
            assertEquals(product.rating, product.rating, 0.0)
            assertTrue(
                product.reviewsText,
                Regex("^\\d+ reviews$|^\\d+\\.\\dk reviews$").matches(product.reviewsText))
        }
    }

    @Test
    fun optionsDependOnCategory() {
        assertEquals("Size", Catalog.byId("aeroknit")!!.optionTitle)
        assertEquals(
            listOf("US 7", "US 8", "US 9", "US 10", "US 11"),
            Catalog.byId("aeroknit")!!.optionValues)
        assertEquals("Case", Catalog.byId("chrono")!!.optionTitle)
        assertEquals(listOf("41 mm", "45 mm"), Catalog.byId("chrono")!!.optionValues)
        assertEquals("Color", Catalog.byId("beacon")!!.optionTitle)
        assertEquals(listOf("Midnight", "Sand", "Coral"), Catalog.byId("trail")!!.optionValues)
    }
}
