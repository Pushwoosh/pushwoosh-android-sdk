package com.pushwoosh.inapp.ui.view

import android.content.Context
import android.graphics.Color
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.graphics.Insets
import com.pushwoosh.inapp.ui.animation.InAppAnimations
import com.pushwoosh.inapp.ui.animation.ReduceMotionUtil
import com.pushwoosh.inapp.ui.image.InAppImageLoader
import com.pushwoosh.inapp.ui.model.SheetContent
import kotlin.math.abs
import kotlin.math.roundToInt

/** True when a fling should dismiss the sheet: vertically dominant and downward — the card is
 *  pinned to the bottom edge, so that is the way it leaves. iOS's point thresholds (90 / 900) are
 *  not ported: `GestureDetector` reports velocity in its own units. */
internal fun shouldCloseSheetOnFling(velocityX: Float, velocityY: Float): Boolean =
    velocityY > abs(velocityX)

/** iOS media geometry: the cover is `width × 0.52`. */
private const val COVER_ASPECT = 0.52f

/** …but never more than this share of the height the card was offered — see [SheetInAppView.CoverImage]. */
private const val COVER_MAX_HEIGHT_FRACTION = 0.4f

/** Bottom sheet: a card pinned to the bottom edge with a grabber, optional cover, left-aligned
 *  text and a column of buttons. Swipe down dismisses; `dimsBackground` adds a tap-to-dismiss
 *  dimmed backdrop. */
internal class SheetInAppView(context: Context, content: SheetContent) : InAppTemplateView(context) {

    private val reduceMotion = ReduceMotionUtil.isReduceMotionEnabled(context)
    private val bottomPad = dp(16f)
    private val closeMargin = dp(9f)
    private val card: FrameLayout
    private val scroll = ScrollView(context)
    private val column: LinearLayout
    private var closeButton: View? = null
    private var pendingEntrance: View.OnLayoutChangeListener? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        // A gesture that begins over scrolled-down content is the user paging back toward the
        // top, not a dismiss — even when the drag itself reaches the top before the fling fires.
        // Sampled at DOWN for exactly that reason: by fling time the ScrollView has already eaten
        // the drag and scrollY may be back at 0.
        private var startedAtTop = true

