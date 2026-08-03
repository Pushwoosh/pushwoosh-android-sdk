package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppButton
import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.InAppText
import com.pushwoosh.inapp.ui.model.SheetContent
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
class SheetInAppViewTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    private fun content(
        backgroundColor: Int = Color.WHITE,
        title: InAppText? = InAppText("Title", Color.BLACK),
        message: InAppText? = InAppText("Body", Color.BLACK),
        imageUrl: String? = null,
        showCloseButton: Boolean = true,
        buttons: List<InAppButton> = emptyList(),
        dimsBackground: Boolean = false
    ) = SheetContent(backgroundColor, title, message, imageUrl, showCloseButton, buttons, dimsBackground)

    private fun button(action: InAppAction = InAppAction.Close) = InAppButton(
        text = InAppText("OK", Color.WHITE),
        backgroundColor = Color.BLUE,
        borderColor = Color.BLUE,
        cornerRadiusDp = 8f,
        action = action
    )

    private fun sheet(content: SheetContent = content()) = SheetInAppView(activity, content)

    /** The card is the last root child: the optional dim goes in first. */
    private fun card(view: SheetInAppView) = view.getChildAt(view.childCount - 1) as FrameLayout

    private fun scroll(view: SheetInAppView) = card(view).getChildAt(0) as ScrollView

    private fun column(view: SheetInAppView) = scroll(view).getChildAt(0) as LinearLayout

    private fun grabber(view: SheetInAppView) = column(view).getChildAt(0)

    /** The cover is the column's only ImageView. */
    private fun cover(view: SheetInAppView): ImageView {
        val col = column(view)
        for (i in 0 until col.childCount) {
            val child = col.getChildAt(i)
            if (child is ImageView) return child
        }
        throw AssertionError("the sheet built no cover ImageView")
    }

    private fun dp(value: Float) = InAppViewUtils.dp(activity, value)

    private fun sp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, activity.resources.displayMetrics)

    private fun topMarginOf(child: View) = (child.layoutParams as LinearLayout.LayoutParams).topMargin

    private fun countCloses(view: SheetInAppView): IntArray {
        val closes = intArrayOf(0)
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {}

            override fun onClose() {
                closes[0]++
            }
        }
        return closes
    }

    private fun layout(view: SheetInAppView, width: Int, height: Int) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, width, height)
    }

    // GradientDrawable.getCornerRadii() is API 24; this module's Robolectric default is SDK 21.
    @Test
    @Config(sdk = [34])
    fun cardIsPinnedToTheBottomAndRoundedOnTopOnly() {
        val view = sheet()
        val lp = card(view).layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.BOTTOM, lp.gravity)
        assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, lp.width)
        assertEquals(FrameLayout.LayoutParams.WRAP_CONTENT, lp.height)

        val r = dp(28f).toFloat()
        assertArrayEquals(
            "only the top corners are rounded — the card sits flush on the bottom edge",
            floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f),
            (card(view).background as GradientDrawable).cornerRadii,
            0.5f
        )
        assertTrue("iOS draws a shadow under the sheet", card(view).elevation > 0f)
    }

    // Same tap-through trap as the modal: an unclickable card lets a body tap fall to the next
    // sibling under the touch point — the dim's tap-to-dismiss (the sheet closes itself) or, on a
    // floating sheet, the app UI underneath.
    @Test
    fun cardSwallowsBodyTaps() {
        assertTrue("card over the dim must swallow taps", card(sheet(content(dimsBackground = true))).isClickable)
        assertTrue("floating card must not leak taps into the app", card(sheet()).isClickable)
    }

    @Test
    fun grabberIsAlwaysShownOnTheCard() {
        val view = sheet()
        val lp = grabber(view).layoutParams as LinearLayout.LayoutParams

        assertEquals(dp(36f), lp.width)
        assertEquals(dp(5f), lp.height)
        assertEquals(Gravity.CENTER_HORIZONTAL, lp.gravity)
        assertEquals(dp(8f), lp.topMargin)
    }

    // iOS paints the grabber in tertiaryLabel, which follows the *device* theme: a light sheet in
    // dark mode loses its handle. The handle is part of the template's contract, so it takes its
    // contrast from the card colour instead.
    @Test
    fun grabberContrastsWithTheCardSurface() {
        val onLight = grabber(sheet(content(backgroundColor = Color.WHITE))).background
        assertEquals(Color.parseColor("#4D000000"), Shadow.extract<ShadowGradientDrawable>(onLight).lastSetColor)

        val onDark = grabber(sheet(content(backgroundColor = Color.parseColor("#263238")))).background
        assertEquals(Color.parseColor("#4DFFFFFF"), Shadow.extract<ShadowGradientDrawable>(onDark).lastSetColor)
    }

    @Test
    fun dimIsBuiltOnlyWhenRequestedAndClosesOnTap() {
        val floating = sheet()
        assertEquals("a floating sheet has no backdrop at all", 1, floating.childCount)

        val dimmed = sheet(content(dimsBackground = true))
        assertEquals(2, dimmed.childCount)
        val closes = countCloses(dimmed)
        dimmed.getChildAt(0).performClick()
        assertEquals("tapping the dim must close the sheet", 1, closes[0])
    }

    // No forced show: a downward fling always dismisses the sheet, so the ✕ stays opt-in — the
    // banner's rule, and iOS's for this template. The empty-buttons case is the one that tells the
    // sheet apart from the modal, which force-shows its ✕ exactly there.
    @Test
    fun closeButtonFollowsShowCloseButtonFlag() {
        assertNotNull(findCloseButton(sheet(content(showCloseButton = true))))
        assertNull(
            "buttons already close the sheet, ✕ obeys showCloseButton=false",
            findCloseButton(sheet(content(showCloseButton = false, buttons = listOf(button()))))
        )
        assertNull(
            "swipe-down is a guaranteed dismiss path — no forced ✕ even without buttons",
            findCloseButton(sheet(content(showCloseButton = false, buttons = emptyList())))
        )
    }

    @Test
    fun closeButtonRidesTheCardAndCloses() {
        val view = sheet(content(showCloseButton = true))
        val close = findCloseButton(view)!!
        assertSame("the ✕ is anchored to the card, not to the screen root", card(view), close.parent)

        val lp = close.layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.TOP or Gravity.END, lp.gravity)
        assertEquals(dp(9f), lp.topMargin)
        assertEquals(dp(9f), lp.marginEnd)

        val closes = countCloses(view)
        close.performClick()
        assertEquals(1, closes[0])
    }

    @Test
    fun downwardFlingClosesTheSheet() {
        assertTrue(shouldCloseSheetOnFling(0f, 1200f))
        assertFalse("a sheet does not leave upwards", shouldCloseSheetOnFling(0f, -1200f))
        assertFalse("a horizontally dominant fling is not a dismiss", shouldCloseSheetOnFling(2000f, 300f))
    }

    // The proportion is asserted on the cover's own measure pass, not via displayMetrics:
    // InAppOverlayActivity declares configChanges="orientation|…", so the view is never rebuilt and
    // a height computed once would be left over from the previous orientation. A fixed proportion
    // also holds the frame for a broken image — the placeholder has no intrinsic size.
    //
    // imageUrl = "" builds the cover while InAppImageLoader.load("") returns before Glide, so the
    // test never touches the network stack.
    @Test
    fun coverKeepsIosProportionOnEveryMeasure() {
        val image = cover(sheet(content(imageUrl = "")))

        image.measure(
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        assertEquals(600, image.measuredWidth)
        assertEquals(312, image.measuredHeight)

        image.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        assertEquals("the proportion is recomputed, not cached from the first pass", 520, image.measuredHeight)
    }

    // A landscape phone is wider than it is tall, so `width × 0.52` alone is taller than the card
    // the cover lives in — and everything under it would be laid out past the card's bottom edge,
    // where nothing draws it and nothing can tap it. iOS is spared by its 480pt width cap, which we
    // deliberately do not have, so the cover clamps against the height it was offered instead.
    @Test
    fun coverClampsAgainstTheHeightItWasOffered() {
        val image = cover(sheet(content(imageUrl = "")))

        image.measure(
            View.MeasureSpec.makeMeasureSpec(2372, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1055, View.MeasureSpec.AT_MOST)
        )

        assertEquals("the proportion alone would ask for ${(2372 * 0.52f).toInt()}", 422, image.measuredHeight)
        assertEquals(2372, image.measuredWidth)
    }

    // A real 420dpi phone in landscape (2400×1080 = 914×411dp) with an ordinary amount of copy:
    // the column overflows the viewport, and without a scroll container the tail children pay the
    // difference — the last CTA gets measured against what is left (a sliver, or nothing) while its
    // siblings keep their natural height. At the default density 1.0 everything fits and this test
    // cannot fail, which is how the squash shipped. Visibility is forced because the module's view
    // tests pass a blank url to keep Glide out, and a blank url is exactly what InAppImageLoader hides.
    /** Copy that overflows a 2400×1080 landscape viewport at 420dpi. Explicit line breaks:
     *  Robolectric's text shadow does not soft-wrap, so a long single-line paragraph would
     *  measure one line tall and never overflow. */
    private fun overflowingContent() = content(
        title = InAppText("Ready for a weekend getaway?", Color.BLACK),
        message = InAppText(
            "Book any stay of two nights or more before Sunday and we will add:\n" +
                "— a free breakfast for two, every morning of your stay\n" +
                "— late checkout on the day you leave\n" +
                "— a spa voucher for any participating hotel\n" +
                "— double loyalty points, landing in your wallet at checkout\n" +
                "The offer is valid while availability lasts — tap below to see the full list " +
                "of destinations and lock in your dates before the weekend crowd does.",
            Color.BLACK
        ),
        imageUrl = "",
        buttons = listOf(button(), button(), button())
    )

    @Test
    @Config(sdk = [34], qualifiers = "land-420dpi")
    fun landscapeSheetKeepsItsLastCtaTappable() {
        val view = sheet(overflowingContent())
        cover(view).visibility = View.VISIBLE

        layout(view, 2400, 1080)

        val col = column(view)
        val first = col.getChildAt(col.childCount - 2)
        val last = col.getChildAt(col.childCount - 1)
        assertEquals("the last CTA must keep its natural height", first.height, last.height)
        assertTrue(
            "the fixture must overflow the viewport, or this test proves nothing (col=${col.height})",
            col.height > 1080
        )
    }

    // A gesture that begins over scrolled-down content is the user paging back toward the top,
    // not a dismiss — only a gesture that starts at the top may close the sheet. The state is
    // sampled at DOWN: by fling time the drag itself has often scrolled the content back to 0.
    @Test
    @Config(sdk = [34], qualifiers = "land-420dpi")
    fun downwardFlingWhileScrolledDownDoesNotDismiss() {
        val view = sheet(overflowingContent())
        cover(view).visibility = View.VISIBLE
        val closes = countCloses(view)
        layout(view, 2400, 1080)

        scroll(view).scrollY = 200
        swipe(card(view), fromY = 100f, toY = 700f)
        assertEquals("a fling over scrolled-down content must page, not dismiss", 0, closes[0])

        scroll(view).scrollY = 0
        swipe(card(view), fromY = 100f, toY = 700f)
        assertEquals("back at the top the same fling dismisses again", 1, closes[0])
    }

    @Test
    fun coverIsInsetFromTheCardAndClipsItself() {
        val view = sheet(content(imageUrl = ""))
        val image = cover(view)
        val lp = image.layoutParams as LinearLayout.LayoutParams

        assertEquals(dp(14f), lp.marginStart)
        assertEquals(dp(14f), lp.marginEnd)
        assertEquals(dp(12f), lp.topMargin)
        assertEquals(ImageView.ScaleType.CENTER_CROP, image.scaleType)
        assertTrue("iOS clips the picture itself; a rounded background would paint behind it", image.clipToOutline)
    }

    @Test
    fun titleAndMessageUseIosTypeScaleAndInsets() {
        val view = sheet()
        val title = column(view).getChildAt(1) as TextView
        val message = column(view).getChildAt(2) as TextView

        assertEquals(sp(22f), title.textSize, 0.5f)
        assertEquals(sp(15f), message.textSize, 0.5f)
        assertNull("iOS sets lines = 0: the sheet's text is never clamped", message.ellipsize)
        assertEquals(dp(24f), (title.layoutParams as LinearLayout.LayoutParams).marginStart)
        assertEquals(dp(24f), (message.layoutParams as LinearLayout.LayoutParams).marginEnd)
    }

    // iOS's stack puts 14 between blocks and the text block carries its own 2pt inset; with no
    // cover above, the text starts 8 under the grabber instead.
    @Test
    fun firstTextBlockClearsTheCoverOrTheGrabber() {
        val withCover = sheet(content(imageUrl = ""))
        assertEquals(dp(16f), topMarginOf(column(withCover).getChildAt(2)))

        val bare = sheet()
        assertEquals(dp(8f), topMarginOf(column(bare).getChildAt(1)))
    }

    // iOS gets the 6 from UIStackView.spacing, which applies *between* items: a message with no
    // title above it starts the text block instead of being pushed down by a gap.
    @Test
    fun messageGapAppliesOnlyBelowTitle() {
        val both = sheet()
        assertEquals(dp(6f), topMarginOf(column(both).getChildAt(2)))

        val messageOnly = sheet(content(title = null))
        assertEquals("nothing above the message means no gap", dp(8f), topMarginOf(column(messageOnly).getChildAt(1)))
    }

    @Test
    fun buttonColumnUsesIosInsetsAndSpacing() {
        val view = sheet(content(buttons = listOf(button(), button())))
        val first = column(view).getChildAt(3)
        val second = column(view).getChildAt(4)

        assertEquals("14 of stack gap + 10 of the button block's own inset", dp(24f), topMarginOf(first))
        assertEquals(dp(10f), topMarginOf(second))
        assertEquals(dp(20f), (first.layoutParams as LinearLayout.LayoutParams).marginStart)
        assertEquals(dp(20f), (first.layoutParams as LinearLayout.LayoutParams).marginEnd)
    }

    // With nothing above them the buttons keep only their own 10 — the stack gap has no block to
    // separate them from.
    @Test
    fun buttonsOnlySheetDropsTheStackGap() {
        val view = sheet(content(title = null, message = null, buttons = listOf(button())))
        assertEquals(dp(10f), topMarginOf(column(view).getChildAt(1)))
    }

    @Test
    fun buttonDispatchesItsOwnAction() {
        val actions = mutableListOf<InAppAction>()
        val view = sheet(content(buttons = listOf(button(InAppAction.Url("https://a.example")))))
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {
                actions.add(action)
            }

            override fun onClose() {}
        }

        column(view).getChildAt(3).performClick()

        assertEquals(listOf<InAppAction>(InAppAction.Url("https://a.example")), actions)
    }

    @Test
    fun columnKeepsTheBottomInset() {
        assertEquals(dp(16f), column(sheet()).paddingBottom)
    }

    // SDK 34, not this module's Robolectric default of 21: on 21 WindowInsetsCompat reaches the
    // listener with its bottom inset zeroed, which would pass this test for the wrong reason.
    @Test
    @Config(sdk = [34])
    fun insetsPadTheColumnSidesAndBottom() {
        val view = sheet()
        activity.setContentView(view)

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(11, 22, 33, 44))
                .build()
        )

        val col = column(view)
        assertEquals(11, col.paddingLeft)
        assertEquals(33, col.paddingRight)
        assertEquals(dp(16f) + 44, col.paddingBottom)
        assertEquals("the card is pinned to the bottom — the status-bar inset is not its business", 0, col.paddingTop)
    }

    // The card is edge-to-edge, so unlike the banner's and the modal's the sheet's ✕ has no side
    // margin of its own to hide behind: a landscape nav bar or cutout on the END edge would sit on
    // top of it and eat the tap. Same fix (and the same RTL nuance) as FullscreenInAppView.
    @Test
    @Config(sdk = [34])
    fun insetsMoveTheCloseButtonClearOfTheSystemBars() {
        val view = sheet(content(showCloseButton = true))
        activity.setContentView(view)

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(11, 22, 33, 44))
                .build()
        )

        val close = findCloseButton(view)!!
        val lp = close.layoutParams as FrameLayout.LayoutParams
        assertEquals(dp(9f) + 33, lp.marginEnd)
        assertEquals("the ✕ rides the card, which never reaches the status bar", dp(9f), lp.topMargin)

        // The laid-out position, not just the field: `marginEnd` only re-arms the relative margin for
        // resolution, and nothing re-resolves it on a plain layout pass — asserting the field alone
        // passes while the button stays where it resolved at attach time (that regression was caught
        // on device, with the ✕ unreachable under a landscape cutout inset).
        layout(view, 1080, 1920)
        assertEquals(
            "the ✕ must actually sit inset from the card's END edge",
            card(view).right - dp(9f) - 33, close.right
        )
    }

    // The ✕ inset picks its physical side from layoutDirection, but the direction resolves only at
    // the first measure — *after* the first insets pass. An RTL host would keep the LTR-resolved
    // side (END = insets.right) until something re-applies the insets; the view must do that
    // itself when the resolution lands.
    @Test
    @Config(sdk = [34])
    fun closeButtonInsetFollowsAnRtlResolution() {
        // Without FLAG_SUPPORTS_RTL every resolution clamps to LTR, whatever layoutDirection says
        // — the library's test manifest carries no supportsRtl, real RTL hosts do.
        activity.applicationInfo.flags = activity.applicationInfo.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
        val view = sheet(content(showCloseButton = true))
        activity.setContentView(view)
        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(33, 0, 11, 0))
                .build()
        )

        view.layoutDirection = View.LAYOUT_DIRECTION_RTL
        layout(view, 1080, 1920)

        val lp = findCloseButton(view)!!.layoutParams as FrameLayout.LayoutParams
        assertEquals("in RTL the END edge is insets.left", dp(9f) + 33, lp.marginEnd)
    }

    // A tall sheet — landscape, or simply a lot of content — must stop at the status bar instead
    // of growing under it: the rounded corners and the grabber stay on visible surface, the
    // overflow scrolls, and the ✕ needs no rescue nudge because it rides a card that can never
    // reach the bar.
    @Test
    @Config(sdk = [34])
    fun cardStopsAtTheStatusBarInsteadOfGrowingUnderIt() {
        val view = sheet(content(imageUrl = "", buttons = listOf(button(), button())))
        cover(view).visibility = View.VISIBLE
        activity.setContentView(view)
        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 130, 0, 0))
                .build()
        )
        val close = findCloseButton(view)!!

        layout(view, 1080, 1920)
        assertTrue("sanity: in a tall window the card floats far below the bar", card(view).top > 130)

        // A window shorter than the content: the card is bottom-pinned and would grow under the
        // inset — it must stop at it instead.
        layout(view, 1080, 400)
        assertTrue(
            "the card must stop at the status bar, not grow under it (top=${card(view).top})",
            card(view).top >= 130
        )
        assertEquals("a card that cannot reach the bar needs no ✕ nudge", 0f, close.translationY, 0.5f)
    }

    // The slide offset is the card's own height, which is 0 until the first layout pass — so the
    // entrance has to wait for it. It must not wait *visibly*: alpha drops to 0 inside animateIn(),
    // or the card shows up in its final position for a frame before jumping down to slide in.
    //
    // The view is deliberately *not* attached here: Robolectric's attach path makes no commitment
    // about a layout pass, so asserting "not laid out yet" against it would be asserting an
    // implementation detail of the harness. A detached view is unambiguously unlaid, and slideIn
    // sets its start values synchronously, which is all this test reads.
    @Test
    fun entranceHidesTheCardUntilItKnowsItsHeight() {
        val view = sheet(content(buttons = listOf(button())))
        val scheduler = Robolectric.getForegroundThreadScheduler()
        scheduler.pause()

        view.animateIn()
        assertEquals("the card must be hidden the moment animateIn runs", 0f, card(view).alpha, 0.001f)
        assertEquals("nothing can slide before the height is known", 0f, card(view).translationY, 0.001f)

        layout(view, 1080, 1920)
        assertEquals(
            "the card enters from its own height plus iOS's 40",
            (card(view).height + dp(40f)).toFloat(), card(view).translationY, 0.5f
        )
        scheduler.unPause()
    }

    @Test
    fun entranceSlidesAtOnceWhenTheCardIsAlreadyLaidOut() {
        val view = sheet(content(buttons = listOf(button())))
        layout(view, 1080, 1920)
        val scheduler = Robolectric.getForegroundThreadScheduler()
        scheduler.pause()

        view.animateIn()

        assertEquals(
            "an already-measured card slides without waiting for another layout pass",
            (card(view).height + dp(40f)).toFloat(), card(view).translationY, 0.5f
        )
        scheduler.unPause()
    }

    // A dismiss can land before the first layout pass (a delegate closing from willPresent, a host
    // Activity torn down mid-show). The armed entrance must not fire on that layout: it would
    // restart the slide on the same ViewPropertyAnimator and cancel the exit — and a cancelled
    // slideOut still runs its onEnd, so the sheet would vanish without its exit animation.
    @Test
    fun dismissBeforeTheFirstLayoutDisarmsTheEntrance() {
        val view = sheet(content(buttons = listOf(button())))
        val scheduler = Robolectric.getForegroundThreadScheduler()
        scheduler.pause()

        view.animateIn()
        view.animateOut {}
        layout(view, 1080, 1920)

        assertEquals(
            "the entrance fired on a layout that came after the dismiss",
            0f, card(view).translationY, 0.001f
        )
        scheduler.unPause()
    }

    // The exit rides the card, not the root: a root-level animation would fade the dim a second
    // time. That onEnd fires exactly once is an e2e check — a child view's exit animation never
    // runs its frames under Robolectric (see carousel-parity-tail.md).
    @Test
    fun exitLeavesTheRootAlphaAlone() {
        val view = sheet(content(dimsBackground = true))
        activity.setContentView(view)
        layout(view, 1080, 1920)

        view.animateOut {}

        assertEquals(1f, view.alpha, 0.001f)
    }

    // The dimmed sheet travels through InAppOverlayActivity, which builds its view via the factory:
    // a missing branch there would silently finish the Activity and drop the message.
    @Test
    fun factoryBuildsTheSheetForTheActivityPath() {
        val built = InAppViewFactory.create(
            activity,
            InAppLayout.Sheet(content(dimsBackground = true)),
            object : InAppTemplateView.Listener {
                override fun onAction(action: InAppAction) {}

                override fun onClose() {}
            }
        )

        assertTrue("the blocking sheet is built by the Activity path", built is SheetInAppView)
    }

    /** A real fling through the card's own touch handling: DOWN, two MOVEs past the touch slop, UP.
     *  GestureDetector derives the velocity from the event times, so they have to be spaced out. */
    private fun swipe(target: View, fromY: Float, toY: Float, x: Float = 500f) {
        listOf(
            MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, x, fromY, 0),
            MotionEvent.obtain(0L, 50L, MotionEvent.ACTION_MOVE, x, (fromY + toY) / 2f, 0),
            MotionEvent.obtain(0L, 100L, MotionEvent.ACTION_MOVE, x, toY, 0),
            MotionEvent.obtain(0L, 100L, MotionEvent.ACTION_UP, x, toY, 0)
        ).forEach {
            target.dispatchTouchEvent(it)
            it.recycle()
        }
    }

    // shouldCloseSheetOnFling is checked as a pure function above; this is the wiring around it —
    // the GestureDetector sitting on the card. Dropping setOnTouchListener leaves every structural
    // assertion in this file green while the sheet loses the dismiss path that lets its ✕ stay
    // opt-in (showCloseButton=false here: without the swipe there would be no way out at all).
    @Test
    fun downwardSwipeOnTheCardActuallyDismissesTheSheet() {
        val view = sheet(content(showCloseButton = false, buttons = listOf(button())))
        val closes = countCloses(view)

        swipe(card(view), fromY = 100f, toY = 700f)

        assertEquals("a downward fling on the card must reach requestClose", 1, closes[0])
    }

    // A clickable child consumes the touch stream it starts, so a listener on the card never sees
    // a swipe that begins on a CTA button or the ✕ — and the sheet's surface is mostly buttons.
    // The guaranteed dismiss path (what lets the ✕ stay opt-in) must survive that: the gesture has
    // to be read at dispatch level, before the children eat the events.
    @Test
    fun downwardSwipeStartingOnACtaButtonStillDismissesTheSheet() {
        val view = sheet(
            content(showCloseButton = false, buttons = listOf(button(InAppAction.Url("https://a.example"))))
        )
        val closes = countCloses(view)
        layout(view, 1080, 1920)

        val col = column(view)
        val btn = col.getChildAt(col.childCount - 1)
        val r = Rect(0, 0, btn.width, btn.height)
        card(view).offsetDescendantRectToMyCoords(btn, r)
        swipe(card(view), fromY = r.exactCenterY(), toY = r.exactCenterY() + 600f, x = r.exactCenterX())

        assertEquals("a fling that starts on a button must still reach requestClose", 1, closes[0])
    }

    @Test
    fun upwardSwipeOnTheCardKeepsTheSheetOnScreen() {
        val view = sheet(content(showCloseButton = false, buttons = listOf(button())))
        val closes = countCloses(view)

        swipe(card(view), fromY = 700f, toY = 100f)

        assertEquals("a sheet does not leave upwards", 0, closes[0])
    }

    // The cover counts as a block above the buttons: an image with no text still owes the first
    // button the stack gap. Seeding hasBlockAbove from hasCover is the easy half of that flag to
    // lose, and the buttons would then hug the picture.
    @Test
    fun coverAloneStillSeparatesTheFirstButton() {
        val view = sheet(content(title = null, message = null, imageUrl = "", buttons = listOf(button())))

        assertEquals(
            "14 of stack gap + 10 of the button block's own inset",
            dp(24f), topMarginOf(column(view).getChildAt(2))
        )
    }

    // The layout listener that waits for the card's height is one-shot. Left registered, every
    // later layout pass — insets applied, keyboard, rotation — would restart the entrance and throw
    // the settled card back off the bottom edge. The second pass is given a different height so it
    // is a real one (the card's frame moves), or the assertion would hold for want of any pass.
    @Test
    fun entranceRunsOnceAndNotOnEveryLayoutPass() {
        val view = sheet(content(buttons = listOf(button())))
        val scheduler = Robolectric.getForegroundThreadScheduler()
        scheduler.pause()

        view.animateIn()
        layout(view, 1080, 1920)
        // Where the entrance leaves the card once its animation has run.
        card(view).translationY = 0f
        card(view).alpha = 1f

        layout(view, 1080, 1500)

        assertEquals("a later layout pass must not re-arm the entrance", 0f, card(view).translationY, 0.001f)
        assertEquals("nor hide the settled card again", 1f, card(view).alpha, 0.001f)
        scheduler.unPause()
    }

    // performClick() fires on a view of any size, so the tap-to-dismiss check above says nothing
    // about the backdrop covering the screen — and a dim that covers nothing dims nothing.
    @Test
    fun dimCoversTheRootAtTheModalOpacity() {
        val dim = sheet(content(dimsBackground = true)).getChildAt(0)
        val lp = dim.layoutParams as FrameLayout.LayoutParams

        assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, lp.width)
        assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, lp.height)
        assertEquals(
            "the sheet dims like the modal (#99000000) — both templates carry it under dimBackground",
            Color.parseColor("#99000000"),
            (dim.background as ColorDrawable).color
        )
    }
}
