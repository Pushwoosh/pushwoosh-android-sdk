package com.pushwoosh.pushmart.ui.inapp

import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.parser.InAppConfigParser
import com.pushwoosh.pushmart.BuildConfig
import com.pushwoosh.pushmart.catalog.Catalog
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InAppPresetsTest {
    private val packageName = BuildConfig.APPLICATION_ID
    private val presets = InAppPresets(packageName)
    private val localPrefix = "android.resource://$packageName/drawable/"

    @Test
    fun hasNinePresetsWithUniqueIds() {
        assertEquals(9, presets.all.size)
        assertEquals(presets.all.size, presets.all.map { it.id }.toSet().size)
        assertEquals(presets.all.size, presets.all.map { it.label }.toSet().size)
    }

    @Test
    fun everyPresetParsesInBothColorSchemes() {
        for (preset in presets.all) {
            for (dark in listOf(false, true)) {
                val message = InAppConfigParser.parse(JSONObject(preset.config).toString(), dark)
                assertNotNull("${preset.id} dark=$dark did not parse", message)
                assertEquals(preset.id, message!!.id)
            }
        }
    }

    @Test
    fun modalDarkOverlayChangesBackgroundAndImage() {
        val json = JSONObject(presets.modal.config).toString()
        val light = InAppConfigParser.parse(json, false)!!.layout as InAppLayout.Modal
        val dark = InAppConfigParser.parse(json, true)!!.layout as InAppLayout.Modal
        assertNotEquals(light.content.backgroundColor, dark.content.backgroundColor)
        assertNotEquals(light.content.imageUrl, dark.content.imageUrl)
    }

    @Test
    fun mediaUrlsAreLocalDrawablesOrHttps() {
        val urls = presets.all.flatMap { mediaUrls(it.config) }
        assertTrue(urls.isNotEmpty())
        for (url in urls) {
            assertTrue(url, url.startsWith(localPrefix) || url.startsWith("https://"))
        }
    }

    @Test
    fun everyLocalDrawableExists() {
        val names =
            presets.all
                .flatMap { mediaUrls(it.config) }
                .filter { it.startsWith(localPrefix) }
                .map { it.removePrefix(localPrefix) }
        assertTrue(names.isNotEmpty())
        val resources = RuntimeEnvironment.getApplication().resources
        for (name in names.toSet()) {
            assertTrue(name, resources.getIdentifier(name, "drawable", packageName) != 0)
        }
    }

    @Test
    fun presetsUseCatalogPhotos() {
        val local = { config: Map<String, Any?> ->
            mediaUrls(config)
                .filter { it.startsWith(localPrefix) }
                .map { it.removePrefix(localPrefix) }
        }
        for (name in presets.all.flatMap { local(it.config) }) {
            val catalogPhoto =
                name.startsWith("product_") && Catalog.byId(name.removePrefix("product_")) != null
            assertTrue(name, catalogPhoto || name.startsWith("drop_"))
        }
        val carousel = local(presets.carousel.config)
        assertTrue(carousel.any { it.startsWith("product_") })
        assertTrue(carousel.any { it.startsWith("drop_") })

        val texts = presets.all.flatMap { strings(it.config) }
        val productLinks = texts.filter { it.startsWith(PRODUCT_LINK) }
        assertTrue(productLinks.isNotEmpty())
        for (link in productLinks) {
            assertNotNull(link, Catalog.byId(link.removePrefix(PRODUCT_LINK)))
        }
        for (text in texts) {
            for (gadget in REMOVED_GADGETS) {
                assertFalse(text, text.contains(gadget, ignoreCase = true))
            }
        }
    }

    private fun strings(node: Any?): List<String> =
        when (node) {
            is String -> listOf(node)
            is Map<*, *> -> node.values.flatMap { strings(it) }
            is List<*> -> node.flatMap { strings(it) }
            else -> emptyList()
        }

    /**
     * Collects image/poster/fallback/video urls; action objects also carry a `url` key (deep links)
     * and are skipped.
     */
    private fun mediaUrls(node: Any?): List<String> =
        when (node) {
            is Map<*, *> ->
                if (node["type"] == "url" || node["type"] == "close") emptyList()
                else
                    node.entries.flatMap { (key, value) ->
                        if (key in MEDIA_KEYS && value is String) listOf(value)
                        else mediaUrls(value)
                    }
            is List<*> -> node.flatMap { mediaUrls(it) }
            else -> emptyList()
        }

    companion object {
        private val MEDIA_KEYS = setOf("image", "poster", "fallback", "url")
        private const val PRODUCT_LINK = "pushwoosh://product/"
        private val REMOVED_GADGETS =
            listOf("Lumen", "Chrono", "AeroKnit", "Orbit", "Trail", "Voyager", "Beacon", "Glide")
    }
}
