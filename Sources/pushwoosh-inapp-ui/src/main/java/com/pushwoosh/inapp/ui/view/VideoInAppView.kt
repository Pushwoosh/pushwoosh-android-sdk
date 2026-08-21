package com.pushwoosh.inapp.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.Insets
import com.pushwoosh.inapp.ui.animation.InAppAnimations
import com.pushwoosh.inapp.ui.model.VideoContent
import kotlin.math.min

/**
 * Full-screen autoplaying video with title, message and CTAs over a gradient scrim, a mute chip and
 * a ✕. The media itself is [InAppVideoPlayerView]'s business — the same split iOS makes between
 * `PWVideoInAppView` and `PWInAppVideoPlayerView`.
 */
internal class VideoInAppView(context: Context, content: VideoContent) : InAppTemplateView(context) {

    private val player = InAppVideoPlayerView(context)
    private val column: LinearLayout
    private val muteIcon = MuteIconDrawable()
    private var muteChip: ImageView? = null
    private var closeButton: View? = null

    private val sidePad = dp(24f)
    private val bottomPad = dp(28f)
    private val chipTopMargin = dp(5f)
    private val chipSideMargin = dp(9f)

    init {
        setBackgroundColor(Color.BLACK)
        addView(player, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(sidePad, sidePad, sidePad, bottomPad)
        }
        addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        // 48dp is this template's iOS constant (PWVideoInAppView.swift:77).
        InAppViewUtils.attachBottomScrim(column, 48f)

        var hasText = false
        content.title?.let {
            column.addView(InAppViewUtils.makeText(context, it.text, it.color, 28f, true), columnLp())
            hasText = true
        }
        content.message?.let {
            column.addView(
                InAppViewUtils.makeText(context, it.text, it.color, 16f, false),
                columnLp(topMargin = if (hasText) dp(8f) else 0)
            )
            hasText = true
        }
        content.buttons.forEachIndexed { index, button ->
            // iOS's stack spaces everything by 8 and inserts a custom 20 after the last text block —
            // so the wider gap exists only when there is text for the CTAs to be separated from
            // (PWVideoInAppView.swift:47,58-59).
            val topMargin = when {
                index > 0 -> dp(8f)
                hasText -> dp(20f)
                else -> 0
            }
            column.addView(InAppViewUtils.makeButton(context, button) { dispatchAction(it) }, columnLp(topMargin))
        }

        val chipSize = dp(InAppViewUtils.CLOSE_BUTTON_SIZE_DP)
        muteChip = makeMuteChip().also {
            addView(it, LayoutParams(chipSize, chipSize, Gravity.TOP or Gravity.START).apply {
                topMargin = chipTopMargin
                marginStart = chipSideMargin
            })
        }

        // Forced show (iOS parity): muting is not a way out, so with no CTA the ✕ is the only
        // guaranteed dismiss path (PWVideoInAppView.swift:90-91).
        if (content.showCloseButton || content.buttons.isEmpty()) {
            closeButton = InAppViewUtils.makeCardCloseButton(context) { requestClose() }.also {
                addView(it, LayoutParams(chipSize, chipSize, Gravity.TOP or Gravity.END).apply {
                    topMargin = chipTopMargin
                    marginEnd = chipSideMargin
                })
            }
        }

        player.onFailed = { requestClose() }
        player.onMuteStateChanged = { updateMuteIcon() }
        player.configure(content.videoUrl, content.posterUrl, content.fallbackImageUrl, content.loops, content.muted)
        updateMuteIcon()
    }

    private fun makeMuteChip(): ImageView =
        ImageView(context).apply {
            background = InAppViewUtils.chipBackground(context)
            val pad = dp(15f)
            setPadding(pad, pad, pad, pad)
            setImageDrawable(muteIcon)
            setOnClickListener { player.isMuted = !player.isMuted }
        }

    /** Reads the player, never a flag of its own — the chip cannot claim sound the video is not
     *  making (audio focus can be taken from us at any moment). */
    private fun updateMuteIcon() {
        muteIcon.muted = player.isMuted
        muteChip?.contentDescription = if (player.isMuted) "Unmute" else "Mute"
    }

    private fun dp(value: Float) = InAppViewUtils.dp(context, value)

    private fun columnLp(topMargin: Int = 0) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            this.topMargin = topMargin
        }

    override fun onInsetsApplied(insets: Insets) {
        val chip = muteChip ?: return
        column.setPadding(sidePad + insets.left, sidePad, sidePad + insets.right, bottomPad + insets.bottom)
        closeButton?.let {
            InAppViewUtils.applyChipInsets(it, layoutDirection, insets, chipTopMargin + insets.top, chipSideMargin)
        }
        InAppViewUtils.applyChipInsets(
            chip, layoutDirection, insets, chipTopMargin + insets.top, chipSideMargin, atStart = true
        )
    }

    // The chips pick their physical side from layoutDirection, but the direction resolves only at
    // the first measure — after the first insets pass — so an RTL host would keep the LTR sides
    // until the next insets change. The null check is not a showCloseButton in disguise: the base
    // View constructor delivers a first resolution before this class's fields exist.
    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)
        if (muteChip != null) onInsetsApplied(systemInsets)
    }

    override fun onHostPaused() = player.pauseForHost()

    override fun onHostResumed() = player.resumeForHost()

    override fun animateIn() = InAppAnimations.fadeIn(this)

    /** The engine goes down first (iOS parity, PWVideoInAppView.swift:161): the sound has to stop
     *  with the tap, not 220ms later when the fade finishes. */
    override fun animateOut(onEnd: () -> Unit) {
        player.teardown()
        InAppAnimations.fadeOut(this, onEnd)
    }
}

/**
 * The mute glyph, drawn rather than shipped: the module carries no drawable resources by design, and
 * the 🔇/🔊 emoji render in colour — beside the monochrome ✕ they read as someone else's icon.
 * Laid out on a 24-unit grid scaled to whatever bounds it gets.
 */
private class MuteIconDrawable : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
    }
    private val path = Path()
    private val arc = RectF()

    var muted: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        if (width <= 0f || height <= 0f) return
        val unit = min(width, height) / 24f
        val cx = bounds.left + width / 2f
        val cy = bounds.top + height / 2f

        // Speaker: a square body on the left flaring into a cone.
        path.reset()
        path.moveTo(cx - 8f * unit, cy - 3f * unit)
        path.lineTo(cx - 4f * unit, cy - 3f * unit)
        path.lineTo(cx, cy - 8f * unit)
        path.lineTo(cx, cy + 8f * unit)
        path.lineTo(cx - 4f * unit, cy + 3f * unit)
        path.lineTo(cx - 8f * unit, cy + 3f * unit)
        path.close()
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f * unit
        if (muted) {
            // A cross beside the cone, not a single slash: at 18dp one diagonal reads as a stray line.
            canvas.drawLine(cx + 2.5f * unit, cy - 3.5f * unit, cx + 8.5f * unit, cy + 3.5f * unit, paint)
            canvas.drawLine(cx + 8.5f * unit, cy - 3.5f * unit, cx + 2.5f * unit, cy + 3.5f * unit, paint)
        } else {
            arc.set(cx - 2f * unit, cy - 5f * unit, cx + 6f * unit, cy + 5f * unit)
            canvas.drawArc(arc, -50f, 100f, false, paint)
            arc.set(cx - 4f * unit, cy - 9f * unit, cx + 10f * unit, cy + 9f * unit)
            canvas.drawArc(arc, -50f, 100f, false, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Drawable, still abstract")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
