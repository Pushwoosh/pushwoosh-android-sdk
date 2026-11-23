package com.pushwoosh.inline.internal.html

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.util.AndroidRuntimeException
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import com.pushwoosh.Pushwoosh
import com.pushwoosh.PushwooshPlatform
import com.pushwoosh.inapp.PushwooshInAppImpl
import com.pushwoosh.inapp.view.js.PushManagerJSInterface
import com.pushwoosh.inapp.view.js.PushwooshJSInterface
import com.pushwoosh.inline.PushwooshInline
import com.pushwoosh.inline.PushwooshInlineColorScheme
import com.pushwoosh.inline.PushwooshInlineView
import com.pushwoosh.inline.internal.FakeClock
import com.pushwoosh.inline.internal.FakeEventSink
import com.pushwoosh.inline.internal.FakeIdentity
import com.pushwoosh.inline.internal.FakeScheduler
import com.pushwoosh.inline.internal.FakeTransport
import com.pushwoosh.inline.internal.InlineManager
import com.pushwoosh.inline.internal.InlineModule
import com.pushwoosh.inline.internal.htmlBanner
import com.pushwoosh.inline.internal.owner
import com.pushwoosh.inline.internal.store.InlineStore
import com.pushwoosh.inline.internal.view.InlineContentFactories
import com.pushwoosh.internal.platform.AndroidPlatformModule
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.io.File
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(sdk = [35], qualifiers = "xhdpi")
class InlineHtmlBannerViewTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var clock: FakeClock
    private lateinit var scheduler: FakeScheduler
    private lateinit var identity: FakeIdentity
    private lateinit var events: FakeEventSink
    private lateinit var transport: FakeTransport
    private lateinit var store: InlineStore
    private lateinit var activity: Activity
    private lateinit var root: LinearLayout
    private val heights = mutableListOf<Float>()
    private lateinit var platformModule: MockedStatic<AndroidPlatformModule>
    private lateinit var platform: MockedStatic<PushwooshPlatform>
    private lateinit var inApp: PushwooshInAppImpl

    @Before
    fun setUp() {
        platformModule = Mockito.mockStatic(AndroidPlatformModule::class.java)
        platformModule.`when`<Boolean> { AndroidPlatformModule.isApplicationInForeground() }.thenReturn(true)
        inApp = Mockito.mock(PushwooshInAppImpl::class.java)
        val pushwooshPlatform = Mockito.mock(PushwooshPlatform::class.java)
        Mockito.`when`(pushwooshPlatform.pushwooshInApp()).thenReturn(inApp)
        platform = Mockito.mockStatic(PushwooshPlatform::class.java)
        platform.`when`<PushwooshPlatform> { PushwooshPlatform.getInstance() }.thenReturn(pushwooshPlatform)
        clock = FakeClock(1_800_000_000_123L)
        scheduler = FakeScheduler(clock)
        identity = FakeIdentity()
        events = FakeEventSink()
        store = InlineStore(File(folder.root, "pushwoosh/inline-banners.json"), clock, scheduler, Executor { it.run() })
        transport = FakeTransport()
        InlineModule.resetForTest(InlineManager(store, transport, events, identity, clock, scheduler))
        activity = Robolectric.buildActivity(Activity::class.java).setup().windowFocusChanged(true).get()
        setAnimatorsEnabled(false)
        InlineHtmlHeights.clear()
    }

    @After
    fun tearDown() {
        PushwooshInline.colorScheme = PushwooshInlineColorScheme.APP
        setAnimatorsEnabled(true)
        InlineModule.resetForTest(null)
        InlineContentFactories.override = null
        platform.close()
        platformModule.close()
    }

    private fun setAnimatorsEnabled(enabled: Boolean) {
        Settings.Global.putFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (enabled) 1f else 0f)
        ReflectionHelpers.callStaticMethod<Unit>(
            ValueAnimator::class.java,
            "setDurationScale",
            ClassParameter.from(Float::class.javaPrimitiveType, if (enabled) 1f else 0f),
        )
    }

    private fun px(dp: Int): Int = (dp * activity.resources.displayMetrics.density).toInt()

    /** Shows the HTML banner `h1` in slot `home` between two fixed siblings. */
    private fun mountSlot(expireDate: Long? = null, close: Boolean = false): PushwooshInlineView {
        store.load(identity.owner())
        val banner = htmlBanner("h1", html = HTML).apply { if (expireDate != null) put("expireDate", expireDate) }
        if (close) banner.put("close", JSONObject().put("enabled", true))
        store.apply(listOf("home"), JSONObject().put("home", banner), identity.owner())
        val slot = PushwooshInlineView(activity, "home")
        slot.setOnHeightChangeListener { heights += it }
        slot.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(View(activity), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(40)))
            addView(slot)
        }
        activity.setContentView(root)
        layout()
        return slot
    }

    private fun layout() {
        ShadowLooper.idleMainLooper()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(px(360), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(px(2_000), View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
    }

    private fun webView(view: View): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { webView(view.getChildAt(it)) }
        else -> null
    }

    private fun web(slot: PushwooshInlineView): WebView = checkNotNull(webView(slot))

    @Test
    fun testConfiguredCloseDismissesHtmlAndRequestsNextBanner() {
        val slot = mountSlot(close = true)
        val web = web(slot)
        scheduler.advance(1_000)
        assertEquals(px(1), slot.height)
        assertTrue(sentEvents().isEmpty())
        reportHeight(web, 120.0)
        scheduler.advance(300)
        assertEquals(px(120), slot.height)
        assertEquals(listOf("show:-"), sentEvents())

        val buttons = ArrayList<View>()
        slot.findViewsWithText(buttons, "Close", View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
        assertEquals("configured close button", 1, buttons.size)
        buttons.single().performClick()
        layout()
        assertEquals(0, slot.height)
        assertEquals(listOf("show:-", "dismiss:-"), sentEvents())
        assertNull(shadowOf(web).getJavascriptInterface("pushwooshInlineHeight"))
        events.deliver(1)
        assertEquals(listOf(listOf("home")), transport.calls)
        transport.answer(0, JSONObject().put("home", htmlBanner("next")))
        layout()
        reportHeight(web(slot), 80.0)
        assertEquals(px(80), slot.height)
    }

    private fun reportHeight(web: WebView, height: Double) {
        (shadowOf(web).getJavascriptInterface("pushwooshInlineHeight") as InlineHeightJs).postMessage(height)
        layout()
    }

    private fun pushwooshImpl(web: WebView) = shadowOf(web).getJavascriptInterface("pushwooshImpl") as PushwooshJSInterface

    private fun pushManager(web: WebView) = shadowOf(web).getJavascriptInterface("pushManager") as PushManagerJSInterface

    private fun sentEvents(): List<String> = events.events.map { it.getString("type") + ":" + it.optString("elementId", "-") }

    private fun touch(slot: View, action: Int, x: Int, y: Int) {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x.toFloat(), y.toFloat(), 0)
        slot.dispatchTouchEvent(event)
        event.recycle()
    }

    // Verifies that the slot height follows the reported document height, ignores <=0 and caps at the viewport.
    @Test
    fun testHtmlBannerHeightFollowsDocumentHeight() {
        val slot = mountSlot()
        val web = web(slot)
        val loaded = shadowOf(web).lastLoadDataWithBaseURL
        assertEquals(HTML, loaded.data)
        assertNull(loaded.baseUrl)
        assertEquals("placeholder before the first report", px(1), slot.height)

        reportHeight(web, 0.0)
        reportHeight(web, -3.0)
        reportHeight(web, Double.NaN)
        reportHeight(web, Double.POSITIVE_INFINITY)
        scheduler.advance(5_000)
        assertEquals("invalid reports", px(1), slot.height)
        assertEquals("not shown before the first valid report", emptyList<String>(), sentEvents())

        reportHeight(web, 120.4)
        assertEquals(px(121), slot.height)
        assertEquals(121f, heights.last())
        scheduler.advance(300)
        assertEquals(listOf("show:-"), sentEvents())

        reportHeight(web, 0.0)
        assertEquals("a later invalid report keeps the height", px(121), slot.height)

        val metrics = activity.resources.displayMetrics
        reportHeight(web, 100_000.0)
        assertEquals("capped at the viewport", maxOf(metrics.widthPixels, metrics.heightPixels), slot.height)
    }

    // Verifies that postEvent from the page is refused in the unconfirmed window with the error callback.
    @Test
    fun testHtmlPostEventIsHeldForUnconfirmedUser() {
        val web = web(mountSlot())
        identity.userId = "u2"

        pushwooshImpl(web).postEvent("restock", "{}", "ok", "err")
        pushManager(web).postEvent("{\"event\":\"restock\",\"attributes\":{}}")
        ShadowLooper.idleMainLooper()

        verify(inApp, never()).postEvent(any(), any(), any())
        assertTrue(shadowOf(web).lastEvaluatedJavascript.contains("invokeCallback(\"err\""))

        identity.userId = "u1"
        pushwooshImpl(web).postEvent("restock", "{}", "ok", "err")
        pushManager(web).postEvent("{\"event\":\"restock\",\"attributes\":{}}")
        ShadowLooper.idleMainLooper()

        verify(inApp, times(2)).postEvent(eq("restock"), any(), any())
    }

    // Verifies that closeInApp from the page dismisses without sending richMediaAction.
    @Test
    fun testHtmlCloseInAppHidesBannerWithoutRichMediaAction() {
        val slot = mountSlot()
        val web = web(slot)
        reportHeight(web, 80.0)

        pushwooshImpl(web).closeInApp()
        layout()

        assertEquals(listOf("dismiss:-"), sentEvents())
        assertEquals(0, slot.height)
        verifyNoInteractions(inApp)
    }

    // Verifies that a WebView that cannot be created leaves the slot collapsed and reports no show.
    @Test
    fun testWebViewCreationFailureKeepsSlotCollapsed() {
        InlineContentFactories.override = InlineHtmlFactory { throw AndroidRuntimeException("no webview") }

        val slot = mountSlot()
        scheduler.advance(5_000)

        assertEquals(0, slot.height)
        assertEquals(emptyList<Float>(), heights)
        assertEquals(emptyList<String>(), sentEvents())
    }

    // Verifies that a tap is hit-tested at CSS px = touch px / density, a marked hit is a click with its id, and a drag is no tap.
    @Test
    fun testHtmlTapOnMarkedElementIsClickWithItsId() {
        val slot = mountSlot()
        val web = web(slot)
        reportHeight(web, 100.0)

        touch(slot, MotionEvent.ACTION_DOWN, px(50), px(30))
        touch(slot, MotionEvent.ACTION_MOVE, px(50), px(80))
        touch(slot, MotionEvent.ACTION_UP, px(50), px(80))
        assertNull("a drag runs no hit test", shadowOf(web).lastEvaluatedJavascript)

        touch(slot, MotionEvent.ACTION_DOWN, px(50), px(30))
        touch(slot, MotionEvent.ACTION_UP, px(50), px(30))
        assertEquals(InlineScripts.hitTest(50.0, 30.0), shadowOf(web).lastEvaluatedJavascript)
        shadowOf(web).lastEvaluatedJavascriptCallback.onReceiveValue("{\"kind\":\"marked\",\"id\":\"buy\"}")

        assertEquals(listOf("click:buy"), sentEvents())
    }

    // Navigation must wait for the hit result so leaving the screen cannot discard its click.
    @Test
    fun testHtmlLinkWaitsForUnmarkedHitBeforeOpening() {
        assertLinkClickBeforeNavigation(marked = false, hitFirst = false)
    }

    @Test
    fun testHtmlLinkFlushesUnmarkedHitBeforeOpening() {
        assertLinkClickBeforeNavigation(marked = false, hitFirst = true)
    }

    @Test
    fun testHtmlLinkPreservesMarkedHitBeforeOpening() {
        assertLinkClickBeforeNavigation(marked = true, hitFirst = false)
    }

    @Test
    fun testHtmlLinkDoesNotRepeatMarkedClickWhenHitArrivesFirst() {
        assertLinkClickBeforeNavigation(marked = true, hitFirst = true)
    }

    @Test
    fun testHigherVersionLinkReportsMarkedClickAfterDismiss() {
        assertLinkClickBeforeNavigation(marked = true, hitFirst = false, afterDismiss = true)
    }

    private fun assertLinkClickBeforeNavigation(marked: Boolean, hitFirst: Boolean, afterDismiss: Boolean = false) {
        val slot = mountSlot()
        if (afterDismiss) {
            InlineModule.managerOrNull()!!.dismiss("home", "h1")
            layout()
            assertEquals(0, slot.height)
            store.apply(listOf("home"), JSONObject().put("home", htmlBanner("h1", version = 2, html = HTML)), identity.owner())
            layout()
        }
        val web = web(slot)
        reportHeight(web, 100.0)
        scheduler.advance(1_000)
        events.events.clear()
        touch(slot, MotionEvent.ACTION_DOWN, px(50), px(30))
        touch(slot, MotionEvent.ACTION_UP, px(50), px(30))
        val hit = shadowOf(web).lastEvaluatedJavascriptCallback
        val result = if (marked) "{\"kind\":\"marked\",\"id\":\"buy\"}" else "{\"kind\":\"control\",\"id\":null}"
        if (hitFirst) hit.onReceiveValue(result)

        @Suppress("DEPRECATION")
        shadowOf(web).webViewClient.shouldOverrideUrlLoading(web, "https://example.com/sale")
        if (!hitFirst) {
            assertNull("navigation waits for the click", shadowOf(activity).nextStartedActivity)
            hit.onReceiveValue(result)
        }

        assertEquals(listOf(if (marked) "click:buy" else "click:-"), sentEvents())
        assertEquals(if (afterDismiss) 2 else 1, events.events.single().getInt("version"))
        assertEquals("https://example.com/sale", shadowOf(activity).nextStartedActivity?.dataString)
        root.removeView(slot)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertEquals("one click after leaving the screen", 1, events.events.size)
        assertNull("one navigation", shadowOf(activity).nextStartedActivity)
    }

    // Verifies that page links open outside the banner, which stays, and pushwoosh://close dismisses it.
    @Test
    fun testHtmlLinksOpenOutsideOrCloseBanner() {
        val slot = mountSlot()
        val web = web(slot)
        reportHeight(web, 100.0)
        val client = shadowOf(web).webViewClient

        @Suppress("DEPRECATION")
        val consumed = client.shouldOverrideUrlLoading(web, "https://example.com/sale")
        val started: Intent? = shadowOf(activity).nextStartedActivity
        assertTrue(consumed)
        assertEquals(Intent.ACTION_VIEW, started?.action)
        assertEquals("https://example.com/sale", started?.dataString)
        layout()
        assertEquals(px(100), slot.height)

        @Suppress("DEPRECATION")
        client.shouldOverrideUrlLoading(web, "javascript:void(0)")
        assertNull(shadowOf(activity).nextStartedActivity)

        @Suppress("DEPRECATION")
        client.shouldOverrideUrlLoading(web, "pushwoosh://close")
        layout()
        assertEquals(listOf("dismiss:-"), sentEvents())
        assertEquals(0, slot.height)
    }

    // Verifies that a page load other than the banner's own document (form post, reload elsewhere) gets no SDK API, leaves the slot only after a tap, and the banner is loaded again.
    @Test
    fun testForeignPageLoadIsRefusedAndBannerRestored() {
        val slot = mountSlot()
        val web = web(slot)
        reportHeight(web, 100.0)
        val client = shadowOf(web).webViewClient
        val ownLoad = shadowOf(web).lastLoadDataWithBaseURL

        client.onPageStarted(web, "about:blank", null)
        val apiScript = shadowOf(web).lastEvaluatedJavascript
        assertTrue(apiScript.contains("_hwid"))

        client.onPageStarted(web, "https://example.com/form", null)
        client.onPageFinished(web, "https://example.com/form")
        assertEquals("no API in a foreign page", apiScript, shadowOf(web).lastEvaluatedJavascript)
        assertNull("no tap, nothing opens", shadowOf(activity).nextStartedActivity)
        val restored = shadowOf(web).lastLoadDataWithBaseURL
        assertTrue("the banner is loaded again", restored !== ownLoad)
        assertEquals(HTML, restored.data)

        touch(slot, MotionEvent.ACTION_DOWN, px(50), px(30))
        touch(slot, MotionEvent.ACTION_UP, px(50), px(30))
        val hit = shadowOf(web).lastEvaluatedJavascriptCallback
        client.onPageStarted(web, "https://example.com/form", null)
        hit.onReceiveValue("{\"kind\":\"control\",\"id\":null}")
        assertEquals("https://example.com/form", shadowOf(activity).nextStartedActivity?.dataString)
        layout()
        assertEquals(px(100), slot.height)
    }

    // Verifies that a redrawn HTML banner reports the own-control click its page held back, before its WebView goes away.
    @Test
    fun testHeldControlClickIsReportedWhenTheBannerIsRedrawn() {
        val slot = mountSlot()
        val web = web(slot)
        reportHeight(web, 100.0)
        scheduler.advance(1_000)

        touch(slot, MotionEvent.ACTION_DOWN, px(50), px(30))
        touch(slot, MotionEvent.ACTION_UP, px(50), px(30))
        shadowOf(web).lastEvaluatedJavascriptCallback.onReceiveValue("{\"kind\":\"control\",\"id\":null}")
        PushwooshInline.colorScheme = PushwooshInlineColorScheme.DARK

        assertEquals(listOf("show:-", "click:-"), sentEvents())
    }

    // Verifies that a view drawn again for the same banner version keeps the reported height until its page reports, while its show still waits for that report; another version starts from the placeholder.
    @Test
    fun testRedrawnBannerKeepsItsHeightUntilThePageReports() {
        val slot = mountSlot()
        reportHeight(web(slot), 200.0)
        heights.clear()

        PushwooshInline.colorScheme = PushwooshInlineColorScheme.DARK
        layout()
        assertEquals(px(200), slot.height)
        assertTrue(heights.isEmpty())
        scheduler.advance(5_000)
        assertEquals(emptyList<String>(), sentEvents())

        reportHeight(web(slot), 200.0)
        scheduler.advance(300)
        assertEquals(listOf("show:-"), sentEvents())

        store.apply(listOf("home"), JSONObject().put("home", htmlBanner("h1", version = 2, html = HTML)), identity.owner())
        layout()
        assertEquals(px(1), slot.height)
    }

    // Verifies that a link tapped on an HTML banner that expired while no timer ran is not opened and the slot collapses.
    @Test
    fun testExpiredHtmlBannerOpensNoLink() {
        val slot = mountSlot(expireDate = clock.nowMillis / 1000 + 60)
        val web = web(slot)
        reportHeight(web, 100.0)
        val client = shadowOf(web).webViewClient
        clock.nowMillis += 3_600_000L

        @Suppress("DEPRECATION")
        val consumed = client.shouldOverrideUrlLoading(web, "https://example.com/sale")
        layout()

        assertTrue(consumed)
        assertNull(shadowOf(activity).nextStartedActivity)
        assertEquals(0, slot.height)
    }

    // Verifies that postEvent called by the page of an HTML banner that expired while no timer ran posts nothing and the slot collapses.
    @Test
    fun testExpiredHtmlBannerPostsNoEvent() {
        val slot = mountSlot(expireDate = clock.nowMillis / 1000 + 60)
        val web = web(slot)
        reportHeight(web, 100.0)
        clock.nowMillis += 3_600_000L

        pushwooshImpl(web).postEvent("restock", "{}", "ok", "err")
        layout()

        verify(inApp, never()).postEvent(any(), any(), any())
        assertEquals(0, slot.height)
    }

    // Verifies that registerForPushNotifications and openAppSettings called by the page of an HTML banner that expired while no timer ran do nothing and the slot collapses.
    @Test
    fun testExpiredHtmlBannerRunsNoClosingAction() {
        val pushwoosh = Mockito.mock(Pushwoosh::class.java)
        Mockito.mockStatic(Pushwoosh::class.java).use { statics ->
            statics.`when`<Pushwoosh> { Pushwoosh.getInstance() }.thenReturn(pushwoosh)
            val application = RuntimeEnvironment.getApplication()
            platformModule.`when`<Context> { AndroidPlatformModule.getApplicationContext() }.thenReturn(application)
            val slot = mountSlot(expireDate = clock.nowMillis / 1000 + 60)
            val web = web(slot)
            reportHeight(web, 100.0)
            val bridge = pushwooshImpl(web)
            clock.nowMillis += 3_600_000L

            bridge.registerForPushNotifications()
            bridge.openAppSettings()
            layout()

            verify(pushwoosh, never()).registerForPushNotifications()
            assertNull(shadowOf(application).nextStartedActivity)
            assertEquals(emptyList<String>(), sentEvents())
            assertEquals(0, slot.height)
        }
    }

    private companion object {
        const val HTML = "<html><body><a href=\"https://example.com\" data-pw-id=\"buy\">Buy</a></body></html>"
    }
}