        override fun onDown(e: MotionEvent): Boolean {
            startedAtTop = scroll.scrollY == 0
            return false
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (startedAtTop && shouldCloseSheetOnFling(velocityX, velocityY)) {
                requestClose()
                return true
            }
            return false
        }
    })

    init {
        if (content.dimsBackground) {
            addView(View(context).apply {
                setBackgroundColor(Color.parseColor("#99000000"))
                setOnClickListener { requestClose() }
            }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }

        // The card is a frame so the ✕ can sit on the card itself (iOS anchors it to the content
        // host) and ride the card's enter/exit slide.
        //
        // The dismiss gesture is read here, in dispatchTouchEvent, and not from a touch listener:
        // a clickable child (a CTA button, the ✕) consumes the stream it starts, and a listener on
        // the card never sees it — a swipe beginning on a button would be dead, on a surface that
        // is mostly buttons. Dispatch sees every event regardless of who consumes it.
        card = object : FrameLayout(context) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(ev)
                return super.dispatchTouchEvent(ev)
            }
        }.apply {
            background = InAppViewUtils.topRoundedBackground(content.backgroundColor, dp(28f).toFloat())
            elevation = dp(8f).toFloat()
            // Clickable so the card swallows body taps: without it a tap on the text falls through
            // to the backdrop's tap-to-dismiss (the sheet closes itself), and on a floating sheet it
            // leaks into the app UI underneath.
            isClickable = true
        }
        addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        // The column scrolls: without this it is measured AT_MOST against the card's height and
        // overflowing copy squashes the tail children — on a landscape phone with ordinary copy
        // the last CTA came out a sliver, present but untappable. The ScrollView measures the
        // column unbounded, so every child keeps its natural height and the overflow scrolls.
        card.addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, bottomPad)
        }
        scroll.addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        column.addView(
            View(context).apply {
                background = InAppViewUtils.roundedBackground(grabberColor(content.backgroundColor), dp(2.5f).toFloat())
            },
            LinearLayout.LayoutParams(dp(36f), dp(5f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(8f)
            }
        )

        val hasCover = content.imageUrl != null
        content.imageUrl?.let { url ->
            val image = CoverImage(context)
            column.addView(image, row(topMargin = dp(12f), horizontalMargin = dp(14f)))
            InAppViewUtils.clipToRoundedCorners(image, dp(18f).toFloat())
            InAppImageLoader.load(url, image)
        }

        // iOS's stack puts 14 between blocks and the text block carries its own 2pt inset; with no
        // cover above, the text starts 8 under the grabber instead.
        val textTop = if (hasCover) dp(16f) else dp(8f)
        var hasBlockAbove = hasCover
        content.title?.let {
            column.addView(
                InAppViewUtils.makeText(context, it.text, it.color, 22f, true),
                row(topMargin = textTop, horizontalMargin = dp(24f))
            )
            hasBlockAbove = true
        }
        content.message?.let {
            // iOS gets the 6 from UIStackView.spacing, which applies between items: a message with
            // no title above it starts the text block instead of being pushed down by a gap.
            val top = if (content.title != null) dp(6f) else textTop
            column.addView(
                InAppViewUtils.makeText(context, it.text, it.color, 15f, false),
                row(topMargin = top, horizontalMargin = dp(24f))
            )
            hasBlockAbove = true
        }
        content.buttons.forEachIndexed { index, button ->
            // The first button pays the stack gap (14) on top of the block's own inset (10) only
            // when there is a block above it to be separated from.
            val top = if (index == 0 && hasBlockAbove) dp(24f) else dp(10f)
            column.addView(
                InAppViewUtils.makeButton(context, button) { dispatchAction(it) },
                row(topMargin = top, horizontalMargin = dp(20f))
            )
        }

        // Strictly by the flag, no forced show: a downward fling always dismisses the sheet, so the
        // ✕ is never the only way out (banner's rule, and iOS's for this template).
        if (content.showCloseButton) {
            val size = dp(InAppViewUtils.CLOSE_BUTTON_SIZE_DP)
            closeButton = InAppViewUtils.makeCardCloseButton(context) { requestClose() }.also {
                card.addView(it, LayoutParams(size, size, Gravity.TOP or Gravity.END).apply {
                    topMargin = closeMargin
                    marginEnd = closeMargin
                })
            }
        }
    }

    /** iOS uses `tertiaryLabel` here, which follows the *device* theme instead of the surface: on a
     *  white sheet in dark mode the grabber disappears. Contrast against the card colour instead. */
    private fun grabberColor(background: Int): Int {
        val luminance = (0.299f * Color.red(background) +
            0.587f * Color.green(background) +
            0.114f * Color.blue(background)) / 255f
        return if (luminance > 0.65f) Color.parseColor("#4D000000") else Color.parseColor("#4DFFFFFF")
    }

    private fun dp(value: Float) = InAppViewUtils.dp(context, value)

    private fun row(topMargin: Int, horizontalMargin: Int) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            this.topMargin = topMargin
            marginStart = horizontalMargin
            marginEnd = horizontalMargin
        }

    override fun onInsetsApplied(insets: Insets) {
        // iOS takes the same from safeAreaLayoutGuide. The top inset caps the card instead of
        // padding the column: a tall sheet — landscape, or a lot of content — is bottom-pinned and
        // would grow under the status bar, taking the rounded corners, the grabber and the ✕ with
        // it. With the margin the card can never reach the bar and the overflow scrolls.
        card.layoutParams = (card.layoutParams as LayoutParams).apply { topMargin = insets.top }
        column.setPadding(insets.left, 0, insets.right, bottomPad + insets.bottom)
        closeButton?.let {
            // The card is edge-to-edge, so the ✕ needs the side inset itself — otherwise a landscape
            // nav bar or cutout on the END edge sits on top of it and eats the tap. The top margin
            // stays at rest: the card never reaches the status bar (capped above).
            InAppViewUtils.applyCloseButtonInsets(it, layoutDirection, insets, closeMargin, closeMargin)
        }
    }

    // The ✕ inset picks its physical side from layoutDirection, but the direction resolves only at
    // the first measure — after the first insets pass — so an RTL host would keep the LTR side
    // until the next insets change. Re-apply when the resolution lands. The null check is not
    // showCloseButton in disguise: the base View constructor delivers a first resolution before
    // this class's fields exist, and the ✕ is the only side-aware consumer anyway.
    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)
        if (closeButton != null) onInsetsApplied(systemInsets)
    }

    private fun enterOffset() = (card.height + dp(40f)).toFloat()

    override fun animateIn() {
        // The card is attached but not laid out yet, so its height — and with it the slide offset —
        // is still 0. Drop alpha now (a card left visible would show up in its final position for a
        // frame), and start the slide from the first layout pass. A fixed offset like the banner's
        // 140dp would make a tall sheet slide in from mid-screen.
        card.alpha = 0f
        if (card.height > 0) {
            slideCardIn()
            return
        }
        pendingEntrance = object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View, left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
            ) {
                cancelPendingEntrance()
                slideCardIn()
            }
        }.also { card.addOnLayoutChangeListener(it) }
    }

    private fun slideCardIn() = InAppAnimations.slideIn(card, enterOffset(), reduceMotion) {}

    /** Disarms the deferred entrance. Kept out of the listener's own body only in name: it runs
     *  there too, so the entrance stays one-shot. */
    private fun cancelPendingEntrance() {
        pendingEntrance?.let { card.removeOnLayoutChangeListener(it) }
        pendingEntrance = null
    }

    override fun animateOut(onEnd: () -> Unit) {
        // A dismiss can arrive before the first layout pass (a delegate closing from willPresent, a
        // host Activity torn down mid-show). The armed entrance would then fire on that layout and
        // restart the slide on the same ViewPropertyAnimator, cancelling the exit — and a cancelled
        // slideOut still calls onEnd (it rides setListener, see InAppAnimations).
        cancelPendingEntrance()
        InAppAnimations.slideOut(card, enterOffset(), reduceMotion, onEnd)
    }

    /** The cover recomputes its proportion on every measure: `InAppOverlayActivity` declares
     *  `configChanges="orientation|…"`, so the view is never rebuilt and a height computed once
     *  from `displayMetrics` would be left over from the previous orientation. The fixed proportion
     *  also holds the frame on a broken image — the placeholder has no intrinsic size. */
    private class CoverImage(context: Context) : ImageView(context) {
        init {
            scaleType = ScaleType.CENTER_CROP
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            var height = (width * COVER_ASPECT).roundToInt()
            // The card fills the screen width, so on a landscape phone `width × 0.52` alone is
            // taller than the whole card: the copy and the CTA would live behind a screenful of
            // scrolling. iOS is spared this by its 480pt width cap, which we deliberately do not
            // have, so the clamp lives here — take at most a share of the height the card was
            // offered and leave the rest to the copy and the buttons. Same rule as the carousel's
            // landscape clamp (see carouselCardSize). Inside the ScrollView the offer arrives as
            // UNSPECIFIED with the remaining height as its size (API 23+), so the mode check is
            // "not EXACTLY" rather than AT_MOST; a zero size means "no constraint known", not
            // "no room", and must not clamp the cover away.
            val available = MeasureSpec.getSize(heightMeasureSpec)
            if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY && available > 0) {
                height = height.coerceAtMost((available * COVER_MAX_HEIGHT_FRACTION).roundToInt())
            }
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )
        }
    }
}
