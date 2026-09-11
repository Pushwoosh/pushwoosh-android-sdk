package com.pushwoosh.inapp.ui.parser

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InAppDarkOverlayTest {

    private fun block(json: String) = JSONObject(json)

    private val modalBlock = """
        {"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","image":"light.png",
         "title":{"text":"Hi","color":"#111111FF"},
         "message":{"text":"body","color":"#333333FF"},
         "buttons":[{"text":{"text":"Go","color":"#FFFFFFFF"},"background":"#0E72E5FF",
                     "border":{"color":"#0E72E5FF","radius":12},
                     "action":{"type":"url","url":"app://x"}}],
         "dark":%s}
    """.trimIndent()

    private fun modalWithDark(dark: String) = block(modalBlock.format(dark))

    @Test
    fun mergesVisualLeavesAndNestedColors() {
        val merged = InAppDarkOverlay.merge(modalWithDark(
            """{"background":"#101014FF","image":"dark.png","title":{"color":"#F2F2F7FF"}}"""))!!
        assertEquals("#101014FF", merged.getString("background"))
        assertEquals("dark.png", merged.getString("image"))
        assertEquals("#F2F2F7FF", merged.getJSONObject("title").getString("color"))
        assertEquals("Hi", merged.getJSONObject("title").getString("text"))
        assertFalse("dark must be consumed", merged.has("dark"))
    }

    @Test
    fun sparseOverlayInheritsTheRest() {
        val merged = InAppDarkOverlay.merge(modalWithDark("""{"background":"#101014FF"}"""))!!
        assertEquals("#111111FF", merged.getJSONObject("title").getString("color"))
        assertEquals("light.png", merged.getString("image"))
        assertEquals(true, merged.getBoolean("showClose"))
    }

    @Test
    fun nonObjectDarkIsBroken() {
        assertNull(InAppDarkOverlay.merge(modalWithDark(""""night"""")))
        assertNull(InAppDarkOverlay.merge(modalWithDark("42")))
        assertNull(InAppDarkOverlay.merge(modalWithDark("""["#000000FF"]""")))
    }

    @Test
    fun textActionUnknownAndNestedDarkKeysAreIgnored() {
        val merged = InAppDarkOverlay.merge(modalWithDark(
            """{"title":{"text":"Dark Hi"},"action":{"type":"close"},"wat":42,
                "dark":{"background":"#000000FF"}}"""))!!
        assertEquals("Hi", merged.getJSONObject("title").getString("text"))
        assertFalse(merged.has("wat"))
        assertFalse(merged.has("dark"))
        assertEquals("#FFFFFFFF", merged.getString("background"))
    }

    @Test
    fun buttonsMergePositionallyVisualKeysOnly() {
        val merged = InAppDarkOverlay.merge(modalWithDark(
            """{"buttons":[{"background":"#8AB4F8FF","text":{"color":"#000000FF","text":"Stop"},
                            "border":{"color":"#8AB4F8FF","radius":99},
                            "action":{"type":"close"}}]}"""))!!
        val button = merged.getJSONArray("buttons").getJSONObject(0)
        assertEquals("#8AB4F8FF", button.getString("background"))
        assertEquals("#000000FF", button.getJSONObject("text").getString("color"))
        assertEquals("Go", button.getJSONObject("text").getString("text"))
        assertEquals("#8AB4F8FF", button.getJSONObject("border").getString("color"))
        assertEquals(12, button.getJSONObject("border").getInt("radius"))
        assertEquals("url", button.getJSONObject("action").getString("type"))
    }

    @Test
    fun arrayLengthMismatchIsBroken() {
        assertNull(InAppDarkOverlay.merge(modalWithDark("""{"buttons":[]}""")))
        assertNull(InAppDarkOverlay.merge(modalWithDark(
            """{"buttons":[{"background":"#000000FF"},{"background":"#000000FF"}]}""")))
    }

    @Test
    fun nonArrayButtonsAndNonObjectElementsAreBroken() {
        assertNull(InAppDarkOverlay.merge(modalWithDark("""{"buttons":5}""")))
        assertNull(InAppDarkOverlay.merge(modalWithDark("""{"buttons":["x"]}""")))
    }

    @Test
    fun nestedItemsButtonsMergePositionally() {
        val stories = block(
            """{"showClose":true,"loop":false,
                "items":[{"duration":5,"image":"s1.png",
                          "buttons":[{"text":{"text":"Buy","color":"#FFFFFFFF"},"background":"#0E72E5FF",
                                      "border":{"color":"#0E72E5FF","radius":8},
                                      "action":{"type":"close"}}]}],
                "dark":{"items":[{"image":"s1-dark.png",
                                  "buttons":[{"background":"#8AB4F8FF"}]}]}}""")
        val merged = InAppDarkOverlay.merge(stories)!!
        val item = merged.getJSONArray("items").getJSONObject(0)
        assertEquals("s1-dark.png", item.getString("image"))
        assertEquals(5, item.getInt("duration"))
        assertEquals("#8AB4F8FF", item.getJSONArray("buttons").getJSONObject(0).getString("background"))
        assertEquals("Buy",
            item.getJSONArray("buttons").getJSONObject(0).getJSONObject("text").getString("text"))
    }

    @Test
    fun videoContentUrlIsNotThemable() {
        val video = block(
            """{"showClose":true,"loop":false,"muted":true,"url":"v.mp4","poster":"p.png",
                "fallback":"f.png","buttons":[],
                "dark":{"url":"dark.mp4","poster":"p-dark.png","fallback":"f-dark.png"}}""")
        val merged = InAppDarkOverlay.merge(video)!!
        assertEquals("v.mp4", merged.getString("url"))
        assertEquals("p-dark.png", merged.getString("poster"))
        assertEquals("f-dark.png", merged.getString("fallback"))
    }

    @Test
    fun coverMergesRecursively() {
        val fullscreen = block(
            """{"showClose":true,"buttons":[],
                "cover":{"background":"#FFFFFFFF","image":"hero.png"},
                "dark":{"cover":{"background":"#101014FF","image":"hero-dark.png"}}}""")
        val merged = InAppDarkOverlay.merge(fullscreen)!!
        assertEquals("#101014FF", merged.getJSONObject("cover").getString("background"))
        assertEquals("hero-dark.png", merged.getJSONObject("cover").getString("image"))
    }

    @Test
    fun nullValuesInDarkCannotDeleteKeys() {
        val merged = InAppDarkOverlay.merge(modalWithDark("""{"image":null}"""))!!
        assertEquals("light.png", merged.getString("image"))
    }

    @Test
    fun invalidLeafValueIsCopiedThroughForStrictParserToReject() {
        val merged = InAppDarkOverlay.merge(modalWithDark("""{"background":"#GGGGGG"}"""))
        assertNotNull("value errors are the strict parser's job", merged)
        assertEquals("#GGGGGG", merged!!.getString("background"))
    }

    @Test
    fun inputBlockIsNotMutated() {
        val input = modalWithDark("""{"background":"#101014FF"}""")
        val before = input.toString()
        InAppDarkOverlay.merge(input)
        assertEquals(before, input.toString())
    }

    @Test
    fun mergedResultHoldsNoLiveReferenceIntoDarkSubtree() {
        val input = modalWithDark("""{"background":{"wat":1}}""")
        val inputBefore = input.toString()
        val merged = InAppDarkOverlay.merge(input)!!
        val mergedBg = merged.getJSONObject("background")
        mergedBg.put("wat", 999)
        assertEquals(inputBefore, input.toString())
    }

    @Test
    fun scalarInDarkOverObjectInLightIsIgnored() {
        val merged = InAppDarkOverlay.merge(modalWithDark(
            """{"title":"#FFF","background":"#101014FF"}"""))!!
        assertNotNull(merged)
        assertEquals("#101014FF", merged.getString("background"))
        assertEquals("Hi", merged.getJSONObject("title").getString("text"))
        assertEquals("#111111FF", merged.getJSONObject("title").getString("color"))
    }
}
