/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.inbox.ui.utils

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import androidx.core.content.ContextCompat
import android.view.View


fun View.clear() {
    this.alpha = 1f
    scaleY = 1f
    scaleX = 1f
    translationX = 0f
    translationY = 0f
    rotation = 0f
    rotationX = 0f
    rotationY = 0f
    pivotX = measuredWidth / 2f
    pivotY = measuredHeight / 2f

    animate().setInterpolator(null)
            .startDelay = 0L
}

internal fun getBitmap(context: Context?, drawableId: Int): Bitmap? {
    if (context == null) {
        return null
    }
    val drawable = ContextCompat.getDrawable(context, drawableId)
    return when (drawable) {
        is BitmapDrawable -> drawable.bitmap
        is VectorDrawable -> getBitmap(drawable)
        else -> throw IllegalArgumentException("unsupported drawable type")
    }
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
private fun getBitmap(vectorDrawable: VectorDrawable): Bitmap {
    val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
    vectorDrawable.draw(canvas)
    return bitmap
}

fun Context.dpFromPx(px: Float): Float {
    return px / resources.displayMetrics.density
}

fun Context.pxFromDp(dp: Float): Float {
    return dp * resources.displayMetrics.density
}
