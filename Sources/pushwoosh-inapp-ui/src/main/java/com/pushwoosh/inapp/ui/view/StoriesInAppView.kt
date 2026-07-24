package com.pushwoosh.inapp.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.SystemClock
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.Insets
import com.pushwoosh.inapp.ui.animation.InAppAnimations
import com.pushwoosh.inapp.ui.image.InAppImageLoader
import com.pushwoosh.inapp.ui.model.InAppText
import com.pushwoosh.inapp.ui.model.StoriesContent
import com.pushwoosh.inapp.ui.model.StoryItem
import kotlin.math.abs

/** Full-screen Instagram-style stories: segmented progress, tap-zones (prev/next),
 *  auto-advance, swipe-down to dismiss, a per-slide column of CTA buttons. */
internal class StoriesInAppView(context: Context, private val content: StoriesContent) : InAppTemplateView(context) {

    private val imageView = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
    private val progress = StoriesProgressView(context)
    private val titleView = InAppViewUtils.makeText(context, "", Color.WHITE, 20f, true, maxLines = 2)
    private val subtitleView = InAppViewUtils.makeText(context, "", Color.WHITE, 15f, false, maxLines = 2)
    private val topBar: LinearLayout
    private val bottomBar: LinearLayout
    private val gestureDetector: GestureDetector

    private val topPad = dp(12f)
    private val bottomPad = dp(20f)
    private var index = 0
    private var animator: ValueAnimator? = null
    private var advance: Runnable? = null
    private val buttonViews = mutableListOf<View>()
    private var paused = false
    private var remainingMs = 0L
    private var segmentStartUptime = 0L
    private var segmentDurationMs = 0L

    init {
        setBackgroundColor(Color.BLACK)
        addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        progress.setCount(content.items.size)
        topBar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(topPad, dp(12f), topPad, topPad)
        }
        topBar.addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3f)))
        addView(topBar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.TOP))

        bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(bottomPad, bottomPad, bottomPad, bottomPad)
        }
        bottomBar.addView(titleView)
        bottomBar.addView(subtitleView)
        addView(bottomBar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        if (content.showCloseButton) {
            // Row under the progress bar: the ✕ is positioned by the bar (iOS anchor) and the
            // topBar padding already carries the system insets.
            val size = dp(InAppViewUtils.CLOSE_BUTTON_SIZE_DP)
            val closeRow = FrameLayout(context)
            closeRow.addView(InAppViewUtils.makeCardCloseButton(context) { requestClose() },
                LayoutParams(size, size, Gravity.END))
            topBar.addView(closeRow, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(3f) })
        }

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (paused) return true
                if (e.x < width / 3f) goPrevious() else goNext()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                pauseTimer()
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (velocityY > 0 && abs(velocityY) > abs(velocityX)) {
                    requestClose()
                    return true
                }
                return false
            }
        })
    }

    override fun onInsetsApplied(insets: Insets) {
        topBar.setPadding(topPad + insets.left, dp(12f) + insets.top, topPad + insets.right, topPad)
        bottomBar.setPadding(bottomPad + insets.left, bottomPad, bottomPad + insets.right, bottomPad + insets.bottom)
    }

    private fun dp(value: Float) = InAppViewUtils.dp(context, value)

    override fun onDetachedFromWindow() {
        // Stop the segment timer when the view leaves the window (dismissal, but also a
        // configuration change that recreates the host Activity without calling animateOut).
        // Otherwise the advance runnable fires on a detached view and drives Glide.with()
        // on a destroyed Activity — an IllegalArgumentException crash.
        stopTimer()
        super.onDetachedFromWindow()
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            if (paused) resumeTimer()
        }
        return true
    }

    private fun showPage(target: Int) {
        if (target !in content.items.indices) return
        index = target
        val item = content.items[target]
        InAppImageLoader.load(item.imageUrl, imageView)
        bindText(titleView, item.title)
        bindText(subtitleView, item.message)
        showButtons(item)
        startTimer(item.durationMs)
    }

    private fun bindText(view: TextView, text: InAppText?) {
        if (text == null) {
            view.visibility = View.GONE
            return
        }
        view.text = text.text
        view.setTextColor(text.color)
        view.visibility = View.VISIBLE
    }

    // Rebuilt on every slide: each StoryItem carries its own button list (possibly empty), so the
    // previous slide's buttons must be removed, not left stale. A tap stops playback + dispatches.
    private fun showButtons(item: StoryItem) {
        buttonViews.forEach { bottomBar.removeView(it) }
        buttonViews.clear()
        for (button in item.buttons) {
            val view = InAppViewUtils.makeButton(context, button) { action ->
                stopTimer()
                dispatchAction(action)
            }
            buttonViews.add(view)
            bottomBar.addView(view, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12f) })
        }
    }

    // Auto-advance runs on postDelayed, not on the animator's onAnimationEnd: with
    // "Animator duration scale: off" the animator finishes on the first frame, which
    // would flip segments at frame rate (and with loops=true spam Glide forever).
    // The animator only paints the progress bar.
    private fun startTimer(durationMs: Long) {
        stopTimer()
        paused = false
        segmentDurationMs = durationMs
        segmentStartUptime = SystemClock.uptimeMillis()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            addUpdateListener { progress.update(index, animatedValue as Float) }
            start()
        }
        advance = Runnable { goNext() }.also { postDelayed(it, durationMs) }
    }

    private fun stopTimer() {
        animator?.cancel()
        animator = null
        advance?.let { removeCallbacks(it) }
        advance = null
    }

    // Long-press freezes playback: pause the progress animator, cancel the pending advance, and
    // remember the remaining time via an uptime clock (independent of "animator duration scale").
    internal fun pauseTimer() {
        if (paused || animator == null) return
        paused = true
        val elapsed = SystemClock.uptimeMillis() - segmentStartUptime
        remainingMs = (segmentDurationMs - elapsed).coerceAtLeast(0L)
        animator?.pause()
        advance?.let { removeCallbacks(it) }
    }

    private fun resumeTimer() {
        if (!paused) return
        paused = false
        segmentDurationMs = remainingMs
        segmentStartUptime = SystemClock.uptimeMillis()
        animator?.resume()
        advance = Runnable { goNext() }.also { postDelayed(it, remainingMs) }
    }

    private fun goNext() {
        when {
            index + 1 < content.items.size -> showPage(index + 1)
            content.loops -> showPage(0)
            else -> requestClose()
        }
    }

    private fun goPrevious() {
        showPage(if (index > 0) index - 1 else 0)
    }

    override fun animateIn() {
        InAppAnimations.fadeIn(this)
        showPage(0)
    }

    override fun animateOut(onEnd: () -> Unit) {
        stopTimer()
        InAppAnimations.fadeOut(this, onEnd)
    }
}
