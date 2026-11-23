package com.pushwoosh.pushmart.catalog

import com.pushwoosh.pushmart.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test
    fun catalogIsTheIosCatalogInIosOrder() {
        assertEquals(
            listOf(
                "outwear_01",
                "outwear_02",
                "outwear_03",
                "outwear_04",
                "outwear_05",
                "tops_01",
                "tops_02",
                "tops_03",
                "tops_05",
                "jeans_01",
                "jeans_02",
                "jeans_03",
                "jeans_04",
                "jeans_05",
                "dress_01",
                "dress_02",
                "dress_03",
                "dress_04",
                "dress_05",
                "skirt_01",
                "skirt_02",
                "skirt_03",
                "skirt_04",
                "skirt_05"),
            Catalog.all.map { it.id })
        assertEquals(setOf("PushMart"), Catalog.all.map { it.brand }.toSet())
    }

    @Test
    fun featuredIsTheIosPicksInOrder() {
        assertEquals(
            listOf("dress_04", "outwear_02", "tops_02", "jeans_01", "skirt_03", "dress_01"),
            Catalog.featured.map { it.id })
    }

    @Test
    fun everyProductSitsInAShopCategory() {
        assertEquals("All", Catalog.categories.first())
        assertEquals(Catalog.categories.drop(1).toSet(), Catalog.all.map { it.category }.toSet())
        assertEquals(Catalog.categories.drop(1), Catalog.preferenceCategories)
    }

    @Test
    fun byIdResolvesKnownAndRejectsLegacyGadgetIds() {
        assertNotNull(Catalog.byId("dress_04"))
        assertNull(Catalog.byId("DRESS_04"))
        for (legacy in
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
                "solo")) {
            assertNull(legacy, Catalog.byId(legacy))
        }
    }

    @Test
    fun inCategoryFiltersAndAllReturnsEverything() {
        assertEquals(24, Catalog.inCategory("All").size)
        assertEquals(
            listOf("tops_01", "tops_02", "tops_03", "tops_05"),
            Catalog.inCategory("Tops").map { it.id })
    }

    @Test
    fun searchMatchesNameOrBrandCaseInsensitive() {
        assertEquals(
            listOf("outwear_02", "outwear_03"), Catalog.search("All", "TRENCH").map { it.id })
        assertEquals(24, Catalog.search("All", "pushmart").size)
        assertEquals(
            listOf("skirt_04", "skirt_05"), Catalog.search("Skirts", "jersey").map { it.id })
        assertEquals(emptyList<String>(), Catalog.search("Jeans", "skirt").map { it.id })
    }

    @Test
    fun searchTrimsQueryAndEmptyQueryMeansNoFilter() {
        assertEquals(
            listOf("outwear_01", "outwear_02", "outwear_03", "outwear_05"),
            Catalog.search("All", "  coat ").map { it.id })
        assertEquals(24, Catalog.search("All", "   ").size)
    }

    @Test
    fun relatedPrefersSameCategoryThenOthersMaxFour() {
        assertEquals(
            listOf("tops_01", "tops_02", "tops_03", "outwear_01"),
            Catalog.related(Catalog.byId("tops_05")!!).map { it.id })
        assertEquals(
            listOf("dress_01", "dress_02", "dress_03", "dress_05"),
            Catalog.related(Catalog.byId("dress_04")!!).map { it.id })
    }

    @Test
    fun priceTextDropsCentsForWholeDollars() {
        assertEquals("$129.99", Catalog.byId("outwear_01")!!.priceText)
        assertEquals("$27.99", Catalog.byId("tops_02")!!.priceText)
        assertEquals("$40", Catalog.byId("tops_02")!!.copy(priceCents = 4000).priceText)
    }

    @Test
    fun everyProductHasABundledPhoto() {
        for (product in Catalog.all) {
            assertTrue(
                product.id,
                runCatching { R.drawable::class.java.getField(product.imageName) }.isSuccess)
        }
    }
}
