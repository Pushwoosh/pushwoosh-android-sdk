package com.pushwoosh.inapp.ui

import com.pushwoosh.PushwooshPlatform
import com.pushwoosh.inapp.PushwooshInAppImpl
import com.pushwoosh.inapp.event.RichMediaCloseEvent
import com.pushwoosh.inapp.event.RichMediaPresentEvent
import com.pushwoosh.inapp.network.model.Resource
import com.pushwoosh.inapp.view.InAppViewEvent
import com.pushwoosh.internal.event.EventBus
import com.pushwoosh.internal.event.Subscription
import com.pushwoosh.internal.preference.PreferenceStringValue
import com.pushwoosh.repository.NotificationPrefs
import com.pushwoosh.repository.RepositoryModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class NativeInAppAnalyticsTest {

    private val resource = Resource("code-a", false)
    private val rawJson = """{"displayType":"banner"}"""

    private val viewEvents = mutableListOf<InAppViewEvent>()
    private val presentEvents = mutableListOf<RichMediaPresentEvent>()
    private val closeEvents = mutableListOf<RichMediaCloseEvent>()
    private val subscriptions = mutableListOf<Subscription<*>>()

    private fun subscribeAll() {
        subscriptions += EventBus.subscribe(InAppViewEvent::class.java) { viewEvents.add(it) }
        subscriptions += EventBus.subscribe(RichMediaPresentEvent::class.java) { presentEvents.add(it) }
        subscriptions += EventBus.subscribe(RichMediaCloseEvent::class.java) { closeEvents.add(it) }
    }

    /** Static-mocks PushwooshPlatform for the duration of [block], exposing the send target. */
    private fun withPlatform(block: (PushwooshInAppImpl) -> Unit) {
        val inApp = Mockito.mock(PushwooshInAppImpl::class.java)
        val platform = Mockito.mock(PushwooshPlatform::class.java)
        Mockito.`when`(platform.pushwooshInApp()).thenReturn(inApp)
        Mockito.mockStatic(PushwooshPlatform::class.java).use { statics ->
            statics.`when`<PushwooshPlatform>(PushwooshPlatform::getInstance).thenReturn(platform)
            block(inApp)
        }
    }

    @After
    fun tearDown() {
        subscriptions.forEach { it.unsubscribe() }
        NativeInAppAnalytics.reset()
    }

    // The legacy RichMediaPresentEvent must not fire from the native path: the legacy
    // RichMediaPresentingDelegate stays silent for native in-apps (iOS parity, MVP p.13).
    @Test
    fun onShownSendsViewEventWithoutLegacyPresentEvent() {
        NativeInAppAnalytics.register(rawJson, resource)
        subscribeAll()

        NativeInAppAnalytics.onShown(rawJson)

        assertEquals(1, viewEvents.size)
        assertSame(resource, viewEvents[0].resource)
        assertTrue(presentEvents.isEmpty())
        assertTrue(closeEvents.isEmpty())
    }

    @Test
    fun onShownForUnknownKeySendsNothing() {
        subscribeAll()

        NativeInAppAnalytics.onShown("""{"displayType":"modal"}""")
        NativeInAppAnalytics.onShown(null)

        assertTrue(viewEvents.isEmpty())
        assertTrue(presentEvents.isEmpty())
    }

    // Verifies that the pending map is bounded at MAX_PENDING (8): the ninth registration
    // evicts exactly the eldest never-shown entry while the next-eldest stays routable.
    @Test
    fun registrationBeyondCapacityEvictsEldestEntryOnly() {
        val resources = (0..8).map { Resource("code-$it", false) }
        val jsons = (0..8).map { """{"displayType":"banner","n":$it}""" }
        jsons.forEachIndexed { i, json -> NativeInAppAnalytics.register(json, resources[i]) }
        subscribeAll()

        NativeInAppAnalytics.onShown(jsons[0])
        NativeInAppAnalytics.onShown(jsons[1])

        assertEquals(1, viewEvents.size)
        assertSame(resources[1], viewEvents[0].resource)
    }

    // Same guarantee for the close side: the legacy RichMediaCloseEvent is not sent.
    @Test
    fun onClosedSendsNoLegacyCloseEvent() {
        NativeInAppAnalytics.register(rawJson, resource)
        subscribeAll()

        NativeInAppAnalytics.onShown(rawJson)
        NativeInAppAnalytics.onClosed(rawJson)

        assertTrue(closeEvents.isEmpty())
    }

    @Test
    fun onClickedSendsClickForRichMediaResource() {
        NativeInAppAnalytics.register(rawJson, Resource("r-AB123-4567", false))

        withPlatform { inApp ->
            NativeInAppAnalytics.onClicked(rawJson)
            Mockito.verify(inApp).sendRichMediaAction("AB123-4567", "", null, null, 1, null)
        }
    }

    @Test
    fun onClickedSendsClickForInAppResource() {
        NativeInAppAnalytics.register(rawJson, resource)

        withPlatform { inApp ->
            NativeInAppAnalytics.onClicked(rawJson)
            Mockito.verify(inApp).sendRichMediaAction("", "code-a", null, null, 1, null)
        }
    }

    @Test
    fun onClickedForUnknownKeySendsNothing() {
        subscribeAll()

        withPlatform { inApp ->
            NativeInAppAnalytics.onClicked("""{"displayType":"modal"}""")
            NativeInAppAnalytics.onClicked(null)
            Mockito.verifyNoInteractions(inApp)
        }

        assertTrue(viewEvents.isEmpty())
        assertTrue(presentEvents.isEmpty())
        assertTrue(closeEvents.isEmpty())
    }

    // peek, not remove: the dismiss that follows a click must still find the entry.
    @Test
    fun onClickedDoesNotConsumePending() {
        NativeInAppAnalytics.register(rawJson, resource)

        withPlatform { inApp ->
            NativeInAppAnalytics.onClicked(rawJson)
            NativeInAppAnalytics.onClicked(rawJson)
            Mockito.verify(inApp, Mockito.times(2)).sendRichMediaAction("", "code-a", null, null, 1, null)
        }
    }

    // The hash is captured at register(); the core show-subscriber nulling the global value
    // afterwards (simulated here by closing the RepositoryModule mock) must not lose it.
    @Test
    fun messageHashCapturedAtRegisterOutlivesGlobalReset() {
        Mockito.mockStatic(RepositoryModule::class.java).use { repo ->
            val prefs = Mockito.mock(NotificationPrefs::class.java)
            val hashValue = Mockito.mock(PreferenceStringValue::class.java)
            Mockito.`when`(hashValue.get()).thenReturn("hash-1")
            Mockito.`when`(prefs.messageHash()).thenReturn(hashValue)
            repo.`when`<NotificationPrefs>(RepositoryModule::getNotificationPreferences).thenReturn(prefs)
            NativeInAppAnalytics.register(rawJson, resource)
        }

        withPlatform { inApp ->
            NativeInAppAnalytics.onClicked(rawJson)
            Mockito.verify(inApp).sendRichMediaAction("", "code-a", "hash-1", null, 1, null)
        }
    }

    // A failed type=4 send (uninitialized platform) is swallowed by the try/catch:
    // onClosed must not throw on the UI thread.
    @Test
    fun onClosedWithUninitializedPlatformDoesNotThrow() {
        NativeInAppAnalytics.register(rawJson, resource)

        Mockito.mockStatic(PushwooshPlatform::class.java).use { statics ->
            statics.`when`<PushwooshPlatform>(PushwooshPlatform::getInstance).thenReturn(null)
            NativeInAppAnalytics.onClosed(rawJson)
        }
    }

    // Verifies that a duplicate close for the same key (e.g. eviction racing an animated
    // dismiss) sends only one type=4: onClosed removes the entry on first use.
    @Test
    fun duplicateOnClosedSendsSingleCloseAction() {
        NativeInAppAnalytics.register(rawJson, resource)

        withPlatform { inApp ->
            NativeInAppAnalytics.onClosed(rawJson)
            NativeInAppAnalytics.onClosed(rawJson)
            Mockito.verify(inApp, Mockito.times(1)).sendRichMediaAction("", "code-a", null, null, 4, null)
        }
    }

    @Test
    fun onClosedSendsCloseActionWithoutLegacyEvent() {
        NativeInAppAnalytics.register(rawJson, Resource("r-AB123-4567", false))
        subscribeAll()

        withPlatform { inApp ->
            NativeInAppAnalytics.onClosed(rawJson)
            Mockito.verify(inApp).sendRichMediaAction("AB123-4567", "", null, null, 4, null)
        }

        assertTrue(closeEvents.isEmpty())
    }

    @Test
    fun clickThenCloseSendsClickThenCloseWithSameHash() {
        Mockito.mockStatic(RepositoryModule::class.java).use { repo ->
            val prefs = Mockito.mock(NotificationPrefs::class.java)
            val hashValue = Mockito.mock(PreferenceStringValue::class.java)
            Mockito.`when`(hashValue.get()).thenReturn("hash-1")
            Mockito.`when`(prefs.messageHash()).thenReturn(hashValue)
            repo.`when`<NotificationPrefs>(RepositoryModule::getNotificationPreferences).thenReturn(prefs)
            NativeInAppAnalytics.register(rawJson, resource)
        }
        subscribeAll()

        withPlatform { inApp ->
            NativeInAppAnalytics.onClicked(rawJson)
            NativeInAppAnalytics.onClosed(rawJson)
            val order = Mockito.inOrder(inApp)
            order.verify(inApp).sendRichMediaAction("", "code-a", "hash-1", null, 1, null)
            order.verify(inApp).sendRichMediaAction("", "code-a", "hash-1", null, 4, null)
        }

        assertTrue(closeEvents.isEmpty())
    }
}
