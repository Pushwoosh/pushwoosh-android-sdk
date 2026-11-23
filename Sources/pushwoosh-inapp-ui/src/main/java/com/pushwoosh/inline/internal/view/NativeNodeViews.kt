package com.pushwoosh.inline.internal.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.pushwoosh.inapp.ui.animation.ReduceMotionUtil
import com.pushwoosh.inapp.ui.view.InAppVideoPlayerView
import com.pushwoosh.inline.internal.model.InlineClose
import com.pushwoosh.inline.internal.model.InlineClosePosition
import com.pushwoosh.inline.internal.model.InlinePosition
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val CLOSE_SIZE_DP = 28f
private const val CLOSE_INSET_DP = 8f

private fun View.dp(value: Float): Int = (value * resources.displayMetrics.density).roundToInt()

private fun exactly(size: Int): Int = View.MeasureSpec.makeMeasureSpec(max(size, 0), View.MeasureSpec.EXACTLY)

private fun atMost(size: Int): Int = View.MeasureSpec.makeMeasureSpec(max(size, 0), View.MeasureSpec.AT_MOST)

/** Empty space: as long as an exact size from its parent, else 0. */
internal class FlexibleSpace(context: Context) : View(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(exactOrZero(widthMeasureSpec), exactOrZero(heightMeasureSpec))
    }

    private fun exactOrZero(spec: Int): Int = if (MeasureSpec.getMode(spec) == MeasureSpec.EXACTLY) MeasureSpec.getSize(spec) else 0
}

/**
 * A node's style around its [content]: the background drawable, then [decor] (background image,
 * glass) sized to the box, then the content inset by the box padding. Decor never changes the
 * box size. A clickable box dims while pressed.
 */
internal class NodeBox(context: Context, val content: View) : ViewGroup(context) {
    private val decor = ArrayList<View>()

    init {
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /** Adds a layer between the background and the content. */
    fun addDecor(view: View) {
        decor += view
        addView(view, indexOfChild(content), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val horizontal = paddingLeft + paddingRight
        val vertical = paddingTop + paddingBottom
        content.measure(
            getChildMeasureSpec(widthMeasureSpec, horizontal, LayoutParams.MATCH_PARENT),
            getChildMeasureSpec(heightMeasureSpec, vertical, LayoutParams.MATCH_PARENT),
        )
        val width = resolveSize(content.measuredWidth + horizontal, widthMeasureSpec)
        val height = resolveSize(content.measuredHeight + vertical, heightMeasureSpec)
        if (content.measuredWidth != width - horizontal || content.measuredHeight != height - vertical) {
            content.measure(exactly(width - horizontal), exactly(height - vertical))
        }
        decor.forEach { it.measure(exactly(width), exactly(height)) }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        decor.forEach { it.layout(0, 0, r - l, b - t) }
        content.layout(paddingLeft, paddingTop, paddingLeft + content.measuredWidth, paddingTop + content.measuredHeight)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        alpha = if (isClickable && isPressed) PRESSED_ALPHA else 1f
    }

    override fun shouldDelayChildPressedState(): Boolean = false

    private companion object {
        const val PRESSED_ALPHA = 0.7f
    }
}

/**
 * As tall as its width divided by [ratio]; an exact height from the parent wins (a stretched row).
 * Without a width (an unconstrained row) it is 0 wide.
 */
internal open class AspectFrame(context: Context, ratio: Float) : FrameLayout(context) {
    var ratio: Float = ratio
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) 0 else MeasureSpec.getSize(widthMeasureSpec)
        val natural = (width / ratio).roundToInt()
        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> min(natural, MeasureSpec.getSize(heightMeasureSpec))
            else -> natural
        }
        super.onMeasure(exactly(width), exactly(height))
    }
}

/**
 * Children drawn over each other. The first one sizes the overlay; the others fill it or sit at
 * their position, [inset] px in from the edges.
 */
internal class OverlayLayout(context: Context, private val inset: Int) : ViewGroup(context) {
    private val positions = ArrayList<InlinePosition>()

