package com.pushwoosh.inline.internal.view

import android.app.Activity
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.pushwoosh.inline.internal.FIXTURE_KEYS
import com.pushwoosh.inline.internal.fixtureBanners
import com.pushwoosh.inline.internal.model.InlineAction
import com.pushwoosh.inline.internal.model.InlineBanner
import com.pushwoosh.inline.internal.model.InlineFormat
import com.pushwoosh.inline.internal.parser.InlineParser
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(sdk = [35], qualifiers = "xhdpi")
class NativeBannerRendererTest {

    private lateinit var activity: Activity
    private lateinit var host: RecordingHost
    private lateinit var root: FrameLayout

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        host = RecordingHost()
        root = FrameLayout(activity)
        activity.setContentView(root)
    }

    // Verifies that a tap on the image with its own action runs that action and a tap elsewhere runs the banner action with no element id.
    @Test
    fun testBannerTapRunsBannerActionAndImageRunsItsOwn() {
        val content = mount(
            banner(
                root = stack(
                    JSONObject().put("type", "text").put("id", "title").put("text", "Title"),
                    JSONObject()
                        .put("type", "image")
                        .put("id", "hero")
                        .put("url", "https://example.com/hero.png")
                        .put("aspect", 2)
                        .put("action", JSONObject().put("type", "url").put("value", "https://example.com/hero")),
                ).put("padding", 16),
            ).put("action", JSONObject().put("type", "deeplink").put("value", "app://banner")),
        )

        tap(content.view.findViewWithTag("hero"))
        tap(content.view.findViewWithTag("title"))

        assertEquals(
            listOf<Pair<String?, InlineAction?>>(
                "hero" to InlineAction.Url("https://example.com/hero"),
                null to InlineAction.Deeplink("app://banner"),
            ),
            host.clicks,
        )
    }

    // Verifies that the close button exists only when close.enabled and closing dismisses without a click.
    @Test
    fun testCloseButtonOnlyWhenEnabledAndDismissesWithoutClick() {
        val disabled = render(banner(text("a")).put("close", JSONObject().put("enabled", false)))
        assertEquals(0, closeButtons(disabled.view).size)

        val enabled = mount(
            banner(stack(text("a")).put("padding", 24))
                .put("close", JSONObject().put("enabled", true))
                .put("action", JSONObject().put("type", "deeplink").put("value", "app://banner")),
        )
        val buttons = closeButtons(enabled.view)
        assertEquals(1, buttons.size)
        assertEquals(px(28), buttons[0].width)

        tap(buttons[0])

        assertEquals(1, host.closes)
        assertTrue(host.clicks.isEmpty())
    }

    // Verifies that dark overrides apply only in dark mode (background color of the node view).
    @Test
    fun testDarkOverrideApplied() {
        val json = banner(
            stack(
                JSONObject()
                    .put("type", "text")
                    .put("id", "label")
                    .put("text", "x")
                    .put("dark", JSONObject().put("color", "#00FF00")),
            )
                .put("id", "card")
                .put("background", "#FFFFFF")
                .put("dark", JSONObject().put("background", "#000000")),
        )

        val light = render(json, dark = false).view
        val dark = render(json, dark = true).view

        assertEquals(0xFFFFFFFF.toInt(), backgroundColor(light.findViewWithTag("card")))
        assertEquals(0xFF000000.toInt(), backgroundColor(dark.findViewWithTag("card")))
        assertEquals(0xFF000000.toInt(), light.findViewWithTag<TextView>("label").currentTextColor)
        assertEquals(0xFF00FF00.toInt(), dark.findViewWithTag<TextView>("label").currentTextColor)
    }

    // Verifies that a fixture banner of every node type renders to a non-zero height at 360 dp width.
    @Test
    fun testFixtureBannersRender() {
        val banners = FIXTURE_KEYS.flatMap { key -> fixtureBanners(key).map { key to it } } + ("every node type" to everyNodeType())
        for ((label, json) in banners) {
            val banner = checkNotNull(InlineParser.parse(json, "slot")) { label }
            if (banner.format != InlineFormat.NATIVE) continue
            for (dark in listOf(false, true)) {
                val content = mount(json, dark)
                assertTrue("$label dark=$dark", content.view.height > 0)
                root.removeAllViews()
                content.dispose()
            }
        }
    }

    private fun everyNodeType(): JSONObject = banner(
        stack(
            text("headline").put("weight", "black").put("strike", true).put("lines", 2).put("align", "center"),
            JSONObject().put("type", "image").put("url", "https://example.com/a.png"),
            JSONObject().put("type", "video").put("url", "https://example.com/a.mp4").put("poster", "https://example.com/p.png"),
            JSONObject().put("type", "divider").put("thickness", 2),
            JSONObject().put("type", "spacer").put("size", 12),
            stack(
                JSONObject().put("type", "button").put("title", "Buy").put("padding", JSONArray().put(8).put(16)),
                JSONObject().put("type", "spacer"),
                JSONObject().put("type", "divider"),
                text("right"),
            ).put("axis", "row").put("distribution", "equal"),
            stack(
                JSONObject().put("type", "image").put("url", "https://example.com/b.png").put("aspect", 1.5),
                text("badge").put("position", "topRight"),
                text("caption").put("position", "bottom").put("glass", true),
            ).put("axis", "overlay").put("spacing", 8),
            JSONObject()
                .put("type", "pager")
                .put("interval", 2)
                .put("pages", JSONArray().put(text("page 1")).put(stack(text("page 2"), text("tall"))))
                .put("border", JSONObject().put("color", "#FF0000"))
                .put("shadow", JSONObject())
                .put("gradient", JSONObject().put("colors", JSONArray().put("#FF0000").put("#0000FF")).put("angle", 45))
                .put("backgroundImage", "https://example.com/bg.png")
                .put("cornerRadius", 12),
        ),
    )

    private fun banner(root: JSONObject): JSONObject = JSONObject()
        .put("bannerId", "b1")
        .put("version", 1)
        .put("format", "native")
        .put("native", JSONObject().put("root", root))

    private fun stack(vararg children: JSONObject): JSONObject =
        JSONObject().put("type", "stack").put("children", JSONArray().apply { children.forEach { put(it) } })

    private fun text(text: String): JSONObject = JSONObject().put("type", "text").put("text", text)

    private fun parse(json: JSONObject): InlineBanner = checkNotNull(InlineParser.parse(json, "slot"))

    private fun render(json: JSONObject, dark: Boolean = false): InlineContent =
        checkNotNull(NativeBannerRenderer().create(activity, parse(json), dark, host))

    /** Lays the banner out in the activity at 360 dp width. */
    private fun mount(json: JSONObject, dark: Boolean = false): InlineContent {
        val content = render(json, dark)
        root.addView(content.view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.measure(View.MeasureSpec.makeMeasureSpec(px(360), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(px(640), View.MeasureSpec.EXACTLY))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        ShadowLooper.idleMainLooper()
        return content
    }

    /** A finger down and up at the center of [view], dispatched from the activity root. */
    private fun tap(view: View) {
        val rect = Rect(0, 0, view.width, view.height)
        root.offsetDescendantRectToMyCoords(view, rect)
        val x = rect.exactCenterX()
        val y = rect.exactCenterY()
        val time = SystemClock.uptimeMillis()
        root.dispatchTouchEvent(MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, x, y, 0))
        root.dispatchTouchEvent(MotionEvent.obtain(time, time + 50, MotionEvent.ACTION_UP, x, y, 0))
        ShadowLooper.idleMainLooper()
    }

    private fun closeButtons(view: View): List<View> =
        ArrayList<View>().also { view.findViewsWithText(it, "Close", View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION) }

    private fun backgroundColor(view: View): Int? = (view.background as? GradientDrawable)?.color?.defaultColor

    private fun px(dp: Int): Int = (dp * activity.resources.displayMetrics.density).toInt()

    private class RecordingHost : InlineContentHost {
        val clicks = mutableListOf<Pair<String?, InlineAction?>>()
        var closes = 0

        override fun onContentSizeChanged() {}

        override fun onClick(elementId: String?, action: InlineAction?) {
            clicks += elementId to action
        }

        override fun onClose() {
            closes++
        }

        override fun onContentReady() {}
    }
}
