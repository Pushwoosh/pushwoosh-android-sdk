/*
 *
 * Copyright (c) 2026. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.inbox.ui.presentation.view.adapter.inbox

import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.fakeInboxMessage
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Mirrors the iOS PushwooshInboxKit resolver contract: server-driven
 * `displayType` read from actionParams (root, `u` dict, `u` as a JSON-encoded
 * string, legacy `userdata`), the iOS image/title heuristic when no known
 * displayType is present (image + title -> captioned, image alone -> banner,
 * neither -> classic) — gated behind
 * PushwooshInboxStyle.richCardsHeuristicEnabled, off by default so existing
 * inboxes keep their legacy rows. A requested kind whose payload is missing
 * degrades to CLASSIC, never to the legacy row: banner/captioned need a hero
 * image, carousel at least one slide, video a descriptor; wallet is iOS-only
 * and always degrades. The hero URL chain is attachment -> b -> imageUrl ->
 * image/u.image.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InboxCardKindTest {

    @After
    fun tearDown() {
        PushwooshInboxStyle.richCardsHeuristicEnabled = false
    }

    private fun enableHeuristic() {
        PushwooshInboxStyle.richCardsHeuristicEnabled = true
    }

    private fun msg(
        actionParams: String? = null,
        bannerUrl: String? = null,
        imageUrl: String? = null,
        title: String? = null
    ): InboxMessage =
        fakeInboxMessage(actionParams = actionParams, bannerUrl = bannerUrl, imageUrl = imageUrl, title = title)

    // ---- resolve(): displayType wire shapes ----

    @Test
    fun resolve_displayTypeAtRoot_isBanner() {
        val m = msg(actionParams = """{"displayType":"banner","attachment":"https://img.example/hero.png"}""")
        assertEquals(InboxCardKind.BANNER, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_displayTypeInUDict_isBanner() {
        val m = msg(actionParams = """{"u":{"displayType":"banner"},"attachment":"https://img.example/hero.png"}""")
        assertEquals(InboxCardKind.BANNER, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_displayTypeInUJsonString_isBanner() {
        val m = msg(actionParams = """{"u":"{\"displayType\":\"banner\"}","attachment":"https://img.example/hero.png"}""")
        assertEquals(InboxCardKind.BANNER, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_displayTypeInUserdata_isBanner() {
        val m = msg(actionParams = """{"userdata":{"displayType":"banner"},"attachment":"https://img.example/hero.png"}""")
        assertEquals(InboxCardKind.BANNER, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_displayTypeIsCaseInsensitive() {
        val m = msg(actionParams = """{"displayType":"Banner","attachment":"https://img.example/hero.png"}""")
        assertEquals(InboxCardKind.BANNER, InboxCardKind.resolve(m))
    }

    // ---- resolve(): captioned displayType ----

    @Test
    fun resolve_displayTypeCaptioned_isCaptioned() {
        val m = msg(actionParams = """{"displayType":"captioned","attachment":"https://img.example/hero.png"}""")
        assertEquals(InboxCardKind.CAPTIONED, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_captionedWithoutAnyImage_degradesToClassic() {
        val m = msg(actionParams = """{"displayType":"captioned"}""", title = "title")
        assertEquals(InboxCardKind.CLASSIC, InboxCardKind.resolve(m))
    }

    // ---- resolve(): the iOS image/title heuristic (opt-in) ----

    @Test
    fun resolve_heuristicOffByDefault_legacyMessagesKeepTheirRow() {
        // An icon + title message from an existing integration must stay a legacy row.
        val m = msg(imageUrl = "https://img.example/icon.png", title = "title")
        assertEquals(InboxCardKind.DEFAULT, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_displayTypeWorksRegardlessOfHeuristicFlag() {
        val m = msg(actionParams = """{"displayType":"banner","attachment":"https://img.example/hero.png"}""")
        assertEquals(InboxCardKind.BANNER, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_heuristic_imageWithTitle_isCaptioned() {
        enableHeuristic()
        val m = msg(imageUrl = "https://img.example/icon.png", title = "title")
        assertEquals(InboxCardKind.CAPTIONED, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_heuristic_imageWithoutTitle_isBanner() {
        enableHeuristic()
        val m = msg(actionParams = """{"attachment":"https://img.example/hero.png"}""", imageUrl = "https://img.example/icon.png")
        assertEquals(InboxCardKind.BANNER, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_heuristic_emptyTitle_isBanner() {
        enableHeuristic()
        val m = msg(imageUrl = "https://img.example/icon.png", title = "")
        assertEquals(InboxCardKind.BANNER, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_heuristic_noImage_isClassic() {
        enableHeuristic()
        assertEquals(InboxCardKind.CLASSIC, InboxCardKind.resolve(msg(title = "title")))
    }

    @Test
    fun resolve_unknownDisplayType_fallsBackToHeuristic() {
        enableHeuristic()
        val m = msg(actionParams = """{"displayType":"fancy"}""", imageUrl = "https://img.example/icon.png", title = "title")
        assertEquals(InboxCardKind.CAPTIONED, InboxCardKind.resolve(m))
    }

    // ---- resolve(): known-but-unported kinds, degrade ----

    @Test
    fun resolve_richKindWithoutItsPayload_degradesToClassic() {
        // carousel needs slides, video a descriptor — an image alone satisfies neither,
        // so each degrades to the classic card, never to a legacy row.
        for (kind in listOf("carousel", "video")) {
            val m = msg(actionParams = """{"displayType":"$kind"}""", imageUrl = "https://img.example/icon.png", title = "title")
            assertEquals(kind, InboxCardKind.CLASSIC, InboxCardKind.resolve(m))
        }
    }

    @Test
    fun resolve_wallet_alwaysDegradesToClassic() {
        // Apple Wallet is iOS-only; a wallet message still renders as a card, not a legacy row.
        val withPass = msg(actionParams = """{"displayType":"wallet","wallet":"https://cdn/c.pkpass"}""", title = "t")
        assertEquals(InboxCardKind.CLASSIC, InboxCardKind.resolve(withPass))

        val withoutPass = msg(actionParams = """{"displayType":"wallet"}""", title = "t")
        assertEquals(InboxCardKind.CLASSIC, InboxCardKind.resolve(withoutPass))
    }

    @Test
    fun resolve_classicDisplayType_isClassic() {
        val m = msg(actionParams = """{"displayType":"classic"}""", title = "title")
        assertEquals(InboxCardKind.CLASSIC, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_carouselWithSlides_isCarousel() {
        val m = msg(actionParams = """{"displayType":"carousel","carousel":[{"image":"https://cdn/1.jpg"}]}""")
        assertEquals(InboxCardKind.CAROUSEL, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_videoWithDescriptor_isVideo() {
        val m = msg(actionParams = """{"displayType":"video","video":{"url":"https://cdn/clip.mp4"}}""")
        assertEquals(InboxCardKind.VIDEO, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_richKindsIgnoreHeuristicFlag() {
        // The flag gates only the image/title guess; an explicit displayType always wins.
        val m = msg(actionParams = """{"displayType":"video","video":{"url":"https://cdn/clip.mp4"}}""")
        assertEquals(InboxCardKind.VIDEO, InboxCardKind.resolve(m))
        enableHeuristic()
        assertEquals(InboxCardKind.VIDEO, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_bannerWithoutAnyImage_degradesToClassic() {
        val m = msg(actionParams = """{"displayType":"banner"}""")
        assertEquals(InboxCardKind.CLASSIC, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_nullActionParams_isDefault() {
        assertEquals(InboxCardKind.DEFAULT, InboxCardKind.resolve(msg()))
    }

    @Test
    fun resolve_malformedActionParams_isDefault() {
        val m = msg(actionParams = "not a json {{{")
        assertEquals(InboxCardKind.DEFAULT, InboxCardKind.resolve(m))
    }

    @Test
    fun resolve_bannerWithOnlyIconImage_staysBanner() {
        // Hero falls back to the icon, so the card still renders — mirrors iOS resolvedBannerURL.
        val m = msg(actionParams = """{"displayType":"banner"}""", imageUrl = "https://img.example/icon.png")
        assertEquals(InboxCardKind.BANNER, InboxCardKind.resolve(m))
    }

    // ---- resolveHeroUrl(): priority chain ----

    @Test
    fun heroUrl_prefersAttachmentOverBannerAndIcon() {
        val m = msg(
            actionParams = """{"attachment":"https://img.example/hero.png"}""",
            bannerUrl = "https://img.example/b.png",
            imageUrl = "https://img.example/icon.png"
        )
        assertEquals("https://img.example/hero.png", InboxCardKind.resolveHeroUrl(m))
    }

    @Test
    fun heroUrl_upperCaseHttpScheme_isAccepted() {
        val m = msg(actionParams = """{"attachment":"HTTPS://cdn.example.com/hero.png"}""")
        assertEquals("HTTPS://cdn.example.com/hero.png", InboxCardKind.resolveHeroUrl(m))
    }

    @Test
    fun heroUrl_nonHttpAttachment_isSkipped() {
        val m = msg(
            actionParams = """{"attachment":"file:///sdcard/hero.png"}""",
            bannerUrl = "https://img.example/b.png"
        )
        assertEquals("https://img.example/b.png", InboxCardKind.resolveHeroUrl(m))
    }

    @Test
    fun heroUrl_fallsBackToBannerUrl() {
        val m = msg(bannerUrl = "https://img.example/b.png", imageUrl = "https://img.example/icon.png")
        assertEquals("https://img.example/b.png", InboxCardKind.resolveHeroUrl(m))
    }

    @Test
    fun heroUrl_nonHttpBannerUrl_isSkipped() {
        val m = msg(bannerUrl = "ftp://img.example/b.png", imageUrl = "https://img.example/icon.png")
        assertEquals("https://img.example/icon.png", InboxCardKind.resolveHeroUrl(m))
    }

    @Test
    fun heroUrl_fallsBackToImageUrl() {
        val m = msg(imageUrl = "https://img.example/icon.png")
        assertEquals("https://img.example/icon.png", InboxCardKind.resolveHeroUrl(m))
    }

    @Test
    fun heroUrl_fallsBackToImageInActionParamsRoot() {
        val m = msg(actionParams = """{"image":"https://img.example/root.png"}""")
        assertEquals("https://img.example/root.png", InboxCardKind.resolveHeroUrl(m))
    }

    @Test
    fun heroUrl_fallsBackToImageInUDict() {
        val m = msg(actionParams = """{"u":{"image":"https://img.example/u.png"}}""")
        assertEquals("https://img.example/u.png", InboxCardKind.resolveHeroUrl(m))
    }

    @Test
    fun heroUrl_fallsBackToImageInUJsonString() {
        val m = msg(actionParams = """{"u":"{\"image\":\"https://img.example/us.png\"}"}""")
        assertEquals("https://img.example/us.png", InboxCardKind.resolveHeroUrl(m))
    }

    @Test
    fun heroUrl_noImageAnywhere_isNull() {
        assertNull(InboxCardKind.resolveHeroUrl(msg(actionParams = """{"foo":"bar"}""")))
        assertNull(InboxCardKind.resolveHeroUrl(msg()))
    }

    @Test
    fun heroUrl_emptyStrings_areSkipped() {
        val m = msg(actionParams = """{"attachment":"","image":""}""", bannerUrl = "", imageUrl = "")
        assertNull(InboxCardKind.resolveHeroUrl(m))
    }

    // ---- resolveIconUrl(): the message icon chain ----

    @Test
    fun iconUrl_prefersImageUrl() {
        val m = msg(actionParams = """{"image":"https://img.example/root.png"}""", imageUrl = "https://img.example/icon.png")
        assertEquals("https://img.example/icon.png", InboxCardKind.resolveIconUrl(m))
    }

    @Test
    fun iconUrl_fallsBackToImageInActionParamsRootThenU() {
        val root = msg(actionParams = """{"image":"https://img.example/root.png"}""")
        assertEquals("https://img.example/root.png", InboxCardKind.resolveIconUrl(root))
        val u = msg(actionParams = """{"u":"{\"image\":\"https://img.example/us.png\"}"}""")
        assertEquals("https://img.example/us.png", InboxCardKind.resolveIconUrl(u))
    }

    @Test
    fun iconUrl_ignoresAttachmentAndBanner() {
        val m = msg(actionParams = """{"attachment":"https://img.example/hero.png"}""", bannerUrl = "https://img.example/b.png")
        assertNull(InboxCardKind.resolveIconUrl(m))
    }

    @Test
    fun iconUrl_noIconAnywhere_isNull() {
        assertNull(InboxCardKind.resolveIconUrl(msg()))
    }

    // ---- isPinned(): wire shapes ----

    @Test
    fun isPinned_boolAtRoot() {
        assertTrue(InboxCardKind.isPinned(msg(actionParams = """{"pinned":true}""")))
        assertFalse(InboxCardKind.isPinned(msg(actionParams = """{"pinned":false}""")))
    }

    @Test
    fun isPinned_numberAtRoot() {
        assertTrue(InboxCardKind.isPinned(msg(actionParams = """{"pinned":1}""")))
        assertFalse(InboxCardKind.isPinned(msg(actionParams = """{"pinned":0}""")))
    }

    @Test
    fun isPinned_stringAtRoot() {
        assertTrue(InboxCardKind.isPinned(msg(actionParams = """{"pinned":"true"}""")))
        assertTrue(InboxCardKind.isPinned(msg(actionParams = """{"pinned":"True"}""")))
        assertFalse(InboxCardKind.isPinned(msg(actionParams = """{"pinned":"false"}""")))
    }

    @Test
    fun isPinned_inUDict() {
        assertTrue(InboxCardKind.isPinned(msg(actionParams = """{"u":{"pinned":true}}""")))
    }

    @Test
    fun isPinned_inUJsonString() {
        assertTrue(InboxCardKind.isPinned(msg(actionParams = """{"u":"{\"pinned\":true}"}""")))
    }

    @Test
    fun isPinned_inUserdata() {
        assertTrue(InboxCardKind.isPinned(msg(actionParams = """{"userdata":{"pinned":true}}""")))
    }

    @Test
    fun isPinned_absentOrMalformed_isFalse() {
        assertFalse(InboxCardKind.isPinned(msg()))
        assertFalse(InboxCardKind.isPinned(msg(actionParams = """{"foo":"bar"}""")))
        assertFalse(InboxCardKind.isPinned(msg(actionParams = "broken {{{")))
    }
}