    fun addChild(view: View, position: InlinePosition) {
        positions += if (positions.isEmpty()) InlinePosition.FILL else position
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount == 0) {
            setMeasuredDimension(resolveSize(0, widthMeasureSpec), resolveSize(0, heightMeasureSpec))
            return
        }
        val first = getChildAt(0)
        first.measure(
            getChildMeasureSpec(widthMeasureSpec, 0, LayoutParams.MATCH_PARENT),
            getChildMeasureSpec(heightMeasureSpec, 0, LayoutParams.WRAP_CONTENT),
        )
        val width = resolveSize(first.measuredWidth, widthMeasureSpec)
        val height = resolveSize(first.measuredHeight, heightMeasureSpec)
        if (first.measuredWidth != width || first.measuredHeight != height) first.measure(exactly(width), exactly(height))
        val innerWidth = width - 2 * inset
        val innerHeight = height - 2 * inset
        for (i in 1 until childCount) {
            when (positions[i]) {
                InlinePosition.FILL -> getChildAt(i).measure(exactly(width), exactly(height))
                InlinePosition.TOP, InlinePosition.BOTTOM -> getChildAt(i).measure(exactly(innerWidth), atMost(innerHeight))
                else -> getChildAt(i).measure(atMost(innerWidth), atMost(innerHeight))
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val w = child.measuredWidth
            val h = child.measuredHeight
            val left = when (positions[i]) {
                InlinePosition.FILL -> 0
                InlinePosition.CENTER -> (width - w) / 2
                InlinePosition.TOP_RIGHT, InlinePosition.BOTTOM_RIGHT -> width - inset - w
                else -> inset
            }
            val top = when (positions[i]) {
                InlinePosition.FILL -> 0
                InlinePosition.CENTER -> (height - h) / 2
                InlinePosition.BOTTOM, InlinePosition.BOTTOM_LEFT, InlinePosition.BOTTOM_RIGHT -> height - inset - h
                else -> inset
            }
            child.layout(left, top, left + w, top + h)
        }
    }
}

/**
 * Pages side by side with a dot indicator under them, as tall as the tallest page. Turns every
 * [intervalMs] while attached, unless the user drags or animations are off. [onPageTap] runs for
 * a tap on a page area no element inside the page handles.
 */
@SuppressLint("ViewConstructor")
internal class InlinePagerView(
    context: Context,
    private val pages: List<View>,
    private val intervalMs: Long?,
    indicatorColor: Int,
    private val onPageTap: (() -> Unit)?,
) : LinearLayout(context) {
    private val pager = ViewPager2(context)
    private val recycler = pager.getChildAt(0) as? RecyclerView
    private val indicator = LinearLayout(context)
    private val dots = ArrayList<View>()
    private val turn = Runnable { turnPage() }
    private var turning = false

    init {
        orientation = VERTICAL
        recycler?.overScrollMode = View.OVER_SCROLL_NEVER
        recycler?.itemAnimator = null
        pager.adapter = PageAdapter()
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = selectDot(position)

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) stopTurning() else if (state == ViewPager2.SCROLL_STATE_IDLE) startTurning()
            }
        })
        addView(pager, LayoutParams(LayoutParams.MATCH_PARENT, 0))

        indicator.orientation = HORIZONTAL
        indicator.gravity = Gravity.CENTER
        val size = dp(DOT_DP)
        indicator.setPadding(0, dp(INDICATOR_PADDING_DP), 0, dp(INDICATOR_PADDING_DP))
        for (i in pages.indices) {
            val dot = View(context)
            dot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(indicatorColor)
            }
            dots += dot
            indicator.addView(dot, LayoutParams(size, size).apply { setMargins(size / 2, 0, size / 2, 0) })
        }
        indicator.visibility = if (pages.size > 1) View.VISIBLE else View.GONE
        addView(indicator, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        selectDot(0)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val tallest = pages.maxOf { page ->
            page.measure(exactly(width), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            page.measuredHeight
        }
        pager.layoutParams.height = tallest
        // The pages were just measured at their natural height: make the pager lay them out again.
        pager.forceLayout()
        recycler?.forceLayout()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTurning()
    }

    override fun onDetachedFromWindow() {
        stopTurning()
        super.onDetachedFromWindow()
    }

    fun stopTurning() {
        turning = false
        removeCallbacks(turn)
    }

    private fun startTurning() {
        stopTurning()
        val interval = intervalMs ?: return
        if (pages.size < 2 || !isAttachedToWindow || ReduceMotionUtil.isReduceMotionEnabled(context)) return
        turning = true
        postDelayed(turn, interval)
    }

    private fun turnPage() {
        if (!turning) return
        pager.setCurrentItem((pager.currentItem + 1) % pages.size, true)
        intervalMs?.let { postDelayed(turn, it) }
    }

    private fun selectDot(position: Int) {
        dots.forEachIndexed { i, dot -> dot.alpha = if (i == position) 1f else INACTIVE_DOT_ALPHA }
    }

    private class PageHolder(val frame: FrameLayout) : RecyclerView.ViewHolder(frame)

    /** One view type per page: each holder shows its own prebuilt page. */
    private inner class PageAdapter : RecyclerView.Adapter<PageHolder>() {
        override fun getItemCount(): Int = pages.size

        override fun getItemViewType(position: Int): Int = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            val frame = FrameLayout(parent.context)
            frame.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT)
            onPageTap?.let { tap -> frame.setOnClickListener { tap() } }
            return PageHolder(frame)
        }

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            val page = pages[position]
            if (page.parent === holder.frame) return
            (page.parent as? ViewGroup)?.removeView(page)
            holder.frame.removeAllViews()
            holder.frame.addView(page, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
    }

    private companion object {
        const val DOT_DP = 7f
        const val INDICATOR_PADDING_DP = 8f
        const val INACTIVE_DOT_ALPHA = 0.3f
    }
}

