package com.pushwoosh.inline

import android.animation.ValueAnimator
import android.app.Activity
import android.content.ComponentCallbacks
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.pushwoosh.inapp.ui.R
import com.pushwoosh.inline.internal.FakeClock
import com.pushwoosh.inline.internal.FakeEventSink
import com.pushwoosh.inline.internal.FakeIdentity
import com.pushwoosh.inline.internal.FakeScheduler
import com.pushwoosh.inline.internal.FakeTransport
import com.pushwoosh.inline.internal.controlBanner
import com.pushwoosh.inline.internal.htmlBanner
import com.pushwoosh.inline.internal.InlineManager
import com.pushwoosh.inline.internal.InlineModule
import com.pushwoosh.inline.internal.RecordingListener
import com.pushwoosh.inline.internal.model.InlineAction
import com.pushwoosh.inline.internal.model.InlineBanner
import com.pushwoosh.inline.internal.nativeBanner
import com.pushwoosh.inline.internal.owner
import com.pushwoosh.inline.internal.store.InlineStore
import com.pushwoosh.inline.internal.view.InlineContent
import com.pushwoosh.inline.internal.view.InlineContentFactories
import com.pushwoosh.inline.internal.view.InlineContentFactory
import com.pushwoosh.inline.internal.view.InlineContentHost
import com.pushwoosh.inline.internal.view.isDark
import com.pushwoosh.internal.event.EventBus
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.platform.ApplicationOpenDetector
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
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
class PushwooshInlineViewTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var clock: FakeClock
    private lateinit var scheduler: FakeScheduler
    private lateinit var identity: FakeIdentity
    private lateinit var transport: FakeTransport
    private lateinit var events: FakeEventSink
    private lateinit var store: InlineStore
    private lateinit var manager: InlineManager
    private lateinit var activity: Activity
    private lateinit var factory: FakeContentFactory
    private lateinit var above: View
    private lateinit var below: View
    private val heights = mutableListOf<Float>()
    private lateinit var platform: MockedStatic<AndroidPlatformModule>
    private var foreground = true

    @Before
    fun setUp() {
        resetDeviceContext()
        platform = Mockito.mockStatic(AndroidPlatformModule::class.java)
        platform.`when`<Boolean> { AndroidPlatformModule.isApplicationInForeground() }.thenAnswer { foreground }
        clock = FakeClock(1_800_000_000_123L)
        scheduler = FakeScheduler(clock)
        identity = FakeIdentity()
        transport = FakeTransport()
        events = FakeEventSink()
        store = newStore()
        manager = InlineManager(store, transport, events, identity, clock, scheduler)
        InlineModule.resetForTest(manager)
        activity = Robolectric.buildActivity(Activity::class.java).setup().windowFocusChanged(true).get()
        setAnimatorsEnabled(false)
        factory = FakeContentFactory()
        InlineContentFactories.override = factory
    }

    @After
    fun tearDown() {
        activity.setContentView(View(activity))
        ShadowLooper.unPauseMainLooper()
        setAnimatorsEnabled(true)
        PushwooshInline.colorScheme = PushwooshInlineColorScheme.APP
        PushwooshInline.listener = null
        InlineModule.resetForTest(null)
        InlineContentFactories.override = null
        platform.close()
        resetDeviceContext()
    }

    private fun resetDeviceContext() {
        ReflectionHelpers.setStaticField(
            Class.forName("com.pushwoosh.inline.internal.view.InlineColorSchemeKt"),
            "deviceContext",
            null,
        )
    }

    private fun newStore() =
        InlineStore(File(folder.root, "pushwoosh/inline-banners.json"), clock, scheduler, Executor { it.run() })

    private fun setAnimatorsEnabled(enabled: Boolean) {
        Settings.Global.putFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (enabled) 1f else 0f)
        ReflectionHelpers.callStaticMethod<Unit>(
            ValueAnimator::class.java,
            "setDurationScale",
            ClassParameter.from(Float::class.javaPrimitiveType, if (enabled) 1f else 0f),
        )
    }

    private fun px(dp: Int): Int = (dp * activity.resources.displayMetrics.density).toInt()

    private fun slotView(slotId: String): PushwooshInlineView = PushwooshInlineView(activity, slotId).also(::prepare)

    private fun prepare(slot: PushwooshInlineView) {
        slot.setOnHeightChangeListener { heights += it }
    }

    /** Puts [slot] between two fixed siblings in a vertical layout on screen. */
    private fun mount(slot: PushwooshInlineView, aboveDp: Int = 40) {
        above = View(activity).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(aboveDp)) }
        below = View(activity).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(40)) }
        slot.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(above)
            addView(slot)
            addView(below)
        }
        activity.setContentView(root)
        layout()
    }

    private fun layout() {
        val root = above.parent as View
        root.measure(
            View.MeasureSpec.makeMeasureSpec(px(360), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(px(640), View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
    }

    private fun slots(vararg banners: Pair<String, JSONObject>): JSONObject =
        JSONObject().apply { for ((slotId, banner) in banners) put(slotId, banner) }

    private fun storeBanners(vararg banners: Pair<String, JSONObject>) {
        if (!store.isLoaded) store.load(identity.owner())
        store.apply(banners.map { it.first }, slots(*banners), identity.owner())
    }

    private fun expiring(id: String) = nativeBanner(id) { put("expireDate", clock.nowMillis / 1000 + 60) }

    // Verifies that an empty slot measures 0 high and never calls the height listener.
    @Test
    fun testEmptySlotHasZeroHeight() {
        val slot = slotView("home")
        mount(slot)
        scheduler.advance(500)
        transport.answer(0, JSONObject())
        layout()

        assertEquals(0, slot.height)
        assertEquals(above.bottom, below.top)
        assertEquals(emptyList<Float>(), heights)
    }

    // Verifies that an answered banner expands the slot and pushes the sibling below; the listener gets the dp height once.
    @Test
    fun testAnsweredBannerExpandsSlotAndPushesSiblings() {
        val slot = slotView("home")
        mount(slot)
        scheduler.advance(500)
        assertEquals(listOf(listOf("home")), transport.calls)

        transport.answer(0, slots("home" to nativeBanner("b1")))
        layout()

        assertEquals(px(120), slot.height)
        assertEquals(slot.top + px(120), below.top)
        assertEquals(listOf(120f), heights)
    }

    // Verifies that an empty answer collapses the slot, reports 0 and disposes the banner content.
    @Test
    fun testEmptyAnswerCollapsesSlot() {
        val slot = slotView("home")
        mount(slot)
        scheduler.advance(500)
        transport.answer(0, slots("home" to nativeBanner("b1")))
        layout()

        manager.requestSlots(listOf("home"), null)
        transport.answer(1, JSONObject())
        layout()

        assertEquals(0, slot.height)
        assertEquals(slot.top, below.top)
        assertEquals(listOf(120f, 0f), heights)
        assertTrue(factory.created.single().disposed)
    }

    // Verifies that a persisted banner shows at once on a cold start, without a request and without waiting for an animation.
    @Test
    fun testColdStartShowsPersistedBanner() {
        storeBanners("home" to nativeBanner("b1"))
        val relaunched = InlineManager(newStore(), transport, events, identity, clock, scheduler)
        InlineModule.resetForTest(relaunched)
        setAnimatorsEnabled(true)
        ShadowLooper.pauseMainLooper()

        val slot = slotView("home")
        mount(slot)
        scheduler.advance(500)

        assertEquals(px(120), slot.height)
        assertEquals("b1", factory.created.single().banner.bannerId)
        assertEquals(emptyList<List<String>>(), transport.calls)
    }

    // Verifies that app:pw_slotId from XML binds and the attached view requests its slot.
    @Test
    fun testXmlSlotIdBindsAndRequests() {
        val attrs = Robolectric.buildAttributeSet().addAttribute(R.attr.pw_slotId, "promo").build()
        val slot = PushwooshInlineView(activity, attrs).also(::prepare)

        mount(slot)
        scheduler.advance(500)

        assertEquals("promo", slot.slotId)
        assertEquals(listOf(listOf("promo")), transport.calls)
    }

    // Verifies that maxHeight caps the slot height and the reported height.
    @Test
    fun testMaxHeightCapsSlot() {
        val slot = slotView("home")
        mount(slot)
        scheduler.advance(500)
        transport.answer(0, slots("home" to nativeBanner("b1") { put("maxHeight", 80) }))
        layout()

        assertEquals(px(80), slot.height)
        assertEquals(slot.top + px(80), below.top)
        assertEquals(listOf(80f), heights)
    }

    // Verifies that reassigning slotId shows the new slot's banner and the old slot's banner and content no longer act on the view.
    @Test
    fun testReassignedSlotIdShowsNewSlotAndDropsOldTracking() {
        storeBanners("a" to nativeBanner("ba"), "b" to nativeBanner("bb"))
        val slot = slotView("a")
        mount(slot)
        val old = factory.created.single()

        slot.slotId = "b"
        scheduler.advance(1_000)
        old.host.onClick("button", null)
        old.host.onClose()
        factory.created.last().host.onClick("button", null)
        storeBanners("a" to nativeBanner("ba2", version = 2))

        assertEquals(listOf("ba", "bb"), factory.created.map { it.banner.bannerId })
        assertTrue(old.disposed)
        assertEquals(listOf("show" to "bb", "click" to "bb"), sentEvents())
    }

    // Verifies that a tapped element's action runs through the slot: a close action collapses the slot with a dismiss and no click.
    @Test
    fun testElementCloseActionDismissesWithoutClick() {
        storeBanners("home" to nativeBanner("b1"))
        val slot = slotView("home")
        mount(slot)

        factory.created.single().host.onClick("close_link", InlineAction.Close)
        layout()

        assertEquals(listOf("dismiss" to "b1"), sentEvents())
        assertEquals(0, slot.height)
    }

    // Verifies that a banner on screen is reported shown once, 0.3 s after it appeared.
    @Test
    fun testBannerOnScreenIsReportedShownAfterThreeTenthsOfASecond() {
        storeBanners("home" to nativeBanner("b1"))
        mount(slotView("home"))

        scheduler.advance(299)
        assertEquals(emptyList<Pair<String, String>>(), sentEvents())
        scheduler.advance(5_000)
        assertEquals(listOf("show" to "b1"), sentEvents())
    }

    // Verifies that a banner with less than half of its height on screen is not reported shown until more than half of it is.
    @Test
    fun testBannerMostlyBelowTheScreenIsNotReportedShown() {
        storeBanners("home" to nativeBanner("b1"))
        val slot = slotView("home")
        mount(slot, aboveDp = WINDOW_HEIGHT_DP - 50)
        scheduler.advance(5_000)
        assertEquals(emptyList<Pair<String, String>>(), sentEvents())

        above.layoutParams.height = px(WINDOW_HEIGHT_DP - 70)
        above.requestLayout()
        layout()
        scheduler.advance(600)
        assertEquals(listOf("show" to "b1"), sentEvents())
    }

    // Verifies that a banner of an app in the background is not reported shown.
    @Test
    fun testBannerOfBackgroundedAppIsNotReportedShown() {
        foreground = false
        storeBanners("home" to nativeBanner("b1"))
        mount(slotView("home"))

        scheduler.advance(5_000)

        assertEquals(emptyList<Pair<String, String>>(), sentEvents())
    }

    // Verifies that a banner whose content is not ready yet is reported shown only 0.3 s after the content reports ready.
    @Test
    fun testBannerIsReportedShownOnlyAfterItsContentIsReady() {
        factory.ready = false
        storeBanners("home" to nativeBanner("b1"))
        mount(slotView("home"))
        scheduler.advance(5_000)
        assertEquals(emptyList<Pair<String, String>>(), sentEvents())

        val content = factory.created.single()
        content.isReady = true
        content.host.onContentReady()
        scheduler.advance(300)
        assertEquals(listOf("show" to "b1"), sentEvents())
    }

    // Verifies that a control slot, 0 high, sends control once its place is on screen and draws nothing.
    @Test
    fun testControlSlotSendsControlWhenItsPlaceIsOnScreen() {
        storeBanners("home" to controlBanner("c1"))
        val slot = slotView("home")
        mount(slot)

        scheduler.advance(5_000)

        assertEquals(0, slot.height)
        assertEquals(emptyList<FakeContent>(), factory.created)
        assertEquals(listOf("control" to "c1"), sentEvents())
    }

    // Verifies that a font scale changed while the view was detached redraws the banner when it is attached again.
    @Test
    fun testFontScaleChangedWhileDetachedRedrawsBannerOnReattach() {
        storeBanners("home" to nativeBanner("b1"))
        val slot = slotView("home")
        mount(slot)
        val root = slot.parent as ViewGroup
        val index = root.indexOfChild(slot)
        root.removeView(slot)

        val configuration = Configuration(activity.resources.configuration).apply { fontScale = 1.5f }
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(configuration, activity.resources.displayMetrics)
        root.addView(slot, index)
        layout()

        assertEquals(2, factory.created.size)
        assertTrue(factory.created.first().disposed)
        assertEquals(px(180), slot.height)
    }

    // Verifies that a click held back by the content when the banner is redrawn is still reported for that banner.
    @Test
    fun testHeldClickIsReportedWhenTheBannerIsRedrawn() {
        storeBanners("home" to nativeBanner("b1"))
        PushwooshInline.colorScheme = PushwooshInlineColorScheme.LIGHT
        mount(slotView("home"))
        factory.created.single().heldClick = true

        PushwooshInline.colorScheme = PushwooshInlineColorScheme.DARK

        assertEquals(listOf("click" to "b1"), sentEvents())
        assertEquals(2, factory.created.size)
    }

    // Verifies that a detached slot disposes its content (reporting a held click), and attaching it again draws the banner anew without a request or a second show.
    @Test
    fun testDetachDisposesContentAndReattachRedrawsWithoutNewShow() {
        storeBanners("home" to nativeBanner("b1"))
        val slot = slotView("home")
        mount(slot)
        scheduler.advance(1_000)
        val first = factory.created.single()
        first.heldClick = true
        val root = slot.parent as ViewGroup
        val index = root.indexOfChild(slot)

        root.removeView(slot)
        assertTrue(first.disposed)
        assertEquals(listOf("show" to "b1", "click" to "b1"), sentEvents())

        root.addView(slot, index)
        layout()
        scheduler.advance(1_000)

        assertEquals(2, factory.created.size)
        assertFalse(factory.created.last().disposed)
        assertEquals(px(120), slot.height)
        assertEquals(listOf("show" to "b1", "click" to "b1"), sentEvents())
        assertEquals(emptyList<List<String>>(), transport.calls)
    }

    // Verifies that a control slot whose line sits exactly on the bottom edge of the visible area does not count as on screen.
    @Test
    fun testControlSlotOnTheBottomEdgeIsNotOnScreen() {
        storeBanners("home" to controlBanner("c1"))
        mount(slotView("home"), aboveDp = WINDOW_HEIGHT_DP)

        scheduler.advance(5_000)

        assertEquals(emptyList<Pair<String, String>>(), sentEvents())
    }

    // Verifies that a banner that expired while no timer ran (the process slept) collapses its slot and tells the listener when the app returns to the foreground, and a banner still valid is left alone.
    @Test
    fun testBannerExpiredDuringSleepCollapsesOnForeground() {
        val listener = RecordingListener()
        PushwooshInline.listener = listener
        InlineModule.install()
        storeBanners("home" to expiring("b1"))
        val slot = slotView("home")
        mount(slot)
        listener.updates.clear()

        EventBus.sendEvent(ApplicationOpenDetector.ApplicationMovedToForegroundEvent())
        ShadowLooper.idleMainLooper()
        layout()
        assertTrue(slot.height > 0)
        assertTrue(listener.updates.isEmpty())

        clock.nowMillis += 3_600_000L
        EventBus.sendEvent(ApplicationOpenDetector.ApplicationMovedToForegroundEvent())
        ShadowLooper.idleMainLooper()
        layout()

        assertEquals(0, slot.height)
        assertEquals(listOf<Pair<String, String?>>("home" to null), listener.updates)
    }

    // Verifies that a slot view attached after its cached banner expired without a timer stays collapsed, and the listener and other slots are told of the expiry only after the attach, from the scheduler.
    @Test
    fun testBannerExpiredDuringSleepIsDroppedWhenSlotAttaches() {
        val listener = RecordingListener()
        PushwooshInline.listener = listener
        storeBanners("home" to expiring("b1"), "side" to expiring("s1"))
        val side = mutableListOf<String?>()
        manager.subscribe("side") { side += it?.bannerId }
        side.clear()
        listener.updates.clear()
        clock.nowMillis += 3_600_000L

        val slot = slotView("home")
        mount(slot)

        assertEquals(0, slot.height)
        assertTrue(listener.updates.isEmpty())
        assertTrue(side.isEmpty())

        scheduler.advance(0)

        assertEquals(listOf<Pair<String, String?>>("home" to null, "side" to null), listener.updates)
        assertEquals(listOf<String?>(null), side)
    }

    // Verifies that a tap on a banner that expired while no timer ran opens nothing, reports no click and collapses the slot.
    @Test
    fun testTapOnExpiredBannerRunsNoActionAndCollapses() {
        val listener = RecordingListener()
        PushwooshInline.listener = listener
        storeBanners("home" to expiring("b1"))
        val slot = slotView("home")
        mount(slot)
        clock.nowMillis += 3_600_000L

        factory.created.single().host.onClick("cta", InlineAction.Url("https://example.com/x"))
        layout()

        assertNull(shadowOf(activity).nextStartedActivity)
        assertTrue(events.events.isEmpty())
        assertTrue(listener.clicked.isEmpty())
        assertEquals(0, slot.height)
    }

    private fun sentEvents(): List<Pair<String, String>> = events.events.map { it.getString("type") to it.getString("bannerId") }

    // Verifies that the color scheme maps to dark/light per APP/SYSTEM/LIGHT/DARK.
    @Test
    @Config(sdk = [28])
    fun testColorSchemeMapping() {
        class Case(val name: String, val scheme: PushwooshInlineColorScheme, val appNight: Boolean, val viewNight: Boolean, val dark: Boolean)
        val cases = listOf(
            Case("light ignores night", PushwooshInlineColorScheme.LIGHT, appNight = true, viewNight = true, dark = false),
            Case("dark ignores day", PushwooshInlineColorScheme.DARK, appNight = false, viewNight = false, dark = true),
            Case("system follows device night", PushwooshInlineColorScheme.SYSTEM, appNight = true, viewNight = false, dark = true),
            Case("system ignores the view", PushwooshInlineColorScheme.SYSTEM, appNight = false, viewNight = true, dark = false),
            Case("app follows the view", PushwooshInlineColorScheme.APP, appNight = false, viewNight = true, dark = true),
            Case("app ignores the application", PushwooshInlineColorScheme.APP, appNight = true, viewNight = false, dark = false),
        )
        for (c in cases) {
            RuntimeEnvironment.setQualifiers(if (c.appNight) "+night" else "+notnight")
            assertEquals("case ${c.name}", c.dark, isDark(c.scheme, viewContext(night = c.viewNight)))
        }
    }

    // Verifies that an application night override cannot change SYSTEM, while APP follows the host.
    @Test
    fun testSystemSchemeIgnoresApplicationNightOverride() {
        val deviceResources = Mockito.mock(Resources::class.java)
        val appResources = Mockito.mock(Resources::class.java)
        val device = object : ContextWrapper(activity) {
            override fun getResources(): Resources = deviceResources
        }
        val app = object : ContextWrapper(activity) {
            override fun getResources(): Resources = appResources
            override fun createWindowContext(display: Display, type: Int, options: android.os.Bundle?): Context = device
        }
        val host = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = app
        }
        for (systemNight in listOf(false, true)) {
            RuntimeEnvironment.setQualifiers(if (systemNight) "+night" else "+notnight")
            val override = Configuration(activity.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (systemNight) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES
            }
            val deviceConfig = Configuration(override).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (systemNight) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            Mockito.`when`(deviceResources.configuration).thenReturn(deviceConfig)
            Mockito.`when`(appResources.configuration).thenReturn(override)

            assertEquals("systemNight=$systemNight", systemNight, isDark(PushwooshInlineColorScheme.SYSTEM, host))
            assertEquals(!systemNight, isDark(PushwooshInlineColorScheme.APP, host))
            assertFalse(isDark(PushwooshInlineColorScheme.LIGHT, host))
            assertTrue(isDark(PushwooshInlineColorScheme.DARK, host))
        }
    }

    @Test
    @Config(sdk = [31, 35])
    fun testSystemConfigurationUpdatesAttachedBannersWithForcedAppTheme() {
        val theme = SystemThemeContexts()
        storeBanners("home" to nativeBanner("b1"), "html" to htmlBanner("h1"))
        PushwooshInline.colorScheme = PushwooshInlineColorScheme.SYSTEM
        val nativeSlot = PushwooshInlineView(theme.host, "home").also(::prepare)
        val htmlSlot = PushwooshInlineView(theme.host, "html").also(::prepare)
        mount(nativeSlot)
        val root = nativeSlot.parent as ViewGroup
        root.addView(htmlSlot)
        layout()
        assertEquals(listOf(false, false), factory.created.map { it.dark })

        theme.changeDeviceNight(true)
        assertEquals(listOf(false, false, true, true), factory.created.map { it.dark })
        assertTrue(factory.created.take(2).all { it.disposed })
        assertEquals(listOf("b1", "h1"), factory.created.takeLast(2).map { it.banner.bannerId })
        assertFalse(isDark(PushwooshInlineColorScheme.APP, theme.host))
        assertEquals(listOf(120f, 120f), heights)

        theme.changeDeviceNight(true)
        assertEquals(4, factory.created.size)
        theme.changeDeviceNight(false)
        assertEquals(listOf(false, false), factory.created.takeLast(2).map { it.dark })
        assertEquals(6, factory.created.size)
        assertTrue(factory.created.take(4).all { it.disposed })

        root.removeView(nativeSlot)
        assertEquals(1, theme.callbacks.size)
        theme.changeDeviceNight(true)
        assertEquals(7, factory.created.size)
        assertEquals("h1", factory.created.last().banner.bannerId)
        assertTrue(factory.created.last().dark)
        root.addView(nativeSlot)
        assertEquals(2, theme.callbacks.size)
        assertEquals(8, factory.created.size)
        assertEquals("b1", factory.created.last().banner.bannerId)
        assertTrue(factory.created.last().dark)

        root.removeView(nativeSlot)
        root.removeView(htmlSlot)
        assertTrue(theme.callbacks.isEmpty())
        theme.changeDeviceNight(false)
        assertEquals(8, factory.created.size)
        assertTrue(factory.created.all { it.disposed })
    }

    @Test
    @Config(sdk = [31, 35])
    fun testSystemObservationFollowsColorSchemeChangesWhileAttached() {
        val theme = SystemThemeContexts()
        storeBanners("home" to nativeBanner("b1"))
        val slot = PushwooshInlineView(theme.host, "home")
        mount(slot)
        assertTrue(theme.callbacks.isEmpty())

        for (scheme in listOf(PushwooshInlineColorScheme.LIGHT, PushwooshInlineColorScheme.DARK, PushwooshInlineColorScheme.APP)) {
            PushwooshInline.colorScheme = PushwooshInlineColorScheme.SYSTEM
            assertEquals(1, theme.callbacks.size)
            theme.changeDeviceNight(true)
            assertTrue(factory.created.last().dark)

            PushwooshInline.colorScheme = scheme
            assertTrue(theme.callbacks.isEmpty())
            assertEquals(scheme == PushwooshInlineColorScheme.DARK, factory.created.last().dark)
            val count = factory.created.size
            theme.changeDeviceNight(false)
            assertEquals(count, factory.created.size)
        }

        PushwooshInline.colorScheme = PushwooshInlineColorScheme.SYSTEM
        assertEquals(1, theme.callbacks.size)
        theme.changeDeviceNight(true)
        assertTrue(factory.created.last().dark)
    }

    private inner class SystemThemeContexts {
        val callbacks = mutableListOf<ComponentCallbacks>()
        private val configuration = Configuration(activity.resources.configuration).apply {
            uiMode = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_NO
        }
        private val deviceResources = Mockito.mock(Resources::class.java).also {
            Mockito.`when`(it.configuration).thenAnswer { Configuration(configuration) }
        }
        private val device = object : ContextWrapper(activity) {
            override fun getResources(): Resources = deviceResources
            override fun registerComponentCallbacks(callback: ComponentCallbacks) { callbacks += callback }
            override fun unregisterComponentCallbacks(callback: ComponentCallbacks) { callbacks -= callback }
        }
        private val app = object : ContextWrapper(activity) {
            override fun createWindowContext(display: Display, type: Int, options: android.os.Bundle?): Context = device
        }
        val host = object : ContextThemeWrapper(activity, 0) {
            override fun getApplicationContext(): Context = app
        }.apply { applyOverrideConfiguration(Configuration(configuration)) }

        fun changeDeviceNight(night: Boolean) {
            configuration.uiMode = Configuration.UI_MODE_TYPE_NORMAL or
                if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            callbacks.toList().forEach { it.onConfigurationChanged(Configuration(configuration)) }
        }
    }

    // Verifies that changing the color scheme redraws the shown banner in the new scheme.
    @Test
    fun testColorSchemeChangeRedrawsBanner() {
        storeBanners("home" to nativeBanner("b1"))
        PushwooshInline.colorScheme = PushwooshInlineColorScheme.LIGHT
        val slot = slotView("home")
        mount(slot)

        PushwooshInline.colorScheme = PushwooshInlineColorScheme.DARK

        assertEquals(listOf(false, true), factory.created.map { it.dark })
        assertTrue(factory.created.first().disposed)
        assertFalse(factory.created.last().disposed)
        assertEquals(listOf(120f), heights)
    }

    // Verifies that a font scale change redraws the shown banner and reports its new height.
    @Test
    fun testFontScaleChangeRedrawsBannerAndReportsHeight() {
        storeBanners("home" to nativeBanner("b1"))
        val slot = slotView("home")
        mount(slot)

        val configuration = Configuration(activity.resources.configuration).apply { fontScale = 1.5f }
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(configuration, activity.resources.displayMetrics)
        slot.dispatchConfigurationChanged(configuration)
        layout()

        assertEquals(2, factory.created.size)
        assertTrue(factory.created.first().disposed)
        assertEquals(listOf(120f, 180f), heights)
        assertEquals(px(180), slot.height)
    }

    private fun viewContext(night: Boolean): Context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), 0).apply {
        val mode = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        applyOverrideConfiguration(Configuration().apply { uiMode = mode or Configuration.UI_MODE_TYPE_NORMAL })
    }

    private class FakeContent(
        context: Context,
        heightPx: Int,
        val banner: InlineBanner,
        val dark: Boolean,
        val host: InlineContentHost,
    ) : InlineContent {
        var disposed = false

        /** A tap held back as an own-control click is reported when the content leaves, like an HTML banner does. */
        var heldClick = false

        override val view: View = object : View(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), heightPx)
            }
        }

        override var isReady: Boolean = true

        override fun retire() {
            reportHeldClick()
        }

        override fun dispose() {
            reportHeldClick()
            disposed = true
        }

        private fun reportHeldClick() {
            if (!heldClick) return
            heldClick = false
            host.onClick(null, null)
        }
    }

    /** Draws every banner as a view 120 dp high times the font scale it is drawn at. */
    private inner class FakeContentFactory : InlineContentFactory {
        val created = mutableListOf<FakeContent>()
        var ready = true

        override fun create(context: Context, banner: InlineBanner, dark: Boolean, host: InlineContentHost): InlineContent =
            FakeContent(activity, (px(120) * activity.resources.configuration.fontScale).toInt(), banner, dark, host).also {
                it.isReady = ready
                created += it
            }
    }

    private companion object {
        /** Robolectric's default window height, the screen part a slot can be seen in. */
        const val WINDOW_HEIGHT_DP = 470
    }
}
