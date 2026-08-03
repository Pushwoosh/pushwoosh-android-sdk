package com.pushwoosh.inapp.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.Insets
import com.pushwoosh.inapp.ui.animation.InAppAnimations
import com.pushwoosh.inapp.ui.animation.ReduceMotionUtil
import com.pushwoosh.inapp.ui.image.InAppImageLoader
import com.pushwoosh.inapp.ui.model.BannerContent
import com.pushwoosh.inapp.ui.model.BannerPosition
import kotlin.math.abs

/** True when a fling toward the banner's own edge should dismiss it: vertically dominant,
 *  and up for a TOP banner / down for a BOTTOM banner. */
internal fun shouldCloseOnSwipe(position: BannerPosition, velocityX: Float, velocityY: Float): Boolean {
    if (abs(velocityY) <= abs(velocityX)) return false
    return when (position) {
        BannerPosition.TOP -> velocityY < 0
        BannerPosition.BOTTOM -> velocityY > 0
    }
}

/** Compact non-blocking bar pinned to the top or bottom edge; slides in, can auto-dismiss.
 *  The view wraps its content height so touches outside the bar reach the host app. */
@SuppressLint("ClickableViewAccessibility")
internal class BannerInAppView(context: Context, private val content: BannerContent) : InAppTemplateView(context) {

    private val reduceMotion = ReduceMotionUtil.isReduceMotionEnabled(context)
    private val card: LinearLayout
    // Two numbers, not one: iOS pins the bar 10 from the safe area and 12 from the container sides.
    private val horizontalMargin = dp(12f)
    private val verticalMargin = dp(10f)
    private var autoDismiss: Runnable? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (shouldCloseOnSwipe(content.position, velocityX, velocityY)) {
                requestClose()
                return true
            }
            return false
        }
    })

    init {
        card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = InAppViewUtils.roundedBackground(content.backgroundColor, dp(18f).toFloat())
            elevation = dp(6f).toFloat()
            // Clickable so the card is a touch target: without it a no-action banner's card
            // isn't clickable, GestureDetector never receives MOVE/UP, and swipe-to-close dies.
            isClickable = true
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
        }
        addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            leftMargin = horizontalMargin
            rightMargin = horizontalMargin
            topMargin = verticalMargin
            bottomMargin = verticalMargin
        })

        content.imageUrl?.let { url ->
            val icon = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            val s = dp(52f)
            card.addView(icon, LinearLayout.LayoutParams(s, s).apply { marginEnd = dp(12f) })
            InAppViewUtils.clipToRoundedCorners(icon, dp(12f).toFloat())
            InAppImageLoader.load(url, icon)
        }

        val textColumn = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        card.addView(textColumn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        content.title?.let {
            textColumn.addView(InAppViewUtils.makeText(context, it.text, it.color, 16f, true, maxLines = 1))
        }
        content.message?.let {
            val message = InAppViewUtils.makeText(context, it.text, it.color, 13f, false, maxLines = 2)
            // iOS gets this from UIStackView.spacing, which applies between items — so no gap when
            // the message is the only line.
            val gap = if (content.title != null) dp(2f) else 0
            textColumn.addView(message, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = gap })
        }

        if (content.showCloseButton) {
            // The glyph sits centred in a 48dp touch box (Android's minimum) instead of iOS's bare
            // 24pt button. Its leading 12dp stands in for iOS's row spacing, but the trailing 12dp
            // is slack iOS does not have — its button ends flush against the row's trailing edge. So
            // the box overhangs the card's 14dp padding by 12dp, putting the glyph centre at
            // 14 − 12 + 24 = 26dp from the card's trailing edge, exactly iOS. The box stays inside
            // the card, so no touch area is lost. marginEnd, not rightMargin: a physical margin
            // would not mirror, sending the overhang into the text column in RTL.
            val size = dp(InAppViewUtils.CLOSE_BUTTON_SIZE_DP)
            card.addView(
                InAppViewUtils.makeInlineCloseButton(context) { requestClose() },
                LinearLayout.LayoutParams(size, size).apply { marginEnd = -dp(12f) }
            )
        }

        card.setOnClickListener { dispatchAction(content.action) }

        // A fling toward the edge dismisses; a plain tap returns false, falling through to the
        // card's own tap handling (the action click listener above, when present).
        card.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
    }

    override fun onInsetsApplied(insets: Insets) {
        (card.layoutParams as LayoutParams).apply {
            leftMargin = horizontalMargin + insets.left
            rightMargin = horizontalMargin + insets.right
            topMargin = verticalMargin + if (content.position == BannerPosition.TOP) insets.top else 0
            bottomMargin = verticalMargin + if (content.position == BannerPosition.BOTTOM) insets.bottom else 0
        }
        card.requestLayout()
    }

    private fun dp(value: Float) = InAppViewUtils.dp(context, value)

    private fun edgeOffset(): Float {
        val sign = if (content.position == BannerPosition.TOP) -1f else 1f
        return sign * dp(140f).toFloat()
    }

    private fun armAutoDismiss() {
        if (content.autoDismissMs <= 0) return
        autoDismiss = Runnable { requestClose() }.also { postDelayed(it, content.autoDismissMs) }
    }

    private fun cancelAutoDismiss() {
        autoDismiss?.let { removeCallbacks(it) }
        autoDismiss = null
    }

    override fun onDetachedFromWindow() {
        // Before super: removeCallbacks still reaches the handler that holds the message, because
        // the framework clears the view's attach info only after this returns. Covers the paths
        // with no dismiss at all — host Activity destroyed, configuration change.
        cancelAutoDismiss()
        super.onDetachedFromWindow()
    }

    override fun animateIn() = InAppAnimations.slideIn(card, edgeOffset(), reduceMotion) { armAutoDismiss() }

    override fun animateOut(onEnd: () -> Unit) {
        cancelAutoDismiss()
        InAppAnimations.slideOut(card, edgeOffset(), reduceMotion, onEnd)
    }
}
