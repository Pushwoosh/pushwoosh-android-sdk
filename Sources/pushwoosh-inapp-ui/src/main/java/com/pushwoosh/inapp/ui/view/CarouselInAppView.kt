package com.pushwoosh.inapp.ui.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.Insets
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.pushwoosh.inapp.ui.animation.InAppAnimations
import com.pushwoosh.inapp.ui.image.InAppImageLoader
import com.pushwoosh.inapp.ui.model.CarouselContent
import com.pushwoosh.inapp.ui.model.CarouselItem
import com.pushwoosh.inapp.ui.model.InAppAction

/** Paged, swipeable cards (image + title + message) over a dimmed backdrop, with a page-dot
 *  indicator (like iOS UIPageControl). Pager height is a fraction of the screen so it adapts
 *  to phones and tablets alike. */
internal class CarouselInAppView(context: Context, content: CarouselContent) : InAppTemplateView(context) {

    private val dots = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }
    private val closeMargin = dp(5f)
    private val closeButton: View

    init {
        setBackgroundColor(Color.parseColor("#CC000000"))
        // Tap on the dimmed backdrop closes; pager/card taps are consumed by their own
        // children, so only empty backdrop zones reach this listener (1:1 with iOS).
        setOnClickListener { requestClose() }

        val pager = ViewPager2(context).apply {
            adapter = CarouselAdapter(content.items) { dispatchAction(it) }
        }

        // The ✕ rides the pager frame, like the iOS card anchor. Always shown: slide taps are
        // optional and there is no swipe-dismiss; iOS treats the backdrop tap as not guaranteed
        // and forces the button — mirrored here.
        val pagerFrame = FrameLayout(context)
        pagerFrame.addView(pager, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val size = dp(InAppViewUtils.CLOSE_BUTTON_SIZE_DP)
        closeButton = InAppViewUtils.makeCardCloseButton(context) { requestClose() }.also {
            pagerFrame.addView(it, LayoutParams(size, size, Gravity.TOP or Gravity.END).apply {
                topMargin = closeMargin
                marginEnd = closeMargin
            })
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val pagerHeight = (resources.displayMetrics.heightPixels * 0.7f).toInt()
        container.addView(pagerFrame, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, pagerHeight))
        container.addView(dots, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16f) })
        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        buildDots(content.items.size)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = highlightDot(position)
        })
    }

    override fun onInsetsApplied(insets: Insets) {
        // The pager is full-bleed horizontally, so a landscape cutout/nav bar can cover the ✕;
        // vertically it is centered and never under the status bar — end inset only.
        // marginEnd is logical while Insets are physical: in RTL the END edge is insets.left.
        val endInset = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) insets.left else insets.right
        (closeButton.layoutParams as LayoutParams).apply { marginEnd = closeMargin + endInset }
        closeButton.requestLayout()
    }

    private fun buildDots(count: Int) {
        dots.removeAllViews()
        if (count <= 1) {
            dots.visibility = View.GONE
            return
        }
        val size = dp(8f)
        for (i in 0 until count) {
            val dot = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.WHITE)
                }
            }
            dots.addView(dot, LinearLayout.LayoutParams(size, size).apply {
                val m = dp(4f)
                leftMargin = m
                rightMargin = m
            })
        }
        highlightDot(0)
    }

    private fun highlightDot(active: Int) {
        for (i in 0 until dots.childCount) {
            dots.getChildAt(i).alpha = if (i == active) 1f else 0.4f
        }
    }

    private fun dp(value: Float) = InAppViewUtils.dp(context, value)

    override fun animateIn() = InAppAnimations.fadeIn(this)

    override fun animateOut(onEnd: () -> Unit) = InAppAnimations.fadeOut(this, onEnd)

    private class CarouselAdapter(
        private val items: List<CarouselItem>,
        private val onAction: (InAppAction) -> Unit
    ) : RecyclerView.Adapter<CarouselAdapter.PageHolder>() {

        class PageHolder(val container: LinearLayout) : RecyclerView.ViewHolder(container)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            val context = parent.context
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
                )
                val p = InAppViewUtils.dp(context, 24f)
                setPadding(p, p, p, p)
            }
            return PageHolder(container)
        }

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            val item = items[position]
            val context = holder.container.context
            holder.container.removeAllViews()

            val image = ImageView(context).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            holder.container.addView(
                image,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            )
            InAppImageLoader.load(item.imageUrl, image)

            item.title?.let {
                holder.container.addView(InAppViewUtils.makeText(context, it.text, it.color, 18f, true, maxLines = 2))
            }
            item.message?.let {
                holder.container.addView(InAppViewUtils.makeText(context, it.text, it.color, 14f, false, maxLines = 2))
            }
            // Holders are recycled: an action-less slide must clear the previous slide's
            // listener, or its tap would fire a neighbour's action and click analytics.
            val action = item.action
            if (action != null) {
                holder.container.setOnClickListener { onAction(action) }
            } else {
                holder.container.setOnClickListener(null)
                holder.container.isClickable = false
            }
        }

        override fun getItemCount() = items.size
    }
}
