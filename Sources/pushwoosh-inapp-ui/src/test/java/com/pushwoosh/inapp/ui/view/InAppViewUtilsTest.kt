package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppButton
import com.pushwoosh.inapp.ui.model.InAppText
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowGradientDrawable

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InAppViewUtilsTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @Test
    fun makeTextClampsWhenMaxLinesPositive() {
        val tv = InAppViewUtils.makeText(activity, "long text", Color.WHITE, 14f, false, maxLines = 2)
        assertEquals(2, tv.maxLines)
        assertEquals(TextUtils.TruncateAt.END, tv.ellipsize)
    }

    @Test
    fun makeTextLeavesEllipsizeNullByDefault() {
        val tv = InAppViewUtils.makeText(activity, "long text", Color.WHITE, 14f, false)
        assertNull(tv.ellipsize)
    }

    private fun button(
        textColor: Int = Color.WHITE,
        background: Int = Color.BLUE,
        border: Int = Color.RED
    ) = InAppButton(
        text = InAppText("Tap", textColor),
        backgroundColor = background,
        borderColor = border,
        cornerRadiusDp = 8f,
        action = InAppAction.Close
    )

    @Test
    fun buttonUsesTextColorFromModel() {
        val b = InAppViewUtils.makeButton(activity, button(textColor = Color.GREEN)) {}
        assertEquals(Color.GREEN, b.currentTextColor)
    }

    @Test
    fun buttonFillIsBackgroundColor() {
        val b = InAppViewUtils.makeButton(activity, button(background = Color.BLUE)) {}
        val bg = Shadow.extract<ShadowGradientDrawable>(b.background)
        assertEquals(Color.BLUE, bg.lastSetColor)
    }

    @Test
    fun buttonHasBorderInBorderColor() {
        val b = InAppViewUtils.makeButton(activity, button(border = Color.RED)) {}
        val bg = Shadow.extract<ShadowGradientDrawable>(b.background)
        assertTrue("button must have a border stroke", bg.strokeWidth > 0)
        assertEquals(Color.RED, bg.strokeColor)
    }

    @Test
    fun buttonDispatchesActionOnClick() {
        var dispatched: InAppAction? = null
        val b = InAppViewUtils.makeButton(activity, button()) { dispatched = it }
        b.performClick()
        assertEquals(InAppAction.Close, dispatched)
    }

    @Test
    fun cardCloseButtonWrapsChipInInset() {
        val b = InAppViewUtils.makeCardCloseButton(activity) {}
        assertTrue("visible chip must be inset from the 48dp touch target", b.background is InsetDrawable)
        assertEquals("Close", b.contentDescription)
    }

    @Test
    fun cardCloseButtonDispatchesClick() {
        var clicked = false
        val b = InAppViewUtils.makeCardCloseButton(activity) { clicked = true }
        b.performClick()
        assertTrue(clicked)
    }

    @Test
    fun inlineCloseButtonHasNoChip() {
        val b = InAppViewUtils.makeInlineCloseButton(activity) {}
        assertNull("the banner's ✕ is drawn bare, like iOS — no oval chip behind it", b.background)
        assertEquals(Color.parseColor("#99FFFFFF"), b.currentTextColor)
        assertEquals("Close", b.contentDescription)
    }

    @Test
    fun clipToRoundedCornersTrimsContent() {
        val view = View(activity)
        view.layout(0, 0, 100, 100)

        InAppViewUtils.clipToRoundedCorners(view, 12f)

        assertTrue("content must be trimmed, not just painted behind", view.clipToOutline)
        assertNotSame(
            "a rounded outline must replace the default background-shaped provider",
            ViewOutlineProvider.BACKGROUND,
            view.outlineProvider
        )
    }

    // Outline.getRadius() is API 24 and this module's Robolectric default is SDK 21.
    @Test
    @Config(sdk = [34])
    fun clipToRoundedCornersUsesGivenRadius() {
        val view = View(activity)
        view.layout(0, 0, 100, 100)
        InAppViewUtils.clipToRoundedCorners(view, 12f)

        val outline = Outline()
        view.outlineProvider!!.getOutline(view, outline)

        assertEquals(12f, outline.radius, 0.5f)
    }

    // GradientDrawable.getCornerRadii() is API 24 and this module's Robolectric default is SDK 21.
    @Test
    @Config(sdk = [34])
    fun topRoundedBackgroundRoundsOnlyTheTopCorners() {
        val drawable = InAppViewUtils.topRoundedBackground(Color.WHITE, 28f)

        assertArrayEquals(
            "top-left, top-right rounded; bottom corners square so the sheet sits flush",
            floatArrayOf(28f, 28f, 28f, 28f, 0f, 0f, 0f, 0f),
            drawable.cornerRadii,
            0.5f
        )
        assertEquals(Color.WHITE, Shadow.extract<ShadowGradientDrawable>(drawable).lastSetColor)
    }

    // GradientDrawable.getColors()/getOrientation() are API 24 and this module's Robolectric
    // default is SDK 21.
    @Test
    @Config(sdk = [34])
    fun bottomScrimFadesFromClearToSeventyFivePercentBlack() {
        val scrim = InAppViewUtils.bottomScrim(activity)
        val gradient = scrim.background as GradientDrawable

        assertEquals("iOS's scrim band is 140pt tall", 140f, InAppViewUtils.BOTTOM_SCRIM_HEIGHT_DP, 0f)
        assertEquals(
            "the nominal band height, kept as the minimum for an UNSPECIFIED spec — callers still " +
                "size the band themselves (see attachBottomScrim)",
            InAppViewUtils.dp(activity, 140f), scrim.minimumHeight
        )
        assertEquals(GradientDrawable.Orientation.TOP_BOTTOM, gradient.orientation)
        assertArrayEquals(
            "iOS scrim stops: clear on top, black at 0.75 at the bottom",
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#BF000000")),
            gradient.colors
        )
    }

    /** A template in miniature: full-bleed "image" plus a bottom-pinned "column" of a fixed height
     *  carrying a top padding, like the real ones (24dp fullscreen / 20dp stories). The height is
     *  fixed so the geometry doesn't depend on how Robolectric measures text. */
    private fun scrimHost(columnHeight: Int, paddingTop: Int): FrameLayout {
        val host = FrameLayout(activity)
        host.addView(
            View(activity),
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        val column = View(activity).apply { setPadding(0, paddingTop, 0, 0) }
        host.addView(
            column,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, columnHeight, Gravity.BOTTOM)
        )
        return host
    }

    private fun column(host: FrameLayout): View = host.getChildAt(host.childCount - 1)

    /** One layout pass. `forceLayout` is not belt-and-braces: the scrim's height is set from the
     *  column's layout callback, and a plain repeat of measure/layout on an already-laid-out tree is
     *  a no-op, so without it the second pass would never pick the new height up. */
    private fun layoutHost(host: FrameLayout, width: Int = 1080, height: Int = 1920) {
        host.forceLayout()
        host.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        host.layout(0, 0, width, height)
    }

    @Test
    fun attachBottomScrimSitsOverTheImageAndUnderTheColumn() {
        val host = scrimHost(columnHeight = 300, paddingTop = 24)
        val image = host.getChildAt(0)

        val scrim = InAppViewUtils.attachBottomScrim(column(host), 48f)

        assertTrue("the darkening must cover the image", host.indexOfChild(scrim) > host.indexOfChild(image))
        assertTrue(
            "and stay under the text it protects",
            host.indexOfChild(scrim) < host.indexOfChild(column(host))
        )
        assertFalse("a band that eats taps would swallow button clicks", scrim.isClickable)
    }

    /** The fallback for a column that never lays out, asserted on the *measured* band rather than on
     *  its layout params: a WRAP_CONTENT band plus [InAppViewUtils.bottomScrim]'s `minimumHeight` reads
     *  like the carousel's 140dp strip and measures 1920 tall — `getDefaultSize` hands a plain View the
     *  whole AT_MOST spec and consults the minimum only for UNSPECIFIED. That is the full-screen
     *  darkening the spec rules out, so the params alone prove nothing. Measure only, no layout: the
     *  column's callback is what sizes the band, and this is the state before it ever runs. */
    @Test
    fun attachBottomScrimStartsAsTheCarouselBand() {
        val host = scrimHost(columnHeight = 300, paddingTop = 24)

        val scrim = InAppViewUtils.attachBottomScrim(column(host), 48f)
        host.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        assertEquals(
            "an unsized band must be the carousel's strip, not a full-screen dim",
            InAppViewUtils.dp(activity, InAppViewUtils.BOTTOM_SCRIM_HEIGHT_DP),
            scrim.measuredHeight
        )
        assertTrue("the gradient is the one the carousel uses", scrim.background is GradientDrawable)
        assertEquals(Gravity.BOTTOM, (scrim.layoutParams as FrameLayout.LayoutParams).gravity)
    }

    @Test
    fun attachBottomScrimHeightRisesAboveTheFirstTextLine() {
        val host = scrimHost(columnHeight = 300, paddingTop = 24)
        val scrim = InAppViewUtils.attachBottomScrim(column(host), 48f)

        layoutHost(host)

        assertEquals(
            "height = column − its top padding + rise: iOS anchors the scrim to a padding-less stack, " +
                "so the rise is measured from the first line of text",
            300 - 24 + InAppViewUtils.dp(activity, 48f),
            scrim.layoutParams.height
        )
    }

    /** The guard against the requestLayout loop (write layout params only when the height actually
     *  changed) is deliberately untested: Robolectric does not reproduce the flag — dropping the guard
     *  leaves `scrim.isLayoutRequested` false, so any assertion on it is green either way, and a repeat
     *  layout pass is invariant with or without it. Don't add one; it would only look like coverage. */
    @Test
    fun attachBottomScrimSpansFromAboveTheTextToTheBottomEdge() {
        val host = scrimHost(columnHeight = 300, paddingTop = 24)
        val scrim = InAppViewUtils.attachBottomScrim(column(host), 48f)

        layoutHost(host) // pass 1: the listener sets the height
        layoutHost(host) // pass 2: the new height reaches layout (in production ViewRootImpl does this)

        assertEquals("bottom edge flush with the column's — the screen edge", column(host).bottom, scrim.bottom)
        assertEquals(
            "top edge a rise above the first line of text",
            column(host).top + 24 - InAppViewUtils.dp(activity, 48f),
            scrim.top
        )
    }

    /** The rise must be converted to pixels: Robolectric's default density is 1, where a dp and a px
     *  are the same number, so every other geometry assertion here stays green on a px-based rise —
     *  while on a real xxhdpi screen the band would come out 96px short and the first line of text
     *  would land back on the bare image, which is the whole defect this scrim fixes. */
    @Test
    @Config(qualifiers = "xxhdpi")
    fun attachBottomScrimRiseIsInDpNotPixels() {
        assertEquals(
            "the fixture is pointless at density 1",
            3f, activity.resources.displayMetrics.density, 0f
        )
        val host = scrimHost(columnHeight = 300, paddingTop = 24)
        val scrim = InAppViewUtils.attachBottomScrim(column(host), 48f)

        layoutHost(host)

        assertEquals("48dp at xxhdpi is 144px", 300 - 24 + 144, scrim.layoutParams.height)
    }

}
