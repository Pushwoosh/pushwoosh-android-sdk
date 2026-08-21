package com.pushwoosh.inapp.ui.view

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.Insets
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

    /** The sheet's surface: top corners only, so the card sits flush on the bottom edge. The radii
     *  array makes this drawable hand out a *path* outline, so `clipToOutline` on the host view
     *  would be a no-op ([android.graphics.Outline.canClip] covers round-rect/oval only); the
     *  elevation shadow still works, because the path is convex. */
    fun topRoundedBackground(color: Int, radiusPx: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadii = floatArrayOf(radiusPx, radiusPx, radiusPx, radiusPx, 0f, 0f, 0f, 0f)
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
    private const val CHIP_INSET_DP = 7f

    /** The translucent oval every over-image chip sits on — the ✕ and the video's mute toggle — so
     *  the two read as one pair of controls instead of two accidents. */
    fun chipBackground(context: Context): Drawable =
        InsetDrawable(
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#66000000"))
            },
            dp(context, CHIP_INSET_DP)
        )

    fun makeCardCloseButton(context: Context, onClick: () -> Unit): TextView =
        makeCloseButton(context, onClick).apply { background = chipBackground(context) }

    /** The banner's ✕: iOS draws it bare at 60% white, letting the row spacing carry the touch
     *  area. The 48dp touch box stays the caller's job (see [CLOSE_BUTTON_SIZE_DP]) — the chip
     *  variants above are for controls that sit over an image and need their own contrast. */
    fun makeInlineCloseButton(context: Context, onClick: () -> Unit): TextView =
        makeCloseButton(context, onClick).apply {
            background = null
            setTextColor(Color.parseColor("#99FFFFFF"))
        }

    /** Re-anchors an edge-to-edge template's chip (the ✕, or the video's mute toggle) clear of the
     *  system bar or cutout on its own edge — END by default, START when [atStart];
     *  [topMargin] stays the caller's arithmetic (the fullscreen pays the status bar, the sheet's
     *  card never reaches it). Templates whose card carries its own screen margins inset the card
     *  instead and skip this (see ModalInAppView / CarouselInAppView). Two traps live here, in one
     *  place — the second one shipped silently broken in one template after being found in another,
     *  which is also why the START edge is a parameter and not a copy of this function:
     *  - the margin is logical while [Insets] are physical: in RTL the END edge is `insets.left`
     *    and the START edge is `insets.right`, and [layoutDirection] must be the template's
     *    *resolved* direction;
     *  - re-assigning `layoutParams`, not `requestLayout()`: `setMarginEnd` only re-arms the
     *    relative margin for resolution, and nothing re-resolves it on a plain layout pass — the ✕
     *    would keep the `rightMargin` it resolved at attach time and stay under the bar (verified
     *    on device, with the ✕ unreachable under a 141px landscape cutout inset). setLayoutParams
     *    re-resolves the direction and requests the layout in one go. */
    fun applyChipInsets(
        button: View,
        layoutDirection: Int,
        insets: Insets,
        topMargin: Int,
        baseSideMargin: Int,
        atStart: Boolean = false
    ) {
        val rtl = layoutDirection == View.LAYOUT_DIRECTION_RTL
        val sideInset = if (rtl == atStart) insets.right else insets.left
        button.layoutParams = (button.layoutParams as FrameLayout.LayoutParams).apply {
            this.topMargin = topMargin
            if (atStart) marginStart = baseSideMargin + sideInset else marginEnd = baseSideMargin + sideInset
        }
    }

    /** Rounds [view]'s **content**, not just its backdrop: [roundedBackground] paints behind a
     *  bitmap instead of trimming it, so an ImageView needs an outline clip to match iOS's
     *  `cornerRadius` + `clipsToBounds`. */
    fun clipToRoundedCorners(view: View, radiusPx: Float) {
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, radiusPx)
            }
        }
        view.clipToOutline = true
    }

    /** Height of the [bottomScrim] band, matching iOS's 140pt scrim. */
    const val BOTTOM_SCRIM_HEIGHT_DP = 140f

    /** iOS's scrim under slide text: a `clear → black 0.75` band at the bottom edge, so white text
     *  stays readable over a bright image. Sizing the band is the caller's job — the carousel pins
     *  [BOTTOM_SCRIM_HEIGHT_DP] outright, [attachBottomScrim] tracks the text — and the baked-in
     *  `minimumHeight` does not stand in for it: it only rescues an UNSPECIFIED spec, while under a
     *  bounded AT_MOST one a plain View takes the whole spec ([android.view.View.getDefaultSize]), so
     *  a WRAP_CONTENT band comes out a full-screen dim rather than a 140dp strip. */
    fun bottomScrim(context: Context): View =
        View(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#BF000000"))
            )
            minimumHeight = dp(context, BOTTOM_SCRIM_HEIGHT_DP)
        }

    /** The same [bottomScrim] band, but anchored to the text instead of capped at a constant: it goes
     *  into [column]'s host at the column's own z-index (over the image, under the text) and stays as
     *  tall as `column − paddingTop + rise`, so the darkening starts [riseAboveTextDp] above the first
     *  line of text. The `paddingTop` subtraction is the point: iOS anchors the scrim to a stack that
     *  carries no padding, so without it the rise would come out a padding too tall. The column must
     *  already be attached to its host — templates call this right after adding it.
     *
     *  The height is recomputed on every layout of the column, because the column grows with its
     *  content (text length, button count, accessibility font), with the bottom inset (onInsetsApplied
     *  re-pads it) and with rotation. The "already the right height" guard is not an optimization:
     *  the callback runs inside a layout pass, so writing layout params unconditionally would request
     *  the next pass forever. Each real height change does cost one extra measure+layout of the
     *  template, logged as `requestLayout() improperly called … during layout: running second layout
     *  pass` — that is the price of correctness, not a defect: mutating `layoutParams.height` in place
     *  requests nothing, and the band would keep its stale height until some later traversal. The
     *  second pass runs inside the same traversal, before `draw`, so no frame shows the old band.
     *  Until the first layout the band is the carousel's [BOTTOM_SCRIM_HEIGHT_DP] one, so a column
     *  that never lays out (a GONE column) degrades to that band. The height has to be spelled out
     *  here rather than left to WRAP_CONTENT plus
     *  [bottomScrim]'s `minimumHeight`: a plain View given a bounded AT_MOST spec takes the whole
     *  spec ([android.view.View.getDefaultSize] consults the minimum only for UNSPECIFIED), so a
     *  WRAP_CONTENT band would degrade to a full-screen dim instead. */
    fun attachBottomScrim(column: View, riseAboveTextDp: Float): View {
        val host = column.parent as FrameLayout
        val scrim = bottomScrim(column.context)
        val rise = dp(column.context, riseAboveTextDp)
        host.addView(
            scrim,
            host.indexOfChild(column),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(column.context, BOTTOM_SCRIM_HEIGHT_DP),
                Gravity.BOTTOM
            )
        )
        column.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val target = column.height - column.paddingTop + rise
            if (scrim.layoutParams.height != target) {
                scrim.layoutParams = scrim.layoutParams.apply { height = target }
            }
        }
        return scrim
    }
}
