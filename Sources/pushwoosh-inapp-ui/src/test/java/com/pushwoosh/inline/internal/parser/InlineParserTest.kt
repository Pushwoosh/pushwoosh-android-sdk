package com.pushwoosh.inline.internal.parser

import com.pushwoosh.inline.internal.FIXTURE_KEYS
import com.pushwoosh.inline.internal.controlBanner
import com.pushwoosh.inline.internal.fixtureBanners
import com.pushwoosh.inline.internal.htmlBanner
import com.pushwoosh.inline.internal.model.InlineAction
import com.pushwoosh.inline.internal.model.InlineColor
import com.pushwoosh.inline.internal.model.InlineFormat
import com.pushwoosh.inline.internal.model.InlineInsets
import com.pushwoosh.inline.internal.model.InlineNode
import com.pushwoosh.inline.internal.nativeBanner
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InlineParserTest {

    // Verifies that every node type, style and dark override in the shared iOS fixture parses.
    @Test
    fun testParsesFixtureBannersOfEveryNodeType() {
        for (key in FIXTURE_KEYS) {
            for (banner in fixtureBanners(key)) {
                assertNotNull(key, InlineParser.parse(banner, "s"))
            }
        }
    }

    // Verifies that missing bannerId / negative version / missing format / html "" drop the banner (table, labelled).
    @Test
    fun testMissingRequiredBannerFieldsDropBanner() {
        val cases = mapOf(
            "missing bannerId" to nativeBanner { remove("bannerId") },
            "empty bannerId" to nativeBanner { put("bannerId", "") },
            "missing version" to nativeBanner { remove("version") },
            "negative version" to nativeBanner { put("version", -1) },
            "fractional version" to nativeBanner { put("version", 1.5) },
            "missing format" to nativeBanner { remove("format") },
            "unknown format" to nativeBanner { put("format", "amp") },
            "native without native" to nativeBanner { remove("native") },
            "native without root" to nativeBanner { put("native", JSONObject()) },
            "empty html" to htmlBanner(html = ""),
            "html without html" to htmlBanner().apply { remove("html") },
            "zero maxHeight" to nativeBanner { put("maxHeight", 0) },
        )
        for ((name, banner) in cases) {
            assertNull(name, InlineParser.parse(banner, "s"))
        }
        assertEquals("<p>x</p>", InlineParser.parse(htmlBanner(), "s")?.html)
    }

    // Verifies that a control banner needs no format and parses as control.
    @Test
    fun testControlBannerWithoutFormatParses() {
        val banner = InlineParser.parse(controlBanner(), "home")

        assertNotNull(banner)
        assertTrue(banner!!.isControl)
        assertNull(banner.root)
        assertEquals(InlineFormat.NONE, banner.format)
        assertEquals("", banner.toPublic().format)
    }

    // Verifies that an unknown node type, unknown enum value or wrongly typed known key drops the whole banner (table: type:"carousel", axis:"diag", lines:"2", strike:1, version:true).
    @Test
    fun testInvalidNodeDropsWholeBanner() {
        val cases = mapOf(
            "unknown nested type" to withRoot(stack("""{"type":"carousel"}""")),
            "unknown axis" to withRoot(node("""{"type":"stack","axis":"diag","children":[{"type":"spacer"}]}""")),
            "string lines" to withRoot(node("""{"type":"text","text":"a","lines":"2"}""")),
            "numeric strike" to withRoot(node("""{"type":"text","text":"a","strike":1}""")),
            "boolean version" to nativeBanner { put("version", true) },
            "numeric test flag" to nativeBanner { put("test", 1) },
            "string control flag" to controlBanner().put("control", "true"),
            "numeric glass" to withRoot(node("""{"type":"spacer","glass":1}""")),
            "boolean size" to withRoot(node("""{"type":"text","text":"a","size":true}""")),
            "color without hash" to withRoot(node("""{"type":"text","text":"a","color":"ffffff"}""")),
            "invalid dark color" to withRoot(node("""{"type":"text","text":"a","dark":{"color":"#zz"}}""")),
            "dark not an object" to withRoot(node("""{"type":"text","text":"a","dark":"#000"}""")),
            "missing node type" to withRoot(node("""{"text":"a"}""")),
            "empty children" to withRoot(node("""{"type":"stack","children":[]}""")),
            "gradient of one color" to withRoot(node("""{"type":"spacer","gradient":{"colors":["#fff"]}}""")),
            "border without color" to withRoot(node("""{"type":"spacer","border":{"width":2}}""")),
            "missing text" to withRoot(node("""{"type":"text"}""")),
            "empty button title" to withRoot(node("""{"type":"button","title":""}""")),
        )
        for ((name, banner) in cases) {
            assertNull(name, InlineParser.parse(banner, "s"))
        }
    }

    // Verifies that depth 16 is refused and depth 15 accepted.
    @Test
    fun testNodeDepthLimit() {
        assertNotNull(InlineParser.parse(withRoot(nested(deepestDepth = 15)), "s"))
        assertNull(InlineParser.parse(withRoot(nested(deepestDepth = 16)), "s"))
    }

    // Verifies that lengths > 4096, font size 0 or > 256, aspect outside 0.1...20, pager interval < 1 drop the banner and the boundary values pass.
    @Test
    fun testLimitsDropBannerAndBoundariesPass() {
        val rejected = mapOf(
            "spacing 4097" to """{"type":"stack","spacing":4097,"children":[{"type":"spacer"}]}""",
            "padding 4097" to """{"type":"spacer","padding":4097}""",
            "negative radius" to """{"type":"spacer","cornerRadius":-1}""",
            "font size 0" to """{"type":"text","text":"a","size":0}""",
            "font size 257" to """{"type":"text","text":"a","size":257}""",
            "aspect 0.09" to """{"type":"image","url":"https://h/a.png","aspect":0.09}""",
            "aspect 20.1" to """{"type":"image","url":"https://h/a.png","aspect":20.1}""",
            "interval 0.9" to """{"type":"pager","interval":0.9,"pages":[{"type":"spacer"}]}""",
            "divider thickness 0" to """{"type":"divider","thickness":0}""",
            "negative lines" to """{"type":"text","text":"a","lines":-1}""",
        )
        for ((name, json) in rejected) {
            assertNull(name, InlineParser.parse(withRoot(node(json)), "s"))
        }
        val accepted = mapOf(
            "spacing 4096" to """{"type":"stack","spacing":4096,"children":[{"type":"spacer"}]}""",
            "font size 256" to """{"type":"text","text":"a","size":256}""",
            "aspect 0.1" to """{"type":"image","url":"https://h/a.png","aspect":0.1}""",
            "aspect 20" to """{"type":"image","url":"https://h/a.png","aspect":20}""",
            "interval 1" to """{"type":"pager","interval":1,"pages":[{"type":"spacer"}]}""",
        )
        for ((name, json) in accepted) {
            assertNotNull(name, InlineParser.parse(withRoot(node(json)), "s"))
        }
        assertNotNull(InlineParser.parse(nativeBanner { put("maxHeight", 4096) }, "s"))
        assertNull(InlineParser.parse(nativeBanner { put("maxHeight", 4097) }, "s"))
    }

    // Verifies that an unknown action type is ignored (action null) and the banner stays.
    @Test
    fun testUnknownActionTypeIsIgnored() {
        val banner = InlineParser.parse(
            withRoot(node("""{"type":"text","text":"a","action":{"type":"share"}}""")).put(
                "action",
                JSONObject("""{"type":"share"}"""),
            ),
            "s",
        )

        assertNotNull(banner)
        assertNull(banner!!.action)
        assertNull(banner.root!!.action)
    }

    // Verifies that a banner-level close action is ignored while a node close action is kept.
    @Test
    fun testBannerActionCloseIsIgnored() {
        val banner = InlineParser.parse(
            withRoot(node("""{"type":"text","text":"a","action":{"type":"close"}}""")).put(
                "action",
                JSONObject("""{"type":"close"}"""),
            ),
            "s",
        )

        assertNotNull(banner)
        assertNull(banner!!.action)
        assertEquals(InlineAction.Close, banner.root!!.action)
    }

    // Verifies that url actions accept only http(s) and image URLs with spaces / non-ASCII are percent-encoded.
    @Test
    fun testUrlRulesAndEncoding() {
        assertNull(InlineParser.parse(nativeBanner { put("action", JSONObject("""{"type":"url","value":"ftp://x"}""")) }, "s"))
        assertNull(InlineParser.parse(nativeBanner { put("action", JSONObject("""{"type":"url","value":"x"}""")) }, "s"))
        assertEquals(
            InlineAction.Url("HTTPS://shop.example/deals"),
            InlineParser.parse(nativeBanner { put("action", JSONObject("""{"type":"url","value":"HTTPS://shop.example/deals"}""")) }, "s")?.action,
        )
        assertEquals(
            InlineAction.Deeplink("pushmart://cart"),
            InlineParser.parse(nativeBanner { put("action", JSONObject("""{"type":"deeplink","value":"pushmart://cart"}""")) }, "s")?.action,
        )

        val image = InlineParser.parse(withRoot(node("""{"type":"image","url":"https://h/a b/é.png"}""")), "s")?.root

        assertEquals("https://h/a%20b/%C3%A9.png", (image as InlineNode.Image).url)
    }

    // Verifies that payload keeps string values and drops other-typed ones individually.
    @Test
    fun testPayloadKeepsStringValuesBesideOthers() {
        val banner = InlineParser.parse(nativeBanner { put("payload", JSONObject("""{"a":"1","b":2}""")) }, "s")

        assertEquals(mapOf("a" to "1"), banner?.payload)
        assertEquals(mapOf("a" to "1"), banner?.toPublic()?.payload)
    }

    // Verifies that unknown keys are ignored and the node id defaults to its type.
    @Test
    fun testUnknownKeysIgnoredAndIdDefaultsToType() {
        val banner = InlineParser.parse(
            withRoot(stack("""{"type":"spacer","flavor":"x"}""", """{"type":"spacer","id":"gap"}"""))
                .put("campaignColor", 42),
            "s",
        )

        val children = (banner?.root as InlineNode.Stack).children
        assertEquals(listOf("spacer", "gap"), children.map { it.id })
    }

    // Verifies that a non-object value (string, number, array) yields no banner.
    @Test
    fun testNonObjectValueGivesNoBanner() {
        val cases = mapOf<String, Any?>(
            "string" to "banner",
            "number" to 5,
            "array" to JSONArray().put(nativeBanner()),
            "null" to null,
            "json null" to JSONObject.NULL,
        )
        for ((name, value) in cases) {
            assertNull(name, InlineParser.parse(value, "s"))
        }
    }

    // Verifies that padding accepts a number, [vertical, horizontal] and [top, right, bottom, left] and rejects other list sizes.
    @Test
    fun testPaddingForms() {
        assertEquals(InlineInsets(4f, 4f, 4f, 4f), paddingOf("4"))
        assertEquals(InlineInsets(4f, 8f, 4f, 8f), paddingOf("[4,8]"))
        assertEquals(InlineInsets(1f, 2f, 3f, 4f), paddingOf("[1,2,3,4]"))
        assertNull(InlineParser.parse(withRoot(node("""{"type":"spacer","padding":[1,2,3]}""")), "s"))
    }

    // Verifies that a dark override replaces only themable keys and folds its color into the node color.
    @Test
    fun testDarkOverrideKeepsThemableKeysOnly() {
        val root = InlineParser.parse(
            withRoot(node("""{"type":"text","text":"a","color":"#000000","background":"#ffffff","dark":{"color":"#ffffff","background":"#000000","size":40}}""")),
            "s",
        )?.root as InlineNode.Text

        assertEquals(InlineColor(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), root.color)
        assertEquals(0xFF000000.toInt(), root.dark?.background)
        assertEquals(15f, root.size)
    }

    // Verifies that links Android opens but a strict URI parser refuses keep the banner and stay byte-for-byte, and media URLs with them parse.
    @Test
    fun testAndroidLinksKeepBannerAndStayUnchanged() {
        val links = listOf("myapp://", "my_app://screen", "myapp://path?x=a b", "https://cdn.example.com/a[1].png")
        for (link in links) {
            assertEquals("deeplink $link", InlineAction.Deeplink(link), actionOf("deeplink", link))
            assertNotNull("image $link", rootOf(JSONObject().put("type", "image").put("url", link)))
            assertNotNull("video $link", rootOf(JSONObject().put("type", "video").put("url", link).put("poster", link)))
        }
        assertEquals(InlineAction.Url("https://cdn.example.com/a[1].png"), actionOf("url", "https://cdn.example.com/a[1].png"))
        assertEquals(InlineAction.Url("https://shop.example/q?x=a b"), actionOf("url", "https://shop.example/q?x=a b"))

        val image = rootOf(JSONObject().put("type", "image").put("url", "https://cdn.example.com/a[1].png")) as InlineNode.Image
        assertEquals("https://cdn.example.com/a[1].png", image.url)
        val spaced = rootOf(JSONObject().put("type", "image").put("url", "myapp://path?x=a b")) as InlineNode.Image
        assertEquals("myapp://path?x=a%20b", spaced.url)
    }

    // Verifies that the relaxed link rule still drops the banner for blank, scheme-less and malformed-scheme values and for url actions that are not http(s).
    @Test
    fun testInvalidLinksStillDropBanner() {
        val banner = { type: String, value: String ->
            InlineParser.parse(nativeBanner { put("action", JSONObject().put("type", type).put("value", value)) }, "s")
        }
        for (value in listOf("", "   ", "screen/1", "://x", "1app://x", "my app://x", " myapp://x", "myapp:", "//cdn.example.com/x")) {
            assertNull("deeplink '$value'", banner("deeplink", value))
        }
        for (value in listOf("javascript:alert(1)", "file:///sdcard/x.html", "ftp://x", "my_app://screen", "x", "https:")) {
            assertNull("url '$value'", banner("url", value))
        }
        for (value in listOf("a.png", "//cdn.example.com/a.png", "")) {
            assertNull("image '$value'", rootOf(JSONObject().put("type", "image").put("url", value)))
        }
    }

    private fun actionOf(type: String, value: String) =
        InlineParser.parse(nativeBanner { put("action", JSONObject().put("type", type).put("value", value)) }, "s")?.action

    private fun rootOf(node: JSONObject) = InlineParser.parse(withRoot(node), "s")?.root

    private fun paddingOf(padding: String): InlineInsets? =
        InlineParser.parse(withRoot(node("""{"type":"spacer","padding":$padding}""")), "s")?.root?.style?.padding

    private fun node(json: String) = JSONObject(json)

    private fun stack(vararg children: String) =
        JSONObject().put("type", "stack").put("children", JSONArray(children.map { JSONObject(it) }))

    private fun withRoot(root: JSONObject) = nativeBanner { put("native", JSONObject().put("root", root)) }

    private fun nested(deepestDepth: Int): JSONObject {
        var node = JSONObject().put("type", "spacer")
        repeat(deepestDepth) { node = JSONObject().put("type", "stack").put("children", JSONArray().put(node)) }
        return node
    }
}