/**
 * A muted looping video in its aspect ratio that plays only while at least half of its height is
 * on screen, checked every 300 ms. The player is built on attach and released on detach, so a
 * re-attached banner plays again.
 */
@SuppressLint("ViewConstructor")
internal class InlineVideoView(
    context: Context,
    ratio: Float,
    private val url: String,
    private val poster: String?,
) : AspectFrame(context, ratio) {
    private var player: InAppVideoPlayerView? = null
    private var playing = false
    private val probe = Rect()
    private val check = object : Runnable {
        override fun run() {
            updatePlayback()
            postDelayed(this, CHECK_MS)
        }
    }

    init {
        setBackgroundColor(Color.BLACK)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (player == null) {
            val view = InAppVideoPlayerView(context)
            view.keepScreenOn = false
            view.configure(url, poster, poster, loop = true, muted = true)
            view.pauseForHost()
            playing = false
            addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            player = view
        }
        post(check)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(check)
        release()
        super.onDetachedFromWindow()
    }

    fun release() {
        val view = player ?: return
        player = null
        playing = false
        view.teardown()
        removeView(view)
    }

    private fun updatePlayback() {
        val view = player ?: return
        val visible = isShown && windowVisibility == View.VISIBLE && height > 0 &&
            getGlobalVisibleRect(probe) && probe.height() * 2 >= height
        if (visible && !playing) {
            playing = true
            view.resumeForHost()
        } else if (!visible && playing) {
            playing = false
            view.pauseForHost()
        }
    }

    private companion object {
        const val CHECK_MS = 300L
    }
}

/** The banner content with the optional close button over its top corner; the button does not size the banner. */
@SuppressLint("ViewConstructor")
internal class BannerFrame(context: Context, private val content: View, close: InlineClose?, onClose: () -> Unit) : ViewGroup(context) {
    private val closeButton: View?
    private val closeLeft = close?.position == InlineClosePosition.TOP_LEFT

    init {
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        closeButton = close?.let { CloseButton(context, it).apply { setOnClickListener { onClose() } } }
        closeButton?.let { addView(it) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        content.measure(
            getChildMeasureSpec(widthMeasureSpec, 0, LayoutParams.MATCH_PARENT),
            getChildMeasureSpec(heightMeasureSpec, 0, LayoutParams.WRAP_CONTENT),
        )
        val size = dp(CLOSE_SIZE_DP)
        closeButton?.measure(exactly(size), exactly(size))
        setMeasuredDimension(resolveSize(content.measuredWidth, widthMeasureSpec), resolveSize(content.measuredHeight, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        content.layout(0, 0, content.measuredWidth, content.measuredHeight)
        val button = closeButton ?: return
        val inset = dp(CLOSE_INSET_DP)
        val left = if (closeLeft) inset else r - l - inset - button.measuredWidth
        button.layout(left, inset, left + button.measuredWidth, inset + button.measuredHeight)
    }
}

/** A cross on a rounded background. */
@SuppressLint("ViewConstructor")
private class CloseButton(context: Context, close: InlineClose) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = close.color
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2f * resources.displayMetrics.density
    }

    init {
        background = GradientDrawable().apply {
            setColor(close.background)
            cornerRadius = min(close.cornerRadius, CLOSE_SIZE_DP / 2) * resources.displayMetrics.density
        }
        contentDescription = "Close"
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val half = CROSS_HALF_SIZE_DP * resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawLine(cx - half, cy - half, cx + half, cy + half, paint)
        canvas.drawLine(cx - half, cy + half, cx + half, cy - half, paint)
    }

    private companion object {
        const val CROSS_HALF_SIZE_DP = 5f
    }
}
