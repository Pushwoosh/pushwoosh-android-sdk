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
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.pushwoosh.inapp.ui.animation.InAppAnimations
import com.pushwoosh.inapp.ui.animation.ReduceMotionUtil
import com.pushwoosh.inapp.ui.image.InAppImageLoader
import com.pushwoosh.inapp.ui.model.CarouselContent
import com.pushwoosh.inapp.ui.model.CarouselItem
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppText
import kotlin.math.roundToInt

/** iOS card proportion: `height = width × 1.32`. */
private const val CARD_ASPECT = 1.32f

/** Card size for the available [widthPx] and [maxHeightPx] (`0` = height unbounded). iOS pins the
 *  height to `width × 1.32` and simply lets the card overflow the screen in landscape; we narrow
 *  the width instead, so slides keep their aspect and the ✕ and dots stay on screen. */
internal fun carouselCardSize(widthPx: Int, maxHeightPx: Int): Pair<Int, Int> {
    // Rounded, not truncated: 660 / 1.32f is 499.99994 in float, and dropping the fraction would
    // shave a pixel off a card whose aspect is exact.
    val height = (widthPx * CARD_ASPECT).roundToInt()
    if (maxHeightPx <= 0 || height <= maxHeightPx) return widthPx to height
    return (maxHeightPx / CARD_ASPECT).roundToInt() to maxHeightPx
}

/** Swipeable slides inside a black card of fixed proportion over a dimmed backdrop, with a
 *  page-dot indicator and the ✕ on the card itself (1:1 with iOS `PWCarouselInAppView`). */
internal class CarouselInAppView(context: Context, content: CarouselContent) : InAppTemplateView(context) {

