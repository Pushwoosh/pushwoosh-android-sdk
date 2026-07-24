package com.pushwoosh.inapp.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Segmented progress bar — one segment per story; the active one fills as it plays. */
internal class StoriesProgressView(context: Context) : View(context) {

    private var count = 0
    private var activeIndex = 0
    private var activeProgress = 0f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4DFFFFFF") }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val gap = InAppViewUtils.dp(context, 4f).toFloat()
    private val radius = InAppViewUtils.dp(context, 1.5f).toFloat()
    private val rect = RectF()

    fun setCount(value: Int) {
        count = value
        invalidate()
    }

    fun update(index: Int, progress: Float) {
        activeIndex = index
        activeProgress = progress.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (count <= 0) return
        val segmentWidth = (width - gap * (count - 1)) / count
        val h = height.toFloat()
        for (i in 0 until count) {
            val left = i * (segmentWidth + gap)
            rect.set(left, 0f, left + segmentWidth, h)
            canvas.drawRoundRect(rect, radius, radius, trackPaint)

            val fraction = when {
                i < activeIndex -> 1f
                i == activeIndex -> activeProgress
                else -> 0f
            }
            if (fraction > 0f) {
                rect.set(left, 0f, left + segmentWidth * fraction, h)
                canvas.drawRoundRect(rect, radius, radius, fillPaint)
            }
        }
    }
}
