package com.pushwoosh.pushmart.ui.inapp

import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.parser.InAppConfigParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InAppPresetsTest {
    private val packageName = "com.pushwoosh.pushmart"
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
    fun aspectRatiosAreSpread() {
        assertTrue(mediaUrls(presets.modal.config).any { it.endsWith("product_lumen_square") })
        assertTrue(mediaUrls(presets.stories.config).any { it.endsWith("product_orbit_portrait") })
        assertTrue(
            mediaUrls(presets.fullscreen.config).any { it.endsWith("product_orbit_portrait") })
        val carousel = mediaUrls(presets.carousel.config)
        assertTrue(carousel.any { it.endsWith("_square") })
        assertTrue(carousel.any { it.endsWith("_portrait") })
        assertTrue(carousel.any { !it.endsWith("_square") && !it.endsWith("_portrait") })
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
    }
}
