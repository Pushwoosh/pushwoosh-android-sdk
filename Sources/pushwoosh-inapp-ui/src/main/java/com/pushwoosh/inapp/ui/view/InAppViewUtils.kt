package com.pushwoosh.inapp.ui.view

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppButton

/** Programmatic UI helpers shared by the native templates (no XML, full styling control). */
internal object InAppViewUtils {

    fun dp(context: Context, value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics).toInt()

    fun roundedBackground(color: Int, radiusPx: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusPx
        }

    fun makeText(
        context: Context,
        value: String,
        color: Int,
        sizeSp: Float,
        bold: Boolean,
        maxLines: Int = 0
    ): TextView =
        TextView(context).apply {
            text = value
            setTextColor(color)
            textSize = sizeSp
            if (bold) setTypeface(typeface, Typeface.BOLD)
            if (maxLines > 0) {
                this.maxLines = maxLines
                ellipsize = TextUtils.TruncateAt.END
            }
        }

    fun makeButton(
        context: Context,
        button: InAppButton,
        onClick: (InAppAction) -> Unit
    ): TextView =
        TextView(context).apply {
            text = button.text.text
            setTextColor(button.text.color)
            gravity = Gravity.CENTER
            isAllCaps = false
            textSize = 16f
            val radius = dp(context, button.cornerRadiusDp).toFloat()
            background = GradientDrawable().apply {
                setColor(button.backgroundColor)
                cornerRadius = radius
                setStroke(dp(context, 1.5f), button.borderColor)
            }
            val pv = dp(context, 12f)
            setPadding(pv, pv, pv, pv)
            setOnClickListener { onClick(button.action) }
        }

    fun makeCloseButton(context: Context, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#66000000"))
            }
            contentDescription = "Close"
            setOnClickListener { onClick() }
        }

    /** 48dp touch target (Android minimum) around the 34dp visible chip (= iOS closeSize):
     *  the oval is inset 7dp on every side, so templates position it "by the visible circle" —
     *  margin = iOS inset − 7dp. */
    const val CLOSE_BUTTON_SIZE_DP = 48f
    private const val CLOSE_CHIP_INSET_DP = 7f

    fun makeCardCloseButton(context: Context, onClick: () -> Unit): TextView =
        makeCloseButton(context, onClick).apply {
            background = InsetDrawable(background, dp(context, CLOSE_CHIP_INSET_DP))
        }
}
