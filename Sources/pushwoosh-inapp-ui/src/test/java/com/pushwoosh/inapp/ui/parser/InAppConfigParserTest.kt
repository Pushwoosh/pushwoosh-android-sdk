package com.pushwoosh.inapp.ui.parser

import com.pushwoosh.inapp.ui.model.BannerPosition
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.StoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InAppConfigParserTest {

    // MARK: - Gate

    @Test
    fun returnsNullWhenNoDisplayType() {
        assertNull(InAppConfigParser.parse("""{"modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}"""))
    }

    @Test
    fun returnsNullForGarbage() {
        assertNull(InAppConfigParser.parse("not json"))
        assertNull(InAppConfigParser.parse(""))
        assertNull(InAppConfigParser.parse(null))
    }

    @Test
    fun returnsNullWhenBlockMissing() {
        assertNull(InAppConfigParser.parse("""{"displayType":"modal"}"""))
    }

    @Test
    fun returnsNullForUnknownDisplayType() {
        assertNull(InAppConfigParser.parse("""{"displayType":"spinwheel","spinwheel":{"showClose":true}}"""))
    }

    // MARK: - Canonical parse (spot-checks)

    @Test
    fun parsesModal() {
        val json = """
            {"displayType":"modal","inAppId":"promo","modal":{
              "showClose":false,"dimBackground":true,"background":"#FFFFFFFF",
              "title":{"text":"Hi","color":"#000000FF"},
              "message":{"text":"body","color":"#111111FF"},
              "image":"https://x/i.png",
              "buttons":[{
                "text":{"text":"Go","color":"#FFFFFFFF"},
                "background":"#0E72E5FF",
                "border":{"color":"#0E72E5FF","radius":12},
                "action":{"type":"url","url":"app://x"}
              }]}}
        """.trimIndent()
        val msg = InAppConfigParser.parse(json)
        assertNotNull(msg)
        assertEquals("promo", msg!!.id)
        val modal = msg.layout as InAppLayout.Modal
        assertEquals("Hi", modal.content.title?.text)
        assertEquals("body", modal.content.message?.text)
        assertFalse(modal.content.showCloseButton)
        assertEquals(1, modal.content.buttons.size)
        assertEquals(InAppAction.Url("app://x"), modal.content.buttons[0].action)
        assertEquals("Go", modal.content.buttons[0].text.text)
    }

    @Test
    fun parsesBannerPositionAndAction() {
        val banner = InAppConfigParser.parse("""
            {"displayType":"banner","banner":{
              "showClose":true,"position":"top","background":"#4B5057FF",
              "action":{"type":"close"}}}
        """.trimIndent())?.layout as InAppLayout.Banner
        assertEquals(BannerPosition.TOP, banner.content.position)
        assertEquals(InAppAction.Close, banner.content.action)
    }

    @Test
    fun parsesFullscreenCoverBackground() {
        val fs = InAppConfigParser.parse("""
            {"displayType":"fullscreen","fullscreen":{
              "showClose":true,
              "cover":{"image":"https://x/c.jpg","background":"#1A1A1EFF"},
              "buttons":[]}}
        """.trimIndent())?.layout as InAppLayout.Fullscreen
        assertEquals("https://x/c.jpg", fs.content.imageUrl)
        assertEquals(0xFF1A1A1E.toInt(), fs.content.backgroundColor)
    }

    @Test
    fun parsesStoriesButtonsAndClampsDuration() {
        val stories = InAppConfigParser.parse("""
            {"displayType":"stories","stories":{
              "showClose":true,"loop":false,
              "items":[{"duration":100,"buttons":[{
                "text":{"text":"Go","color":"#FFFFFFFF"},
                "background":"#000000FF",
                "border":{"color":"#000000FF","radius":26},
                "action":{"type":"url","url":"app://x"}}]}]}}
        """.trimIndent())?.layout as InAppLayout.Stories
        assertEquals(StoryItem.MAX_DURATION_MS, stories.content.items[0].durationMs)
        assertEquals(1, stories.content.items[0].buttons.size)
    }

    @Test
    fun parsesCarouselItemFields() {
        val carousel = InAppConfigParser.parse("""
            {"displayType":"carousel","carousel":{"showClose":true,"items":[
              {"image":"https://x/1.png",
               "title":{"text":"A","color":"#FFFFFFFF"},
               "message":{"text":"B","color":"#000000FF"},
               "action":{"type":"url","url":"app://c"}}]}}
        """.trimIndent())?.layout as InAppLayout.Carousel
        val item = carousel.content.items[0]
        assertEquals("https://x/1.png", item.imageUrl)
        assertEquals("A", item.title?.text)
        assertEquals("B", item.message?.text)
        assertEquals(InAppAction.Url("app://c"), item.action)
    }

    @Test
    fun blockWithoutVisibleContentIsValid() {
        // modal with all required fields but no title/message/image and empty buttons — shown as-is.
        assertNotNull(InAppConfigParser.parse("""
            {"displayType":"modal","modal":{
              "showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}
        """.trimIndent()))
    }

    // MARK: - Strict rejection

    @Test
    fun rejectsBooleanCoercion() {
        assertNull(InAppConfigParser.parse("""
            {"displayType":"modal","modal":{
              "showClose":"true","dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}
        """.trimIndent()))
    }

    @Test
    fun rejectsDurationCoercion() {
        assertNull(InAppConfigParser.parse("""
            {"displayType":"stories","stories":{"showClose":true,"loop":false,
              "items":[{"duration":"4","buttons":[]}]}}
        """.trimIndent()))
    }

    @Test
    fun rejectsNonPositiveDuration() {
        assertNull(InAppConfigParser.parse("""
            {"displayType":"stories","stories":{"showClose":true,"loop":false,
              "items":[{"duration":0,"buttons":[]}]}}
        """.trimIndent()))
    }

    @Test
    fun rejectsColorWithoutHash() {
        assertNull(InAppConfigParser.parse("""
            {"displayType":"modal","modal":{
              "showClose":true,"dimBackground":true,"background":"FFFFFFFF","buttons":[]}}
        """.trimIndent()))
    }

    @Test
    fun rejectsPositionOutsideEnum() {
        assertNull(InAppConfigParser.parse("""
            {"displayType":"banner","banner":{
              "showClose":true,"position":"left","background":"#4B5057FF","action":{"type":"close"}}}
        """.trimIndent()))
    }

    @Test
    fun rejectsEmptyRequiredItems() {
        assertNull(InAppConfigParser.parse("""{"displayType":"carousel","carousel":{"showClose":true,"items":[]}}"""))
    }

    @Test
    fun rejectsMissingButtonBorder() {
        assertNull(InAppConfigParser.parse("""
            {"displayType":"modal","modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF",
              "buttons":[{"text":{"text":"Go","color":"#FFFFFFFF"},"background":"#000000FF","action":{"type":"close"}}]}}
        """.trimIndent()))
    }

    // MARK: - Forward-compat & envelope

    @Test
    fun ignoresUnknownKeys() {
        assertNotNull(InAppConfigParser.parse("""
            {"displayType":"modal","futureField":42,"modal":{
              "showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[],"futureBlockKey":"x"}}
        """.trimIndent()))
    }

    @Test
    fun toleratesStringifiedEnvelopeNumbers() {
        val msg = InAppConfigParser.parse("""
            {"displayType":"modal","maxDisplays":"3","cooldown":"60","modal":{
              "showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}
        """.trimIndent())
        assertEquals(3, msg?.maxDisplays)
        assertEquals(60L, msg?.cooldownSec)
    }

    @Test
    fun expireDateSetsExpireEpochSecVerbatim() {
        val msg = InAppConfigParser.parse("""
            {"displayType":"modal","expireDate":1700000000,"modal":{
              "showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}
        """.trimIndent())
        assertEquals(1700000000L, msg?.expireEpochSec)
    }

    @Test
    fun ttlConvertsToFutureEpochRelativeToNow() {
        val before = System.currentTimeMillis() / 1000
        val msg = InAppConfigParser.parse("""
            {"displayType":"modal","ttl":3600,"modal":{
              "showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}
        """.trimIndent())
        val after = System.currentTimeMillis() / 1000
        val expiry = msg?.expireEpochSec!!
        assertTrue("expiry $expiry outside [${before + 3600}, ${after + 3600}]",
            expiry in (before + 3600)..(after + 3600))
    }

    @Test
    fun expireDateTakesPrecedenceOverTtl() {
        val msg = InAppConfigParser.parse("""
            {"displayType":"modal","expireDate":1700000000,"ttl":3600,"modal":{
              "showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}
        """.trimIndent())
        assertEquals(1700000000L, msg?.expireEpochSec)
    }

    @Test
    fun nonPositiveTtlIsIgnored() {
        for (ttl in listOf(0, -5)) {
            val msg = InAppConfigParser.parse("""
                {"displayType":"modal","ttl":$ttl,"modal":{
                  "showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}
            """.trimIndent())
            assertNull(msg?.expireEpochSec)
        }
    }

    // MARK: - Strict readers (shared)

    @Test
    fun rejectsEmptyRequiredString() {
        // A present-but-empty required string (button text) fails closed — strictString requires non-empty.
        assertNull(InAppConfigParser.parse("""
            {"displayType":"modal","modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF",
              "buttons":[{"text":{"text":"","color":"#FFFFFFFF"},"background":"#000000FF",
              "border":{"color":"#000000FF","radius":12},"action":{"type":"close"}}]}}
        """.trimIndent()))
    }

    @Test
    fun readsBooleanTrueVerbatim() {
        // strictBool must return the actual value, not a constant — true flags survive as true.
        val modal = InAppConfigParser.parse("""
            {"displayType":"modal","modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}
        """.trimIndent())?.layout as InAppLayout.Modal
        assertTrue(modal.content.showCloseButton)
        assertTrue(modal.content.dimsBackground)
    }

    @Test
    fun bannerAutoDismissConvertsSecondsToMillis() {
        // autoDismiss is authored in seconds and stored in millis; 6 -> 6000, not 0 or 0.006.
        val banner = InAppConfigParser.parse("""
            {"displayType":"banner","banner":{"showClose":true,"position":"top","background":"#4B5057FF",
              "action":{"type":"close"},"autoDismiss":6}}
        """.trimIndent())?.layout as InAppLayout.Banner
        assertEquals(6000L, banner.content.autoDismissMs)
    }
}
