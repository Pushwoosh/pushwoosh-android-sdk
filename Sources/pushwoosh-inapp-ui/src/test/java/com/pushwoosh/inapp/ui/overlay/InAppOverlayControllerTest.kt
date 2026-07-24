package com.pushwoosh.inapp.ui.overlay

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import com.pushwoosh.PushwooshPlatform
import com.pushwoosh.inapp.PushwooshInAppImpl
import com.pushwoosh.inapp.event.RichMediaCloseEvent
import com.pushwoosh.inapp.network.model.Resource
import com.pushwoosh.inapp.ui.InAppMessageDelegate
import com.pushwoosh.inapp.ui.InAppModule
import com.pushwoosh.inapp.ui.NativeInAppAnalytics
import com.pushwoosh.inapp.ui.PushwooshInAppUi
import com.pushwoosh.inapp.ui.model.BannerContent
import com.pushwoosh.inapp.ui.model.BannerPosition
import com.pushwoosh.inapp.ui.model.CarouselContent
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.inapp.ui.model.InAppText
import com.pushwoosh.inapp.ui.view.InAppTemplateView
import com.pushwoosh.internal.event.EventBus
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.utils.BackgroundExecutor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InAppOverlayControllerTest {

    private lateinit var activity: Activity
    private lateinit var mockPlatform: PushwooshPlatform
    private lateinit var platform: MockedStatic<PushwooshPlatform>
    private val closedIds = mutableListOf<String?>()

    // InAppModule holds the delegate weakly; keep a strong reference for the test's lifetime.
    private var delegate: InAppMessageDelegate? = null

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mockPlatform = Mockito.mock(PushwooshPlatform::class.java)
        Mockito.`when`(mockPlatform.topActivity).thenReturn(activity)
        platform = Mockito.mockStatic(PushwooshPlatform::class.java)
        platform.`when`<PushwooshPlatform>(PushwooshPlatform::getInstance).thenReturn(mockPlatform)
        delegate = object : InAppMessageDelegate {
            override fun didClose(messageId: String?) {
                closedIds.add(messageId)
            }
        }
        InAppModule.delegate = delegate
    }

    @After
    fun tearDown() {
        // The controller is a singleton: detach this test's overlays so the attach-state
        // listener clears activeView/activeMessage and state does not bleed across tests.
        val root = activity.window.decorView as ViewGroup
        for (i in root.childCount - 1 downTo 0) {
            val child = root.getChildAt(i)
            if (child is InAppTemplateView) {
                root.removeView(child)
            }
        }
        InAppModule.delegate = null
        delegate = null
        platform.close()
        NativeInAppAnalytics.reset()
    }

    private fun banner(id: String, rawJson: String = """{"displayType":"banner","id":"$id"}""") = InAppMessage(
        id,
        InAppLayout.Banner(
            BannerContent(BannerPosition.BOTTOM, null, InAppText(id, Color.WHITE), null, Color.BLACK, InAppAction.Close, 0L, true)
        ),
        null,
        null,
        null,
        rawJson
    )

    /// A host-Activity death (view detached without a dismiss) reports the dismissal to the queue:
    /// the queue — the single source — fires didClose exactly once (the show was confirmed) and
    /// analytics sends the type=4 close action. The legacy RichMediaCloseEvent must not go out:
    /// the legacy RichMediaPresentingDelegate stays silent for native in-apps (iOS parity, MVP
    /// p.13). The slot is released, so the same id shows again.
    @Test
    fun detachThroughQueueReportsSingleDidClose() {
        resetStatic(InAppModule, "queueManagerInstance")
        resetStatic(InAppModule, "frequencyStoreInstance")
        val appContext = RuntimeEnvironment.getApplication()

        Mockito.mockStatic(AndroidPlatformModule::class.java).use { platformModule ->
            platformModule.`when`<Context> { AndroidPlatformModule.getApplicationContext() }.thenReturn(appContext)
            platformModule.`when`<Boolean> { AndroidPlatformModule.isApplicationInForeground() }.thenReturn(true)

            val inApp = Mockito.mock(PushwooshInAppImpl::class.java)
            Mockito.`when`(mockPlatform.pushwooshInApp()).thenReturn(inApp)
            val message = banner("detached")
            NativeInAppAnalytics.register(message.rawJson, Resource("detached-code", false))
            InAppModule.queueManager(appContext).enqueueForeground(message)
            assertTrue(InAppOverlayController.isActive)

            val closeEvents = mutableListOf<RichMediaCloseEvent>()
            val subscription = EventBus.subscribe(RichMediaCloseEvent::class.java) { closeEvents.add(it) }
            try {
                val root = activity.window.decorView as ViewGroup
                val overlay = root.getChildAt(root.childCount - 1)
                root.removeView(overlay)

                assertEquals(listOf<String?>("detached"), closedIds)
                Mockito.verify(inApp).sendRichMediaAction("", "detached-code", null, null, 4, null)
                assertTrue(closeEvents.isEmpty())
                assertFalse(InAppOverlayController.isActive)

                InAppModule.queueManager(appContext).enqueueForeground(banner("detached"))
                assertTrue(InAppOverlayController.isActive)
            } finally {
                subscription.unsubscribe()
            }
        }
    }

    /// A dismiss whose exit animation is interrupted by the host Activity dying mid-animation must
    /// still release the queue slot. Robolectric does not drive animateOut to completion, so its
    /// onEnd never fires — exactly the real-world case where a detached view's ViewPropertyAnimator
    /// abandons the end callback. The view's detach is the backstop: it reports the dismissal to
    /// the queue, so didClose fires once and the freed slot accepts the next message. Without the
    /// backstop the slot would stay claimed forever and block every later in-app.
    @Test
    fun dismissInterruptedByDetachStillReleasesQueueSlot() {
        resetStatic(InAppModule, "queueManagerInstance")
        resetStatic(InAppModule, "frequencyStoreInstance")
        val appContext = RuntimeEnvironment.getApplication()

        Mockito.mockStatic(AndroidPlatformModule::class.java).use { platformModule ->
            platformModule.`when`<Context> { AndroidPlatformModule.getApplicationContext() }.thenReturn(appContext)
            platformModule.`when`<Boolean> { AndroidPlatformModule.isApplicationInForeground() }.thenReturn(true)

            val first = banner("first")
            NativeInAppAnalytics.register(first.rawJson, Resource("first-code", false))
            InAppModule.queueManager(appContext).enqueueForeground(first)
            assertTrue(InAppOverlayController.isActive)

            // User closes it: the exit animation starts (activeView nulled synchronously) but its
            // onEnd never fires under Robolectric, so the slot release is still pending.
            InAppOverlayController.dismissActive()
            assertFalse(InAppOverlayController.isActive)
            assertEquals(emptyList<String?>(), closedIds)

            // Host Activity dies mid-animation: the view detaches. This must release the slot.
            val root = activity.window.decorView as ViewGroup
            val overlay = root.getChildAt(root.childCount - 1)
            root.removeView(overlay)

            assertEquals(listOf<String?>("first"), closedIds)

            // The freed slot accepts the next message.
            InAppModule.queueManager(appContext).enqueueForeground(banner("second"))
            assertTrue(InAppOverlayController.isActive)
        }
    }

    /// S2 regression: a confirmed overlay's dismissal must reach the queue even when the
    /// application-context global has gone null (a dying process). The dismissal callback is bound
    /// at present time to the queue the channel already holds a strong reference to, so a detach
    /// releases the slot — fires didClose and advances — without re-resolving getApplicationContext()
    /// from inside the controller. Before the fix the controller re-resolved that nullable global on
    /// dismiss and silently returned, stranding the slot and blocking every later in-app.
    @Test
    fun detachReleasesSlotWhenAppContextGoesNull() {
        resetStatic(InAppModule, "queueManagerInstance")
        resetStatic(InAppModule, "frequencyStoreInstance")
        val appContext = RuntimeEnvironment.getApplication()

        Mockito.mockStatic(AndroidPlatformModule::class.java).use { platformModule ->
            platformModule.`when`<Context> { AndroidPlatformModule.getApplicationContext() }.thenReturn(appContext)
            platformModule.`when`<Boolean> { AndroidPlatformModule.isApplicationInForeground() }.thenReturn(true)

            InAppModule.queueManager(appContext).enqueueForeground(banner("first"))
            assertTrue(InAppOverlayController.isActive)

            // Process is dying: the application-context global is now null. The old dismiss path
            // re-resolved it here and bailed, wedging the slot.
            platformModule.`when`<Context> { AndroidPlatformModule.getApplicationContext() }.thenReturn(null)

            val root = activity.window.decorView as ViewGroup
            root.removeView(root.getChildAt(root.childCount - 1))

            assertEquals(listOf<String?>("first"), closedIds)
            assertFalse(InAppOverlayController.isActive)

            // The slot was actually freed (not merely nulled by detach): the next message shows.
            InAppModule.queueManager(appContext).enqueueForeground(banner("second"))
            assertTrue(InAppOverlayController.isActive)
        }
    }

    private fun resetStatic(owner: Any, field: String) {
        owner.javaClass.getDeclaredField(field).apply {
            isAccessible = true
            set(owner, null)
        }
    }

    /// dismissActive() routes the on-screen overlay into the normal animated dismiss: activeView
    /// is nulled synchronously, so isActive / isPresenting flip false at once. The paired didClose
    /// fires from the pre-existing dismiss(view, message) exit-animation callback, which
    /// Robolectric's overlay-attach harness does not drive to completion (proven for both banner
    /// and modal), so it is not asserted here — only the synchronous state flip is.
    @Test
    fun dismissActiveClosesShownOverlay() {
        assertTrue(InAppOverlayController.show(banner("live"), {}))
        assertTrue(InAppOverlayController.isActive)
        assertTrue(PushwooshInAppUi.isPresenting)

        InAppOverlayController.dismissActive()

        assertFalse(InAppOverlayController.isActive)
        assertFalse(PushwooshInAppUi.isPresenting)
    }

    /// dismiss() through the public API closes a shown overlay (covers the overlay branch of
    /// PushwooshInAppUi.dismiss(), which routes to InAppOverlayController.dismissActive()).
    @Test
    fun publicDismissClosesShownOverlay() {
        assertTrue(InAppOverlayController.show(banner("live"), {}))
        assertTrue(InAppOverlayController.isActive)

        Mockito.mockStatic(BackgroundExecutor::class.java).use { bg ->
            bg.`when`<Unit> { BackgroundExecutor.main(any()) }.thenAnswer {
                (it.getArgument(0) as Runnable).run()
                null
            }
            PushwooshInAppUi.dismiss()
        }

        assertFalse(InAppOverlayController.isActive)
    }

    /// dismissActive() with nothing on screen is a no-op: no crash, no didClose.
    @Test
    fun dismissActiveNoOpWhenNothingShown() {
        InAppOverlayController.dismissActive()

        assertFalse(InAppOverlayController.isActive)
        assertEquals(emptyList<String?>(), closedIds)
    }

    /// A present-side-effect throw (here the integrator's willPresent) must not leave an orphan
    /// view attached to the decorView. show() detaches the view and clears activeView before the
    /// exception propagates, so the channel releasing the slot lands on a clean state and a later
    /// advance() cannot stack the next overlay over an unreachable orphan.
    @Test
    fun showDetachesViewWhenPresentCallbackThrows() {
        val throwing = object : InAppMessageDelegate {
            override fun willPresent(messageId: String?) = throw RuntimeException("integrator blew up")
        }
        InAppModule.delegate = throwing

        try {
            InAppOverlayController.show(banner("boom"), {})
            fail("show() must propagate the callback throw")
        } catch (e: RuntimeException) {
            // expected — the channel converts this into a silent slot release
        }

        assertFalse(InAppOverlayController.isActive)
        val root = activity.window.decorView as ViewGroup
        var overlays = 0
        for (i in 0 until root.childCount) {
            if (root.getChildAt(i) is InAppTemplateView) overlays++
        }
        assertEquals(0, overlays)
    }

    /// S3 regression: the same orphan-detach guarantee must hold when the present side effect throws
    /// an Error (a TODO()/assert), not an Exception. The catch is widened to Throwable, so an Error
    /// still detaches the view and clears activeView before propagating — without it the widened
    /// channel catch would release the slot but leave an unreachable orphan on the decorView.
    @Test
    fun showDetachesViewWhenPresentCallbackThrowsError() {
        val throwing = object : InAppMessageDelegate {
            override fun willPresent(messageId: String?): Unit = throw NotImplementedError("integrator not done")
        }
        InAppModule.delegate = throwing

        try {
            InAppOverlayController.show(banner("boom"), {})
            fail("show() must propagate the callback Error")
        } catch (e: NotImplementedError) {
            // expected — the channel converts this into a silent slot release
        }

        assertFalse(InAppOverlayController.isActive)
        val root = activity.window.decorView as ViewGroup
        var overlays = 0
        for (i in 0 until root.childCount) {
            if (root.getChildAt(i) is InAppTemplateView) overlays++
        }
        assertEquals(0, overlays)
    }

    /// N1 guard: show() and InAppRoutingChannel.isNonBlocking are in manual lockstep. A future
    /// non-blocking layout added to routing but not to show()'s when() must fail loud, not return a
    /// bare `false`: post-M2 the channel reads `false` as "no host Activity yet" and re-defers the
    /// same message onto the head of the queue forever. show() throws for such a layout instead, so
    /// the channel's catch logs and drops it. No view is attached (the throw precedes addView).
    @Test
    fun showThrowsForNonOverlayLayout() {
        val carousel = InAppMessage(
            "carousel",
            InAppLayout.Carousel(CarouselContent(emptyList(), false)),
            null,
            null,
            null,
            "{}"
        )

        try {
            InAppOverlayController.show(carousel, {})
            fail("show() must throw for a non-overlay layout, not return false")
        } catch (e: IllegalStateException) {
            // expected — the channel converts this into a logged drop, never a retryable false
        }

        assertFalse(InAppOverlayController.isActive)
        val root = activity.window.decorView as ViewGroup
        var overlays = 0
        for (i in 0 until root.childCount) {
            if (root.getChildAt(i) is InAppTemplateView) overlays++
        }
        assertEquals(0, overlays)
    }

    /// The documented retryable contract on the real controller: with no host Activity
    /// (topActivity null — between screens, paused under a permission dialog, PiP) show()
    /// returns false with no side effects — nothing attached, no dismissal reported, no
    /// didClose. The channel reads this false as "defer and retry", so a regression that
    /// throws here instead (e.g. a `!!`) would turn a transient state into a prod crash.
    @Test
    fun showReturnsFalseWhenNoHostActivity() {
        Mockito.`when`(mockPlatform.topActivity).thenReturn(null)
        val dismissed = mutableListOf<String?>()

        assertFalse(InAppOverlayController.show(banner("held"), { dismissed.add(it) }))

        assertFalse(InAppOverlayController.isActive)
        assertEquals(emptyList<String?>(), dismissed)
        assertEquals(emptyList<String?>(), closedIds)
        val root = activity.window.decorView as ViewGroup
        var overlays = 0
        for (i in 0 until root.childCount) {
            if (root.getChildAt(i) is InAppTemplateView) overlays++
        }
        assertEquals(0, overlays)
    }

    /// A tap on a Url element in the overlay path reports a server click (type=1) alongside
    /// the host clickedAction callback.
    @Test
    fun urlActionSendsClickAnalytics() {
        val inApp = Mockito.mock(PushwooshInAppImpl::class.java)
        Mockito.`when`(mockPlatform.pushwooshInApp()).thenReturn(inApp)
        val message = banner("clicked")
        NativeInAppAnalytics.register(message.rawJson, Resource("r-AB123-4567", false))
        assertTrue(InAppOverlayController.show(message, {}))

        val root = activity.window.decorView as ViewGroup
        val overlay = root.getChildAt(root.childCount - 1) as InAppTemplateView
        overlay.listener!!.onAction(InAppAction.Url("https://example.com"))

        Mockito.verify(inApp).sendRichMediaAction("AB123-4567", "", null, null, 1, null)
    }

    /// A close (✕) tap in the overlay path reports no click (type=1) — only Url actions are
    /// clicks. The type=4 of this dismiss pends on the exit-animation callback, which this
    /// harness does not drive (see dismissActiveClosesShownOverlay), so no send at all is
    /// expected here. The pushwooshInApp stub is load-bearing: without it a wrongly-sent
    /// click would die as an NPE inside the analytics try/catch and the verify would pass
    /// trivially.
    @Test
    fun closeTapSendsNoClickAnalytics() {
        val inApp = Mockito.mock(PushwooshInAppImpl::class.java)
        Mockito.`when`(mockPlatform.pushwooshInApp()).thenReturn(inApp)
        val message = banner("closed")
        NativeInAppAnalytics.register(message.rawJson, Resource("r-AB123-4567", false))
        assertTrue(InAppOverlayController.show(message, {}))

        val root = activity.window.decorView as ViewGroup
        val overlay = root.getChildAt(root.childCount - 1) as InAppTemplateView
        overlay.listener!!.onClose()

        Mockito.verifyNoInteractions(inApp)
    }
}
