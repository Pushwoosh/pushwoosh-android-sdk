package com.pushwoosh.inapp.ui.view

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.pushwoosh.inapp.ui.animation.InAppAnimations
import com.pushwoosh.inapp.ui.animation.ReduceMotionUtil
import com.pushwoosh.inapp.ui.image.InAppImageLoader
import com.pushwoosh.inapp.ui.model.ModalContent

/** Centered card: optional image, title, message and a vertical stack of buttons.
 *  `dimsBackground` adds a tap-to-dismiss dimmed backdrop. */
internal class ModalInAppView(context: Context, content: ModalContent) : InAppTemplateView(context) {

    private val card: FrameLayout
    private val reduceMotion = ReduceMotionUtil.isReduceMotionEnabled(context)

    init {
        if (content.dimsBackground) {
            addView(View(context).apply {
                setBackgroundColor(Color.parseColor("#99000000"))
                setOnClickListener { requestClose() }
            }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }

        // The card is a frame so the ✕ can sit on the card itself (iOS anchors it to the
        // content host) and ride the card's enter/exit animation.
        card = FrameLayout(context).apply {
            background = InAppViewUtils.roundedBackground(content.backgroundColor, dp(16f).toFloat())
            clipToOutline = true
            // Clickable so the card swallows body taps: without it a tap on the title/text/image
            // falls through to the backdrop's tap-to-dismiss (modal closes by tapping itself), and
            // on a floating modal leaks into the app UI underneath.
            isClickable = true
        }
        addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
            val m = dp(28f)
            leftMargin = m
            rightMargin = m
        })

        // iOS media geometry: the image sits 14dp from the card edge (text keeps 20dp) and has
        // its own 18dp rounding, so the ✕ chip (20dp off the corner) floats inside the picture
        // over its rounded corner instead of kissing it; without an image nothing shifts.
        val hasImage = content.imageUrl != null
        val mediaInset = dp(14f)
        val textInset = dp(20f) - mediaInset
        val contentColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(mediaInset, if (hasImage) mediaInset else dp(20f), mediaInset, dp(20f))
        }
        card.addView(contentColumn, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        content.imageUrl?.let { url ->
            val image = ImageView(context).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                // Reserve height so a failed/absent image can't collapse the WRAP_CONTENT card
                // (iOS holds a fixed-aspect placeholder box); the programmatic placeholder has no
                // intrinsic size on its own.
                minimumHeight = dp(180f)
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, dp(18f).toFloat())
                    }
                }
                clipToOutline = true
            }
            contentColumn.addView(image, column(bottomMargin = dp(12f)))
            InAppImageLoader.load(url, image)
        }
        content.title?.let {
            contentColumn.addView(
                InAppViewUtils.makeText(context, it.text, it.color, 20f, true),
                column(horizontalMargin = textInset)
            )
        }
        content.message?.let {
            contentColumn.addView(
                InAppViewUtils.makeText(context, it.text, it.color, 15f, false),
                column(topMargin = dp(8f), horizontalMargin = textInset)
            )
        }
        content.buttons.forEach { button ->
            contentColumn.addView(
                InAppViewUtils.makeButton(context, button) { dispatchAction(it) },
                column(topMargin = dp(12f), horizontalMargin = textInset)
            )
        }

        // Forced show (iOS parity): with no buttons the ✕ is the only guaranteed dismiss path —
        // a floating modal has no backdrop tap and BACK doesn't reach the overlay.
        if (content.showCloseButton || content.buttons.isEmpty()) {
            val size = dp(InAppViewUtils.CLOSE_BUTTON_SIZE_DP)
            card.addView(InAppViewUtils.makeCardCloseButton(context) { requestClose() },
                LayoutParams(size, size, Gravity.TOP or Gravity.END).apply {
                    topMargin = dp(13f)
                    marginEnd = dp(13f)
                })
        }
    }

    private fun dp(value: Float) = InAppViewUtils.dp(context, value)

    private fun column(topMargin: Int = 0, bottomMargin: Int = 0, horizontalMargin: Int = 0) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            this.topMargin = topMargin
            this.bottomMargin = bottomMargin
            marginStart = horizontalMargin
            marginEnd = horizontalMargin
        }

    override fun animateIn() = InAppAnimations.scaleIn(card, reduceMotion)

    override fun animateOut(onEnd: () -> Unit) = InAppAnimations.scaleOut(card, reduceMotion, onEnd)
}
