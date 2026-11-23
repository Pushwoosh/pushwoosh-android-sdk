package com.pushwoosh.inline

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.pushwoosh.inapp.ui.R
import com.pushwoosh.inapp.ui.animation.ReduceMotionUtil
import com.pushwoosh.inline.internal.Cancelable
import com.pushwoosh.inline.internal.InlineModule
import com.pushwoosh.inline.internal.model.InlineAction
import com.pushwoosh.inline.internal.model.InlineBanner
import com.pushwoosh.inline.internal.view.InlineActionPerformer
import com.pushwoosh.inline.internal.view.InlineContent
import com.pushwoosh.inline.internal.view.InlineContentFactories
import com.pushwoosh.inline.internal.view.InlineContentHost
import com.pushwoosh.inline.internal.view.InlineVisibilityTracker
import com.pushwoosh.inline.internal.view.VisibilitySample
import com.pushwoosh.inline.internal.view.isDark
import com.pushwoosh.inline.internal.view.observeSystemColorScheme
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.utils.PWLog
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Shows the banner of the inline slot [slotId] in the app's layout.
 *
 * While the slot is empty the view is 0 high; when a banner arrives it takes the banner's height
 * (at most the banner's `maxHeight`, the rest is clipped), so a `wrap_content` view pushes its
 * neighbours. Hosts that size the view themselves follow [setOnHeightChangeListener]. Once on
 * screen the view requests its slot, draws its banner in [PushwooshInline.colorScheme] and reports
 * what happens to it. In XML the slot id is `app:pw_slotId`. Main thread only.
 */
class PushwooshInlineView : FrameLayout {

    /** Gets the height in dp the slot's banner needs at the view's width; `0` = the slot is empty. */
    fun interface OnHeightChangeListener {
        fun onHeightChanged(heightDp: Float)
    }

    private class Shown(val banner: InlineBanner, val host: InlineContentHost, val content: InlineContent)

    private var heightListener: OnHeightChangeListener? = null
    private var subscription: Cancelable? = null
    private var systemSchemeSubscription: Cancelable? = null
    private var subscribing = false

    /** The slot's banner, a control banner too; what is drawn of it is [shown]. */
    private var banner: InlineBanner? = null
    private var shown: Shown? = null
    private var dark = false
    private var fontScale = 0f
    private var densityDpi = 0
    private var reportedHeight = 0f

    private var animator: ValueAnimator? = null
    private var animationStartHeight = 0
    private var animationFraction: Float? = null
    private val leaving = mutableListOf<InlineContent>()
    private val schemeListener: () -> Unit = { applyColorScheme() }
    private val foregroundListener: () -> Unit = { recheckTracking() }

    private var tracker: InlineVisibilityTracker? = null
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener { recheckTracking() }
    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { recheckTracking() }
    private val probeRect = Rect()
    private val probeOffset = Point()

    /** The slot shown here; assigning another id shows that slot instead. */
    var slotId: String = ""
        set(value) {
            if (field == value) return
            field = value
            rebind()
        }

    constructor(context: Context) : this(context, null as AttributeSet?)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val values = context.obtainStyledAttributes(attrs, R.styleable.PushwooshInlineView)
        try {
            slotId = values.getString(R.styleable.PushwooshInlineView_pw_slotId).orEmpty()
        } finally {
            values.recycle()
        }
    }

    constructor(context: Context, slotId: String) : super(context) {
        this.slotId = slotId
    }

    /**
     * Called on the main thread when the height the banner needs changes; not called for a slot that stays empty.
     * The reported height excludes the view's own padding.
     */
    fun setOnHeightChangeListener(l: OnHeightChangeListener?) {
        heightListener = l
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        InlineModule.colorSchemeListeners += schemeListener
        InlineModule.foregroundListeners += foregroundListener
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
        viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        applyAppearance()
        bind()
        startTracking()
    }

    override fun onDetachedFromWindow() {
        systemSchemeSubscription?.cancel()
        systemSchemeSubscription = null
        stopTracking()
        viewTreeObserver.removeOnScrollChangedListener(scrollListener)
        viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        InlineModule.colorSchemeListeners -= schemeListener
        InlineModule.foregroundListeners -= foregroundListener
        subscription?.cancel()
        subscription = null
        animator?.end()
        releaseContent()
        super.onDetachedFromWindow()
    }

    /** Disposes what is drawn (a WebView with its scripts); attaching again draws the slot's banner anew. */
    private fun releaseContent() {
        val current = shown ?: return
        current.content.retire()
        shown = null
        banner = null
        remove(current.content)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        applyAppearance()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        recheckTracking()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        recheckTracking()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        recheckTracking()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val innerWidth = (MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight).coerceAtLeast(0)
        val childWidthSpec = if (widthMode == MeasureSpec.UNSPECIFIED) {
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        } else {
            MeasureSpec.makeMeasureSpec(innerWidth, MeasureSpec.EXACTLY)
        }
        val childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        for (i in 0 until childCount) getChildAt(i).measure(childWidthSpec, childHeightSpec)

        val target = targetHeight()
        val height = animationFraction?.let { animationStartHeight + ((target - animationStartHeight) * it).roundToInt() } ?: target
        val width = (shown?.content?.view?.measuredWidth ?: 0) + paddingLeft + paddingRight
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw) reportHeight()
    }

    override fun dispatchDraw(canvas: Canvas) {
        val saved = canvas.save()
        canvas.clipRect(0, 0, width, height)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(saved)
    }

    private fun bind() {
        if (slotId.isEmpty()) {
            show(null, animated = false)
            return
        }
        val manager = InlineModule.managerOrNull() ?: return
        subscribing = true
        try {
            subscription = manager.subscribe(slotId) { show(it, animated = !subscribing) }
        } finally {
            subscribing = false
        }
        manager.slotDidAppear(slotId)
    }

    private fun rebind() {
        stopTracking()
        subscription?.cancel()
        subscription = null
        if (!isAttachedToWindow) {
            show(null, animated = false)
            return
        }
        bind()
        startTracking()
    }

    private fun show(next: InlineBanner?, animated: Boolean) {
        val previous = banner
        banner = next
        if (next == null && previous == null) return
        if (next != null && next.slotId == previous?.slotId && next.sameShown(previous)) return
        draw(animated)
        stopTracking()
        startTracking()
    }

    /** Draws [banner] anew: a control banner and a banner that cannot be drawn leave the slot collapsed. */
    private fun draw(animated: Boolean) {
        val next = banner?.takeIf { !it.isControl }?.let { banner ->
            val host = Host(banner)
            val content = InlineContentFactories.create(context, banner, dark, host)
            if (content == null) PWLog.warn(TAG, "Inline banner '${banner.bannerId}' cannot be drawn, slot '${banner.slotId}' stays empty")
            content?.let { Shown(banner, host, it) }
        }
        replace(next, animated)
    }

    private fun replace(next: Shown?, animated: Boolean) {
        val previous = shown
        if (previous == null && next == null) return
        val startHeight = height
        animator?.cancel()
        previous?.content?.retire()
        shown = next
        next?.let { addView(it.content.view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)) }
        reportHeight()
        if (!animated || !isAttachedToWindow || ReduceMotionUtil.isReduceMotionEnabled(context)) {
            previous?.let { remove(it.content) }
            requestLayout()
            return
        }
        previous?.let { leaving += it.content }
        animate(startHeight, previous?.content?.view, next?.content?.view)
    }

    /** Height, alpha and scale 0.96 → 1 of the new content; the old content fades out and is removed at the end. */
    private fun animate(startHeight: Int, outgoing: View?, incoming: View?) {
        animationStartHeight = startHeight
        animationFraction = 0f
        incoming?.setAppearance(0f)
        val animation = ValueAnimator.ofFloat(0f, 1f)
        animation.duration = ANIMATION_MS
        animation.interpolator = DecelerateInterpolator()
        animation.addUpdateListener {
            val fraction = it.animatedValue as Float
            animationFraction = fraction
            incoming?.setAppearance(fraction)
            outgoing?.setAppearance(1f - fraction)
            requestLayout()
        }
        animation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(finished: Animator) {
                if (animator === finished) animator = null
                animationFraction = null
                incoming?.setAppearance(1f)
                leaving.toList().forEach { remove(it) }
                leaving.clear()
                requestLayout()
            }
        })
        animator = animation
        animation.start()
    }

    private fun View.setAppearance(fraction: Float) {
        alpha = fraction
        scaleX = HIDDEN_SCALE + (1f - HIDDEN_SCALE) * fraction
        scaleY = scaleX
    }

    private fun remove(content: InlineContent) {
        removeView(content.view)
        content.dispose()
    }

    /** Redraws the shown banner when the scheme, the font scale or the density changed, also while the view was detached. */
    private fun applyAppearance() {
        val metricsChanged = rememberTextMetrics()
        if (!applyColorScheme() && metricsChanged && shown != null) draw(animated = false)
    }

    /** Redraws the shown banner when the scheme turned dark or light; `true` = redrawn. */
    private fun applyColorScheme(): Boolean {
        val scheme = PushwooshInline.colorScheme
        if (isAttachedToWindow && scheme == PushwooshInlineColorScheme.SYSTEM) {
            if (systemSchemeSubscription == null) {
                systemSchemeSubscription = observeSystemColorScheme(context, schemeListener)
            }
        } else {
            systemSchemeSubscription?.cancel()
            systemSchemeSubscription = null
        }
        val nowDark = isDark(scheme, context)
        if (nowDark == dark) return false
        dark = nowDark
        if (shown == null) return false
        draw(animated = false)
        return true
    }

    /** Stores the font scale and density banners are drawn with; `true` = they changed since the last call. */
    private fun rememberTextMetrics(): Boolean {
        val configuration = resources.configuration
        val changed = configuration.fontScale != fontScale || configuration.densityDpi != densityDpi
        fontScale = configuration.fontScale
        densityDpi = configuration.densityDpi
        return changed
    }

    /** The view's height for its content: 0 when empty, else the content height capped at `maxHeight`. */
    private fun targetHeight(): Int {
        val current = shown ?: return 0
        val contentHeight = current.content.view.measuredHeight
        val capped = current.banner.maxHeight?.let { min(contentHeight, ceil(it * resources.displayMetrics.density).toInt()) } ?: contentHeight
        return capped + paddingTop + paddingBottom
    }

    private fun reportHeight() {
        val height = fittingHeight()
        if (height == reportedHeight) return
        reportedHeight = height
        heightListener?.onHeightChanged(height)
    }

    /** The banner's height in dp at the current width, rounded up and capped at `maxHeight`; 0 while empty or without a width. */
    private fun fittingHeight(): Float {
        val current = shown ?: return 0f
        val innerWidth = width - paddingLeft - paddingRight
        if (innerWidth <= 0) return 0f
        val view = current.content.view
        view.measure(MeasureSpec.makeMeasureSpec(innerWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val heightDp = ceil(view.measuredHeight.toDouble() / resources.displayMetrics.density).toFloat()
        return current.banner.maxHeight?.let { min(heightDp, it) } ?: heightDp
    }

    /** Starts timing the impression of the slot's banner, a control banner too; once per banner while attached. */
    private fun startTracking() {
        if (tracker != null || !isAttachedToWindow) return
        val current = banner ?: return
        val manager = InlineModule.managerOrNull() ?: return
        tracker = InlineVisibilityTracker(::sampleVisibility, { current.isControl }, manager.scheduler) {
            manager.bannerDidBecomeVisible(current.slotId, current.bannerId)
        }.also { it.start() }
    }

    private fun stopTracking() {
        tracker?.stop()
        tracker = null
    }

    private fun recheckTracking() {
        tracker?.recheck()
    }

    private fun sampleVisibility(): VisibilitySample = VisibilitySample(
        foreground = AndroidPlatformModule.isApplicationInForeground(),
        frontmost = isAttachedToWindow && isShown && hasWindowFocus(),
        visibleShare = targetHeight().takeIf { it > 0 }?.let { full -> if (getGlobalVisibleRect(probeRect)) probeRect.height().toFloat() / full else 0f },
        lineVisible = isLineVisible(),
        contentReady = banner?.isControl == true || shown?.content?.isReady == true,
    )

    /** The slot's top edge lies within the visible part of its parent: a 0-high slot is on screen. */
    private fun isLineVisible(): Boolean {
        val parent = parent as? View ?: return false
        if (!parent.getGlobalVisibleRect(probeRect, probeOffset)) return false
        val lineY = probeOffset.y + top
        return lineY >= probeRect.top && lineY < probeRect.bottom
    }

    /** Routes what [banner]'s content reports; content the view no longer shows is ignored. */
    private inner class Host(private val banner: InlineBanner) : InlineContentHost {
        private val isCurrent: Boolean get() = shown?.host === this

        override fun onContentSizeChanged() {
            if (!isCurrent) return
            requestLayout()
            reportHeight()
        }

        override fun onClick(elementId: String?, action: InlineAction?) {
            if (!isCurrent) return
            val manager = InlineModule.managerOrNull() ?: return
            if (action == null) {
                manager.click(banner.slotId, banner.bannerId, elementId)
            } else {
                InlineActionPerformer(context, manager).perform(banner.slotId, banner, elementId, action)
            }
        }

        override fun onClose() {
            if (isCurrent) InlineModule.managerOrNull()?.dismiss(banner.slotId, banner.bannerId)
        }

        override fun onContentReady() {
            if (isCurrent) recheckTracking()
        }
    }

    private companion object {
        const val TAG = "PushwooshInlineView"
        const val ANIMATION_MS = 500L
        const val HIDDEN_SCALE = 0.96f
    }
}
