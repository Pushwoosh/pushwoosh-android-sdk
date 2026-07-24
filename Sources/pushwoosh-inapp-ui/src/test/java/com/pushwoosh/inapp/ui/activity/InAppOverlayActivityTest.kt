package com.pushwoosh.inapp.ui.activity

import android.content.Context
import android.os.Looper
import android.view.ViewGroup
import com.pushwoosh.PushwooshPlatform
import com.pushwoosh.inapp.PushwooshInAppImpl
import com.pushwoosh.inapp.event.RichMediaCloseEvent
import com.pushwoosh.inapp.network.model.Resource
import com.pushwoosh.inapp.ui.InAppMessageDelegate
import com.pushwoosh.inapp.ui.InAppModule
import com.pushwoosh.inapp.ui.NativeInAppAnalytics
import com.pushwoosh.inapp.ui.PushwooshInAppUi
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.view.InAppTemplateView
import com.pushwoosh.internal.event.EventBus
import com.pushwoosh.internal.utils.BackgroundExecutor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InAppOverlayActivityTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private lateinit var backgroundExecutor: MockedStatic<BackgroundExecutor>

    @Before
    fun setup() {
        resetStatic(InAppModule, "queueManagerInstance")
        resetStatic(InAppModule, "frequencyStoreInstance")
        InAppOverlayActivity.current = null
        InAppModule.delegate = null

        // Run every main-thread post inline so dismiss() resolves deterministically.
        backgroundExecutor = Mockito.mockStatic(BackgroundExecutor::class.java)
        backgroundExecutor.`when`<Unit> { BackgroundExecutor.main(any()) }.thenAnswer {
            (it.getArgument(0) as Runnable).run()
            null
        }
    }

    @After
    fun tearDown() {
        backgroundExecutor.close()
        InAppOverlayActivity.current = null
        InAppModule.delegate = null
    }

    private fun buildModal(id: String): ActivityController<InAppOverlayActivity> {
        val json = """{"displayType":"modal","inAppId":"$id","modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[],"title":{"text":"Hi","color":"#000000FF"}}}"""
        val intent = InAppOverlayActivity.intent(context, json)
        return Robolectric.buildActivity(InAppOverlayActivity::class.java, intent).setup()
    }

    private fun resetStatic(owner: Any, field: String) {
        owner.javaClass.getDeclaredField(field).apply {
            isAccessible = true
            set(owner, null)
        }
    }

    /// A shown Activity reports isPresenting=true (current is set on build) and clears it on a
    /// real destroy.
    @Test
    fun aliveActivityReportsPresentingThenClearsOnDestroy() {
        val controller = buildModal("m1")

        assertNotNull(InAppOverlayActivity.current)
        assertTrue(PushwooshInAppUi.isPresenting)

        controller.pause().stop().destroy()

        assertNull(InAppOverlayActivity.current)
        assertFalse(PushwooshInAppUi.isPresenting)
    }

    /// dismiss() runs the Activity's close path, finishing it.
    @Test
    fun dismissFinishesShownActivity() {
        val controller = buildModal("m1")
        val activity = controller.get()
        assertFalse(activity.isFinishing)

        PushwooshInAppUi.dismiss()
        // Complete the exit animation so close()'s animateOut end-callback runs finish().
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(activity.isFinishing)
    }

    private fun recordingDelegate(events: MutableList<String>) = object : InAppMessageDelegate {
        override fun willPresent(messageId: String?) {
            events.add("will:$messageId")
        }

        override fun didPresent(messageId: String?) {
            events.add("did:$messageId")
        }

        override fun didClose(messageId: String?) {
            events.add("close:$messageId")
        }
    }

    /// A shown modal fires willPresent then didPresent exactly once, in order.
    @Test
    fun showFiresWillThenDidPresentOnce() {
        val events = mutableListOf<String>()
        val delegate = recordingDelegate(events)
        InAppModule.delegate = delegate
        buildModal("m1")
        assertEquals(listOf("will:m1", "did:m1"), events)
    }

    /// A parse failure produces no delegate events — the launch is silent.
    @Test
    fun parseFailureEmitsNoDelegateEvents() {
        val events = mutableListOf<String>()
        val delegate = recordingDelegate(events)
        InAppModule.delegate = delegate
        val intent = InAppOverlayActivity.intent(context, "not json")
        Robolectric.buildActivity(InAppOverlayActivity::class.java, intent).setup().pause().stop().destroy()
        assertEquals(emptyList<String>(), events)
    }

    /// A view-build failure (a banner handed to the Activity path) is equally silent.
    @Test
    fun viewFailureEmitsNoDelegateEvents() {
        val events = mutableListOf<String>()
        val delegate = recordingDelegate(events)
        InAppModule.delegate = delegate
        val json = """{"displayType":"banner","banner":{"showClose":true,"position":"top","background":"#4B5057FF","action":{"type":"close"}}}"""
        val intent = InAppOverlayActivity.intent(context, json)
        Robolectric.buildActivity(InAppOverlayActivity::class.java, intent).setup().pause().stop().destroy()
        assertEquals(emptyList<String>(), events)
    }

    /// A config-change recreate does not replay the willPresent/didPresent pair.
    @Test
    fun recreateDoesNotReplayPresentPair() {
        val events = mutableListOf<String>()
        val delegate = recordingDelegate(events)
        InAppModule.delegate = delegate
        val controller = buildModal("m1")
        assertEquals(listOf("will:m1", "did:m1"), events)

        controller.configurationChange()
        assertEquals(listOf("will:m1", "did:m1"), events)
    }

    /// A shown modal (view built) sends close analytics (type=4) on a real destroy — the positive
    /// branch of the templateView gate whose failure branch is covered by
    /// viewFailureSendsNoCloseAnalytics. The legacy RichMediaCloseEvent must not go out: the legacy
    /// RichMediaPresentingDelegate stays silent for native in-apps (iOS parity, MVP p.13).
    @Test
    fun shownActivitySendsCloseAnalyticsOnDestroy() {
        val json = """{"displayType":"modal","inAppId":"m1","modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[],"title":{"text":"Hi","color":"#000000FF"}}}"""
        NativeInAppAnalytics.register(json, Resource("code", false))
        val closeEvents = mutableListOf<RichMediaCloseEvent>()
        val subscription = EventBus.subscribe(RichMediaCloseEvent::class.java) { closeEvents.add(it) }
        try {
            val intent = InAppOverlayActivity.intent(context, json)
            val inApp = Mockito.mock(PushwooshInAppImpl::class.java)
            val platform = Mockito.mock(PushwooshPlatform::class.java)
            Mockito.`when`(platform.pushwooshInApp()).thenReturn(inApp)
            Mockito.mockStatic(PushwooshPlatform::class.java).use { statics ->
                statics.`when`<PushwooshPlatform>(PushwooshPlatform::getInstance).thenReturn(platform)
                Robolectric.buildActivity(InAppOverlayActivity::class.java, intent).setup().pause().stop().destroy()
                Mockito.verify(inApp).sendRichMediaAction("", "code", null, null, 4, null)
            }
            assertEquals(0, closeEvents.size)
        } finally {
            subscription.unsubscribe()
            NativeInAppAnalytics.reset()
        }
    }

    /// A view-build failure sends no close analytics: the view never built, so no unpaired
    /// type=4 close action.
    @Test
    fun viewFailureSendsNoCloseAnalytics() {
        val json = """{"displayType":"banner","banner":{"showClose":true,"position":"top","background":"#4B5057FF","action":{"type":"close"}}}"""
        NativeInAppAnalytics.register(json, Resource("code", false))
        try {
            val intent = InAppOverlayActivity.intent(context, json)
            val inApp = Mockito.mock(PushwooshInAppImpl::class.java)
            val platform = Mockito.mock(PushwooshPlatform::class.java)
            Mockito.`when`(platform.pushwooshInApp()).thenReturn(inApp)
            Mockito.mockStatic(PushwooshPlatform::class.java).use { statics ->
                statics.`when`<PushwooshPlatform>(PushwooshPlatform::getInstance).thenReturn(platform)
                Robolectric.buildActivity(InAppOverlayActivity::class.java, intent).setup().pause().stop().destroy()
                Mockito.verifyNoInteractions(inApp)
            }
        } finally {
            NativeInAppAnalytics.reset()
        }
    }

    /// A close (✕) tap in the Activity path is not a click: it runs the dismiss path, which
    /// sends exactly one type=4 and never a type=1.
    @Test
    fun closeTapSendsNoClickAnalytics() {
        val json = """{"displayType":"modal","inAppId":"m1","modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[],"title":{"text":"Hi","color":"#000000FF"}}}"""
        NativeInAppAnalytics.register(json, Resource("r-AB123-4567", false))
        try {
            val intent = InAppOverlayActivity.intent(context, json)
            val controller = Robolectric.buildActivity(InAppOverlayActivity::class.java, intent).setup()
            val activity = controller.get()
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val view = content.getChildAt(0) as InAppTemplateView

            val inApp = Mockito.mock(PushwooshInAppImpl::class.java)
            val platform = Mockito.mock(PushwooshPlatform::class.java)
            Mockito.`when`(platform.pushwooshInApp()).thenReturn(inApp)
            Mockito.mockStatic(PushwooshPlatform::class.java).use { statics ->
                statics.`when`<PushwooshPlatform>(PushwooshPlatform::getInstance).thenReturn(platform)
                view.listener!!.onClose()
                // Complete the exit animation so close()'s animateOut end-callback runs finish().
                shadowOf(Looper.getMainLooper()).idle()
                assertTrue(activity.isFinishing)
                controller.pause().stop().destroy()

                Mockito.verify(inApp).sendRichMediaAction("AB123-4567", "", null, null, 4, null)
                Mockito.verifyNoMoreInteractions(inApp)
            }
        } finally {
            NativeInAppAnalytics.reset()
        }
    }

    /// A tap on a Url button in the Activity path reports a server click (type=1).
    @Test
    fun urlActionSendsClickAnalytics() {
        val json = """{"displayType":"modal","inAppId":"m1","modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[],"title":{"text":"Hi","color":"#000000FF"}}}"""
        NativeInAppAnalytics.register(json, Resource("r-AB123-4567", false))
        try {
            val intent = InAppOverlayActivity.intent(context, json)
            val activity = Robolectric.buildActivity(InAppOverlayActivity::class.java, intent).setup().get()
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val view = content.getChildAt(0) as InAppTemplateView

            val inApp = Mockito.mock(PushwooshInAppImpl::class.java)
            val platform = Mockito.mock(PushwooshPlatform::class.java)
            Mockito.`when`(platform.pushwooshInApp()).thenReturn(inApp)
            Mockito.mockStatic(PushwooshPlatform::class.java).use { statics ->
                statics.`when`<PushwooshPlatform>(PushwooshPlatform::getInstance).thenReturn(platform)
                view.listener!!.onAction(InAppAction.Url("https://example.com"))
                Mockito.verify(inApp).sendRichMediaAction("AB123-4567", "", null, null, 1, null)
            }
        } finally {
            NativeInAppAnalytics.reset()
        }
    }
}