    private val reduceMotion = ReduceMotionUtil.isReduceMotionEnabled(context)
    private val dots = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }
    private val closeMargin = dp(5f)
    private val cardMargin = dp(22f)
    private val dim: View
    private val card: FrameLayout

    init {
        // The dim is a child of its own rather than the root's background: its alpha animates
        // independently of the card's scale, and the card's clipToOutline doesn't fight a root fill.
        dim = View(context).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
            setOnClickListener { requestClose() }
        }
        addView(dim, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // isClickable so the card swallows taps that missed the slide: without it they fall through
        // to the dim with its tap-to-close, and the card closes itself.
        card = AspectCard(context).apply {
            background = InAppViewUtils.roundedBackground(Color.BLACK, dp(24f).toFloat())
            clipToOutline = true
            isClickable = true
        }
        addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
            leftMargin = cardMargin
            rightMargin = cardMargin
            topMargin = cardMargin
            bottomMargin = cardMargin
        })

        val pager = ViewPager2(context).apply {
            adapter = CarouselAdapter(content.items) { dispatchAction(it) }
        }
        card.addView(pager, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        card.addView(dots, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply { bottomMargin = dp(12f) })

        // The ✕ is always shown: slide taps are optional and there is no swipe-dismiss; iOS treats
        // the backdrop tap as not guaranteed and forces the button — mirrored here.
        val size = dp(InAppViewUtils.CLOSE_BUTTON_SIZE_DP)
        card.addView(InAppViewUtils.makeCardCloseButton(context) { requestClose() },
            LayoutParams(size, size, Gravity.TOP or Gravity.END).apply {
                topMargin = closeMargin
                marginEnd = closeMargin
            })

        buildDots(content.items.size)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = highlightDot(position)
        })
    }

    override fun onInsetsApplied(insets: Insets) {
        // Insets land on the card, so the ✕ and the dots need no inset logic: the card is already
        // clear of cutouts and they live inside it. The margins are physical, like Insets, so
        // there is nothing to mirror in RTL.
        //
        // Symmetric on purpose — the larger inset of each axis is applied to both sides. A
        // centered FrameLayout child is offset by the *whole* leftMargin − rightMargin (see
        // FrameLayout.layoutChildren), not half of it, so asymmetric margins would shift the card
        // by twice the inset difference and push it off screen once a one-sided inset passes
        // 2 × 22dp. Paying the bigger inset on both sides keeps the card centered on the screen,
        // which is where iOS centers it too (its constraints ignore the safe area), and every edge
        // clear of the bars.
        val horizontal = cardMargin + maxOf(insets.left, insets.right)
        val vertical = cardMargin + maxOf(insets.top, insets.bottom)
        (card.layoutParams as LayoutParams).apply {
            leftMargin = horizontal
            rightMargin = horizontal
            topMargin = vertical
            bottomMargin = vertical
        }
        card.requestLayout()
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

    override fun animateIn() {
        InAppAnimations.fadeIn(dim)
        InAppAnimations.scaleIn(card, reduceMotion)
    }

    override fun animateOut(onEnd: () -> Unit) {
        // onEnd rides the card only, so the callback fires once; fadeOut and scaleOut share the same
        // duration, so the dim never outlives the card.
        InAppAnimations.fadeOut(dim) {}
        InAppAnimations.scaleOut(card, reduceMotion, onEnd)
    }

    /** The card recomputes its proportion on every measure: `InAppOverlayActivity` declares
     *  `configChanges="orientation|..."`, so it is never recreated and the view is never rebuilt —
     *  a height computed once from `displayMetrics` would be left over from the last orientation. */
    private class AspectCard(context: Context) : FrameLayout(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val maxHeight =
                if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) 0
                else MeasureSpec.getSize(heightMeasureSpec)
            val (width, height) = carouselCardSize(MeasureSpec.getSize(widthMeasureSpec), maxHeight)
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )
        }
    }

    private class CarouselAdapter(
        private val items: List<CarouselItem>,
        private val onAction: (InAppAction) -> Unit
    ) : RecyclerView.Adapter<CarouselAdapter.PageHolder>() {

        class PageHolder(
            val root: FrameLayout,
            val image: ImageView,
            val scrim: View,
            val title: TextView,
            val message: TextView
        ) : RecyclerView.ViewHolder(root)

        // The cell's shape mirrors the iOS cell: assembled once, then bind only sets values.
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            val context = parent.context
            val root = FrameLayout(context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
                )
            }

            val image = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            root.addView(image, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))

            val scrim = InAppViewUtils.bottomScrim(context)
            root.addView(scrim, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                InAppViewUtils.dp(context, InAppViewUtils.BOTTOM_SCRIM_HEIGHT_DP),
                Gravity.BOTTOM
            ))

            val textColumn = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            root.addView(textColumn, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ).apply {
                marginStart = InAppViewUtils.dp(context, 18f)
                marginEnd = InAppViewUtils.dp(context, 18f)
                bottomMargin = InAppViewUtils.dp(context, 36f)
            })

            val title = InAppViewUtils.makeText(context, "", Color.WHITE, 20f, true, maxLines = 2)
            textColumn.addView(title, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            val message = InAppViewUtils.makeText(context, "", Color.WHITE, 14f, false, maxLines = 2)
            textColumn.addView(message, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))

            return PageHolder(root, image, scrim, title, message)
        }

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            val item = items[position]
            InAppImageLoader.load(item.imageUrl, holder.image)

            val hasTitle = bindText(holder.title, item.title)
            val hasMessage = bindText(holder.message, item.message)
            // On iOS the gap is UIStackView.spacing, which applies between items: a message with no
            // title above it is not pushed down.
            (holder.message.layoutParams as LinearLayout.LayoutParams).topMargin =
                if (hasTitle) InAppViewUtils.dp(holder.root.context, 3f) else 0
            holder.message.requestLayout()
            holder.scrim.visibility = if (hasTitle || hasMessage) View.VISIBLE else View.GONE

            // Holders are recycled: an action-less slide must clear the previous slide's
            // listener, or its tap would fire a neighbour's action and click analytics.
            val action = item.action
            if (action != null) {
                holder.root.setOnClickListener { onAction(action) }
            } else {
                holder.root.setOnClickListener(null)
                holder.root.isClickable = false
            }
        }

        /** `true` when [text] has something to show. A blank string is an absence (iOS hides the
         *  label): it takes no line and does not light up the scrim. */
        private fun bindText(view: TextView, text: InAppText?): Boolean {
            if (text == null || text.text.isBlank()) {
                view.visibility = View.GONE
                return false
            }
            view.text = text.text
            view.setTextColor(text.color)
            view.visibility = View.VISIBLE
            return true
        }

        override fun getItemCount() = items.size
    }
}
