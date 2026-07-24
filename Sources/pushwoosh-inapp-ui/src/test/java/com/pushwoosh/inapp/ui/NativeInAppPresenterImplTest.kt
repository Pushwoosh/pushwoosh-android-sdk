package com.pushwoosh.inapp.ui

import com.pushwoosh.inapp.network.model.Resource
import com.pushwoosh.inapp.ui.parser.InAppConfigParser
import com.pushwoosh.inapp.view.InAppViewEvent
import com.pushwoosh.internal.event.EventBus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class NativeInAppPresenterImplTest {

    private val presenter = NativeInAppPresenterImpl()
    private val resource = Resource("test-code", false)

    private val modalNoId =
        """{"displayType":"modal","modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}"""

    private fun parsedId(json: String): String? = InAppConfigParser.parse(json)?.id

    @After
    fun tearDown() {
        NativeInAppAnalytics.reset()
    }

    @Test
    fun presentReturnsFalseForGarbage() {
        assertFalse(presenter.present("not json", resource))
    }

    @Test
    fun presentReturnsFalseWithoutDisplayType() {
        assertFalse(presenter.present("""{"modal":{"title":{"text":"hi"}}}""", resource))
    }

    @Test
    fun presentReturnsFalseForUnknownDisplayType() {
        assertFalse(presenter.present("""{"displayType":"pip"}""", resource))
    }

    @Test
    fun presentReturnsTrueForValidModalConfig() {
        val json = """{"displayType":"modal","modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}"""
        assertTrue(presenter.present(json, resource))
    }

    @Test
    fun presentRegistersResourceForShowAnalytics() {
        // present() now registers analytics under the patched rawJson (inAppId injected from
        // the resource code), so onShown must use the same patched key.
        val patched = ensureInAppId(modalNoId, resource.code)
        presenter.present(modalNoId, resource)

        val viewEvents = mutableListOf<InAppViewEvent>()
        val subscription = EventBus.subscribe(InAppViewEvent::class.java) { viewEvents.add(it) }
        try {
            NativeInAppAnalytics.onShown(patched)
            assertEquals(1, viewEvents.size)
            assertSame(resource, viewEvents[0].resource)
        } finally {
            subscription.unsubscribe()
        }
    }

    @Test
    fun presentKeysAnalyticsByPatchedJsonNotOriginal() {
        presenter.present(modalNoId, resource)

        val viewEvents = mutableListOf<InAppViewEvent>()
        val subscription = EventBus.subscribe(InAppViewEvent::class.java) { viewEvents.add(it) }
        try {
            // The original id-less JSON is no longer the registration key.
            NativeInAppAnalytics.onShown(modalNoId)
            assertEquals(0, viewEvents.size)
        } finally {
            subscription.unsubscribe()
        }
    }

    // MARK: - ensureInAppId (id = inAppId ?? resource.code)

    @Test
    fun fallsBackToResourceCodeWhenNoInAppId() {
        val patched = ensureInAppId(modalNoId, "r-46121-729A9")
        assertEquals("r-46121-729A9", parsedId(patched))
    }

    @Test
    fun keepsExplicitInAppId() {
        val json =
            """{"displayType":"modal","inAppId":"promo","modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}"""
        val patched = ensureInAppId(json, "r-46121-729A9")
        assertEquals("promo", parsedId(patched))
    }

    @Test
    fun overwritesEmptyStringInAppId() {
        val json =
            """{"displayType":"modal","inAppId":"","modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}"""
        val patched = ensureInAppId(json, "58B65-0DB37")
        assertEquals("58B65-0DB37", parsedId(patched))
    }

    @Test
    fun overwritesNumericInAppId() {
        val json =
            """{"displayType":"modal","inAppId":123,"modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}"""
        val patched = ensureInAppId(json, "58B65-0DB37")
        assertEquals("58B65-0DB37", parsedId(patched))
    }

    @Test
    fun leavesIdNullWhenCodeEmpty() {
        assertNull(parsedId(ensureInAppId(modalNoId, "")))
    }

    @Test
    fun leavesIdNullWhenCodeNull() {
        assertNull(parsedId(ensureInAppId(modalNoId, null)))
    }

    @Test
    fun returnsOriginalAndNeverThrowsOnGarbageJson() {
        assertEquals("not json", ensureInAppId("not json", "58B65-0DB37"))
        assertNull(InAppConfigParser.parse(ensureInAppId("not json", "58B65-0DB37")))
        assertFalse(presenter.present("not json", Resource("58B65-0DB37", false)))
    }

    @Test
    fun roundTripIdSurvivesOverlayReparse() {
        // Overlay re-parses message.rawJson and reads .id; prove it equals the queue's id
        // (no queue/overlay drift). parse() sets rawJson == its input, so reparse must agree.
        val patched = ensureInAppId(modalNoId, "r-46121-729A9")
        val message = InAppConfigParser.parse(patched)!!
        val overlayReparseId = InAppConfigParser.parse(message.rawJson)!!.id
        assertEquals("r-46121-729A9", message.id)
        assertEquals(message.id, overlayReparseId)
    }
}
