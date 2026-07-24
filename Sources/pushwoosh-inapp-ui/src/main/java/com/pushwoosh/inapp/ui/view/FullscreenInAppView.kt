package com.pushwoosh.inapp.ui.view

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.Insets
import com.pushwoosh.inapp.ui.animation.InAppAnimations
import com.pushwoosh.inapp.ui.image.InAppImageLoader
import com.pushwoosh.inapp.ui.model.FullscreenContent

/** Edge-to-edge takeover: full-bleed image with overlaid title, message and buttons. */
internal class FullscreenInAppView(context: Context, content: FullscreenContent) : InAppTemplateView(context) {

    private val column: LinearLayout
    private val basePad = InAppViewUtils.dp(context, 24f)
    private val closeTopMargin = InAppViewUtils.dp(context, 5f)
    private val closeEndMargin = InAppViewUtils.dp(context, 9f)
    private var closeButton: View? = null

    init {
        setBackgroundColor(content.backgroundColor)

        val image = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        InAppImageLoader.load(content.imageUrl, image)

        column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(basePad, basePad, basePad, basePad)
        }
        addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        content.title?.let {
            column.addView(InAppViewUtils.makeText(context, it.text, it.color, 26f, true), columnLp())
        }
        content.message?.let {
            column.addView(InAppViewUtils.makeText(context, it.text, it.color, 16f, false), columnLp(topMargin = dp(8f)))
        }
        content.buttons.forEach { button ->
            column.addView(
                InAppViewUtils.makeButton(context, button) { dispatchAction(it) },
                columnLp(topMargin = dp(12f))
            )
        }

        // Forced show (iOS parity): with no buttons the ✕ is the only guaranteed dismiss path.
        if (content.showCloseButton || content.buttons.isEmpty()) {
            val size = InAppViewUtils.dp(context, InAppViewUtils.CLOSE_BUTTON_SIZE_DP)
            closeButton = InAppViewUtils.makeCardCloseButton(context) { requestClose() }.also {
                addView(it, LayoutParams(size, size, Gravity.TOP or Gravity.END).apply {
                    topMargin = closeTopMargin
                    marginEnd = closeEndMargin
                })
            }
        }
    }

    override fun onInsetsApplied(insets: Insets) {
        column.setPadding(basePad + insets.left, basePad, basePad + insets.right, basePad + insets.bottom)
        closeButton?.let {
            // marginEnd is logical while Insets are physical: in RTL the END edge is insets.left.
            val endInset = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) insets.left else insets.right
            (it.layoutParams as LayoutParams).apply {
                topMargin = closeTopMargin + insets.top
                marginEnd = closeEndMargin + endInset
            }
            it.requestLayout()
        }
    }

    private fun dp(value: Float) = InAppViewUtils.dp(context, value)

    private fun columnLp(topMargin: Int = 0) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            this.topMargin = topMargin
        }

    override fun animateIn() = InAppAnimations.fadeIn(this)

    override fun animateOut(onEnd: () -> Unit) = InAppAnimations.fadeOut(this, onEnd)
}
