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
    private val baseMargin = dp(12f)

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (shouldCloseOnSwipe(content.position, velocityX, velocityY)) {
                requestClose()
                return true
            }
            return false
        }
    })

    val position: BannerPosition get() = content.position
    val autoDismissMs: Long get() = content.autoDismissMs

    init {
        card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = InAppViewUtils.roundedBackground(content.backgroundColor, dp(12f).toFloat())
            elevation = dp(6f).toFloat()
            // Clickable so the card is a touch target: without it a no-action banner's card
            // isn't clickable, GestureDetector never receives MOVE/UP, and swipe-to-close dies.
            isClickable = true
            val p = dp(12f)
            setPadding(p, p, p, p)
        }
        addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            leftMargin = baseMargin
            rightMargin = baseMargin
            topMargin = baseMargin
            bottomMargin = baseMargin
        })

        content.imageUrl?.let { url ->
            val icon = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            val s = dp(40f)
            card.addView(icon, LinearLayout.LayoutParams(s, s).apply { rightMargin = dp(12f) })
            InAppImageLoader.load(url, icon)
        }

        val textColumn = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        card.addView(textColumn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        content.title?.let { textColumn.addView(InAppViewUtils.makeText(context, it.text, it.color, 15f, true, maxLines = 1)) }
        content.message?.let { textColumn.addView(InAppViewUtils.makeText(context, it.text, it.color, 13f, false, maxLines = 2)) }

        if (content.showCloseButton) {
            val size = dp(28f)
            card.addView(InAppViewUtils.makeCloseButton(context) { requestClose() }, LinearLayout.LayoutParams(size, size).apply { leftMargin = dp(8f) })
        }

        card.setOnClickListener { dispatchAction(content.action) }

        // A fling toward the edge dismisses; a plain tap returns false, falling through to the
        // card's own tap handling (the action click listener above, when present).
        card.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
    }

    override fun onInsetsApplied(insets: Insets) {
        (card.layoutParams as LayoutParams).apply {
            leftMargin = baseMargin + insets.left
            rightMargin = baseMargin + insets.right
            topMargin = baseMargin + if (content.position == BannerPosition.TOP) insets.top else 0
            bottomMargin = baseMargin + if (content.position == BannerPosition.BOTTOM) insets.bottom else 0
        }
        card.requestLayout()
    }

    private fun dp(value: Float) = InAppViewUtils.dp(context, value)

    private fun edgeOffset(): Float {
        val sign = if (content.position == BannerPosition.TOP) -1f else 1f
        return sign * dp(140f).toFloat()
    }

    override fun animateIn() = InAppAnimations.slideIn(card, edgeOffset(), reduceMotion)

    override fun animateOut(onEnd: () -> Unit) = InAppAnimations.slideOut(card, edgeOffset(), reduceMotion, onEnd)
}
