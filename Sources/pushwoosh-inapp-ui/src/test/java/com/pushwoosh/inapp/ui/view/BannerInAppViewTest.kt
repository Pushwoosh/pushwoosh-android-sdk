package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pushwoosh.inapp.ui.animation.ReduceMotionUtil
import com.pushwoosh.inapp.ui.model.BannerContent
import com.pushwoosh.inapp.ui.model.BannerPosition
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppText
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class BannerInAppViewTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @Test
    fun topBannerClosesOnUpwardFling() {
        assertTrue(shouldCloseOnSwipe(BannerPosition.TOP, 0f, -1200f))
        assertFalse(shouldCloseOnSwipe(BannerPosition.TOP, 0f, 1200f))
    }

    @Test
    fun bottomBannerClosesOnDownwardFling() {
        assertTrue(shouldCloseOnSwipe(BannerPosition.BOTTOM, 0f, 1200f))
        assertFalse(shouldCloseOnSwipe(BannerPosition.BOTTOM, 0f, -1200f))
    }

    @Test
    fun horizontalDominantFlingDoesNotClose() {
        assertFalse(shouldCloseOnSwipe(BannerPosition.TOP, -2000f, -300f))
        assertFalse(shouldCloseOnSwipe(BannerPosition.BOTTOM, 2000f, 300f))
    }

    // The banner card is always clickable (it always carries an action now); this guards that the
    // card stays a touch target so the GestureDetector receives MOVE/UP and swipe-to-close works.
    @Test
    fun bannerCardIsClickable() {
        val content = BannerContent(
            position = BannerPosition.TOP,
            imageUrl = null,
            title = InAppText("Info", Color.WHITE),
            message = null,
            backgroundColor = Color.BLACK,
            action = InAppAction.Close,
            autoDismissMs = 0L,
            showCloseButton = false
        )
        val view = BannerInAppView(activity, content)
        val card = view.getChildAt(0) as LinearLayout
        assertTrue("banner card must be clickable so swipe gestures are received", card.isClickable)
    }

    private fun content(showCloseButton: Boolean) = BannerContent(
        position = BannerPosition.TOP,
        imageUrl = null,
        title = InAppText("Info", Color.WHITE),
        message = null,
        backgroundColor = Color.BLACK,
        action = InAppAction.Close,
        autoDismissMs = 0L,
        showCloseButton = showCloseButton
    )

    @Test
    fun closeButtonFollowsShowCloseButtonFlag() {
        assertNotNull("banner with showCloseButton=true must show the inline ✕",
            findCloseButton(BannerInAppView(activity, content(showCloseButton = true))))
        assertNull("banner has swipe-to-dismiss — the ✕ stays opt-in",
            findCloseButton(BannerInAppView(activity, content(showCloseButton = false))))
    }

    // The ✕ closes the banner. B7 swapped the chip variant for makeInlineCloseButton, and the
    // handler travels with the helper call — this guards that the swap kept it, the way the other
    // four templates guard their own ✕.
    @Test
    fun closeButtonClosesBanner() {
        val view = BannerInAppView(activity, content(showCloseButton = true))
        val closes = countCloses(view)

        findCloseButton(view)!!.performClick()

        assertEquals("the banner's ✕ must request a close", 1, closes[0])
    }

    private fun autoDismissContent(autoDismissMs: Long) = BannerContent(
        position = BannerPosition.TOP,
        imageUrl = null,
        title = InAppText("Info", Color.WHITE),
        message = null,
        backgroundColor = Color.BLACK,
        action = InAppAction.Close,
        autoDismissMs = autoDismissMs,
        showCloseButton = false
    )

    // Attaches the banner: an unattached view's postDelayed never reaches the real handler in
    // production, and the controller always shows the banner attached.
    private fun attach(view: BannerInAppView) = activity.setContentView(view)

    private fun countCloses(view: BannerInAppView): IntArray {
        val closes = intArrayOf(0)
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {}
            override fun onClose() {
                closes[0]++
            }
        }
        return closes
    }

    private fun advanceBy(millis: Long) =
        Robolectric.getForegroundThreadScheduler().advanceBy(millis, TimeUnit.MILLISECONDS)

    // B1: the autoDismiss timer belongs to the view — animateIn() arms it, the controller schedules
    // nothing. Robolectric finishes the entrance inside animateIn() without advancing the virtual
    // clock, so this test cannot tell arming-at-start from arming-at-end; the cancelled-entrance
    // case that *does* distinguish them is dismissDuringEntranceArmsNoTimer, and the wall-clock
    // offset itself is an e2e assertion.
    @Test
    fun autoDismissClosesBannerAfterEntranceAnimation() {
        val view = BannerInAppView(activity, autoDismissContent(1000L))
        attach(view)
        val closes = countCloses(view)

        view.animateIn()
        assertEquals("the close must be posted, not fired inline from animateIn", 0, closes[0])

        advanceBy(1500L)
        assertEquals("the banner must close itself once autoDismiss elapses", 1, closes[0])
    }

    // B2: a dismissal cancels the pending close. Without removeCallbacks the runnable survives the
    // dismissal and keeps a strong reference to a detached view for the rest of the autoDismiss
    // window — the iOS dismiss() invalidates the timer instead.
    @Test
    fun animateOutCancelsPendingAutoDismiss() {
        val view = BannerInAppView(activity, autoDismissContent(1000L))
        attach(view)
        val closes = countCloses(view)
        view.animateIn()

        view.animateOut {}
        advanceBy(2000L)

        assertEquals("a dismissed banner must not fire autoDismiss afterwards", 0, closes[0])
    }

    // B2, host-Activity death: the view leaves the window without any dismiss (navigation,
    // rotation, Activity destroyed). removeView() alone does not drop the pending runnable, so
    // onDetachedFromWindow must — the analogue of the iOS deinit invalidating the timer.
    @Test
    fun detachCancelsPendingAutoDismiss() {
        val view = BannerInAppView(activity, autoDismissContent(1000L))
        attach(view)
        val closes = countCloses(view)
        view.animateIn()

        (view.parent as ViewGroup).removeView(view)
        advanceBy(2000L)

        assertEquals("a detached banner must not hold a pending close", 0, closes[0])
    }

    // autoDismiss = 0 means "keep it until the user acts" (the parser's contract): no timer at all
    // — in particular not a postDelayed(0) that would close the banner in the same frame.
    @Test
    fun zeroAutoDismissNeverArmsTimer() {
        val view = BannerInAppView(activity, autoDismissContent(0L))
        attach(view)
        val closes = countCloses(view)

        view.animateIn()
        advanceBy(5000L)

        assertEquals("autoDismiss=0 must never close the banner by itself", 0, closes[0])
    }

    // A dismissal that lands *during* the slide-in must leave nothing pending: the entrance is
    // cancelled, and a cancelled entrance arms no timer. This is the whole reason slideIn hangs the
    // callback on withEndAction and not on setListener — the latter also runs onAnimationEnd on
    // cancel, i.e. after animateOut has already cleared the timer, so the banner would re-arm on its
    // way out and fire a close on a view nobody can see. Pausing the scheduler is what makes the
    // mid-animation window observable at all: unpaused, Robolectric finishes the entrance inside
    // animateIn().
    @Test
    fun dismissDuringEntranceArmsNoTimer() {
        val view = BannerInAppView(activity, autoDismissContent(1000L))
        attach(view)
        val closes = countCloses(view)
        val scheduler = Robolectric.getForegroundThreadScheduler()

        scheduler.pause()
        view.animateIn()
        // Non-vacuity guard: alpha is still at its entrance start value, so the dismissal below
        // really does interrupt the slide-in instead of following a finished one.
        assertEquals("the entrance must still be in flight here", 0f, card(view).alpha, 0.001f)
        view.animateOut {}
        scheduler.unPause()
        advanceBy(3000L)

        assertEquals("a cancelled entrance must leave no pending autoDismiss", 0, closes[0])
    }

    // Reduce motion drops the slide but keeps the entrance, so autoDismiss must still be armed:
    // slideIn hangs onEnd on both of its branches. Dropping it from the reduce-motion branch would
    // silently cost accessibility users every self-dismissing banner.
    @Test
    fun autoDismissAlsoArmsUnderReduceMotion() {
        Settings.Global.putFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        // Guards the fixture itself: a banner built with reduce motion off would make this test a
        // copy of autoDismissClosesBannerAfterEntranceAnimation.
        assertTrue(ReduceMotionUtil.isReduceMotionEnabled(activity))
        val view = BannerInAppView(activity, autoDismissContent(1000L))
        attach(view)
        val closes = countCloses(view)

        view.animateIn()
        advanceBy(1500L)

        assertEquals("reduce motion must not cost the banner its autoDismiss", 1, closes[0])
    }

    private fun geometryContent(
        position: BannerPosition = BannerPosition.TOP,
        imageUrl: String? = null,
        title: InAppText? = InAppText("Title", Color.WHITE),
        message: InAppText? = InAppText("Body", Color.WHITE),
        showCloseButton: Boolean = true
    ) = BannerContent(
        position = position,
        imageUrl = imageUrl,
        title = title,
        message = message,
        backgroundColor = Color.BLACK,
        action = InAppAction.Close,
        autoDismissMs = 0L,
        showCloseButton = showCloseButton
    )

    private fun card(view: BannerInAppView) = view.getChildAt(0) as LinearLayout

    private fun dp(value: Float) = InAppViewUtils.dp(activity, value)

    private fun dispatchInsets(view: BannerInAppView) {
        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(11, 22, 33, 44))
                .build()
        )
    }

    // GradientDrawable.getCornerRadius() is API 24; this module's Robolectric default is SDK 21.
    @Test
    @Config(sdk = [34])
    fun cardCornerRadiusMatchesIos() {
        val background = card(BannerInAppView(activity, geometryContent())).background as GradientDrawable
        assertEquals(dp(18f).toFloat(), background.cornerRadius, 0.5f)
    }

    @Test
    fun cardPaddingIsWiderHorizontally() {
        val bar = card(BannerInAppView(activity, geometryContent()))
        assertEquals(dp(14f), bar.paddingLeft)
        assertEquals(dp(14f), bar.paddingRight)
        assertEquals(dp(12f), bar.paddingTop)
        assertEquals(dp(12f), bar.paddingBottom)
    }

    @Test
    fun titleIsSixteenSp() {
        val column = card(BannerInAppView(activity, geometryContent())).getChildAt(0) as LinearLayout
        val title = column.getChildAt(0) as TextView
        val expected = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 16f, activity.resources.displayMetrics
        )
        assertEquals(expected, title.textSize, 0.5f)
    }

    // iOS gets this gap from UIStackView.spacing, which sits *between* items — so a banner with
    // only a message must not be pushed down by it.
    @Test
    fun messageGapAppliesOnlyBelowTitle() {
        val withTitle = card(BannerInAppView(activity, geometryContent())).getChildAt(0) as LinearLayout
        val gapped = withTitle.getChildAt(1).layoutParams as LinearLayout.LayoutParams
        assertEquals(dp(2f), gapped.topMargin)

        val titleless = card(BannerInAppView(activity, geometryContent(title = null))).getChildAt(0) as LinearLayout
        val ungapped = titleless.getChildAt(0).layoutParams as LinearLayout.LayoutParams
        assertEquals("nothing above the message means no gap", 0, ungapped.topMargin)
    }

    // iOS uses two different numbers here: 10 from the safe area on the pinned edge, 12 from the
    // container on the sides. One shared margin cannot express both — the horizontal 12 already
    // matched iOS and must survive this change.
    @Test
    fun cardMarginsAreTwelveHorizontallyAndTenVertically() {
        val lp = card(BannerInAppView(activity, geometryContent())).layoutParams as FrameLayout.LayoutParams
        assertEquals(dp(12f), lp.leftMargin)
        assertEquals(dp(12f), lp.rightMargin)
        assertEquals(dp(10f), lp.topMargin)
        assertEquals(dp(10f), lp.bottomMargin)
    }

    // The pinned edge adds its system inset (status bar for TOP, nav bar for BOTTOM); the opposite
    // edge keeps the bare 10dp, because the banner wraps its height and never reaches the far edge.
    // The horizontal 12dp takes its inset on both sides regardless of position.
    //
    // SDK 34, not this module's Robolectric default of 21: on 21 WindowInsetsCompat reaches the
    // listener with its bottom inset zeroed (left/top/right survive), which would silently pass the
    // BOTTOM branch for the wrong reason. Verified by probe — on 34 all four insets arrive.
    @Test
    @Config(sdk = [34])
    fun insetsAddToThePinnedEdgeOnly() {
        val top = BannerInAppView(activity, geometryContent(position = BannerPosition.TOP))
        attach(top)
        dispatchInsets(top)
        val topLp = card(top).layoutParams as FrameLayout.LayoutParams
        assertEquals(dp(12f) + 11, topLp.leftMargin)
        assertEquals(dp(10f) + 22, topLp.topMargin)
        assertEquals(dp(12f) + 33, topLp.rightMargin)
        assertEquals("a top banner ignores the bottom inset", dp(10f), topLp.bottomMargin)

        val bottom = BannerInAppView(activity, geometryContent(position = BannerPosition.BOTTOM))
        attach(bottom)
        dispatchInsets(bottom)
        val bottomLp = card(bottom).layoutParams as FrameLayout.LayoutParams
        assertEquals(dp(12f) + 11, bottomLp.leftMargin)
        assertEquals(dp(12f) + 33, bottomLp.rightMargin)
        assertEquals("a bottom banner ignores the top inset", dp(10f), bottomLp.topMargin)
        assertEquals(dp(10f) + 44, bottomLp.bottomMargin)
    }

    // imageUrl = "" builds the icon while InAppImageLoader.load("") returns before Glide, so the
    // unit test never touches the network stack (same trick as ModalInAppViewTest).
    @Test
    fun iconIsFiftyTwoDpAndClipped() {
        val icon = card(BannerInAppView(activity, geometryContent(imageUrl = ""))).getChildAt(0) as ImageView
        val lp = icon.layoutParams as LinearLayout.LayoutParams

        assertEquals(dp(52f), lp.width)
        assertEquals(dp(52f), lp.height)
        assertTrue("iOS clips the icon itself; a rounded background would paint behind it", icon.clipToOutline)
    }

    // Outline.getRadius() is API 24; this module's Robolectric default is SDK 21.
    @Test
    @Config(sdk = [34])
    fun iconCornerRadiusMatchesIos() {
        val icon = card(BannerInAppView(activity, geometryContent(imageUrl = ""))).getChildAt(0) as ImageView
        icon.layout(0, 0, dp(52f), dp(52f))

        val outline = Outline()
        icon.outlineProvider!!.getOutline(icon, outline)

        assertEquals(dp(12f).toFloat(), outline.radius, 0.5f)
    }

    // B7: visually the iOS glyph (bare, 60% white), but in a 48dp box — Android's minimum touch
    // target, which the other templates already got. 48 = iOS's 12pt row spacing + 24pt glyph +
    // 12pt, so the box replaces the old leading margin instead of adding to it, and the trailing
    // 12pt — slack iOS has no room for — overhangs the card's padding.
    @Test
    fun closeButtonIsBareGlyphInFortyEightDpBox() {
        val view = BannerInAppView(activity, geometryContent())
        val close = findCloseButton(view)!!
        val lp = close.layoutParams as LinearLayout.LayoutParams

        assertEquals(dp(InAppViewUtils.CLOSE_BUTTON_SIZE_DP), lp.width)
        assertEquals(dp(InAppViewUtils.CLOSE_BUTTON_SIZE_DP), lp.height)
        assertEquals("the box's own leading 12dp stands in for iOS's row spacing", 0, lp.marginStart)
        assertNull("the banner ✕ has no chip behind it", close.background)

        // Glyph centre = card's 14dp padding + this margin + half the box. iOS puts it 26pt from
        // the card edge (24pt button flush against a trailing inset of 14), so the box has to eat
        // 12dp of the padding — without this the glyph drifts 12dp inwards.
        val glyphCentreFromEdge = dp(14f) + lp.marginEnd + dp(InAppViewUtils.CLOSE_BUTTON_SIZE_DP) / 2
        assertEquals("the ✕ must land where iOS puts it", dp(26f), glyphCentreFromEdge)
    }

    // Both inline margins are logical, so RTL mirrors them. A physical rightMargin resolves to the
    // same edge in either direction: the ✕'s overhang would reach into the text column instead of
    // out of the card, and the ✕ — last child, so first in hit-testing — would eat taps meant for
    // the message.
    @Test
    fun inlineMarginsMirrorInRtl() {
        val view = BannerInAppView(activity, geometryContent(imageUrl = ""))
        val iconLp = (card(view).getChildAt(0).layoutParams as LinearLayout.LayoutParams)
            .apply { resolveLayoutDirection(View.LAYOUT_DIRECTION_RTL) }
        val closeLp = (findCloseButton(view)!!.layoutParams as LinearLayout.LayoutParams)
            .apply { resolveLayoutDirection(View.LAYOUT_DIRECTION_RTL) }

        assertEquals("the icon↔text gap moves to the icon's left", dp(12f), iconLp.leftMargin)
        assertEquals(0, iconLp.rightMargin)
        assertEquals("the overhang follows the trailing edge", -dp(12f), closeLp.leftMargin)
        assertEquals(0, closeLp.rightMargin)
    }
}
