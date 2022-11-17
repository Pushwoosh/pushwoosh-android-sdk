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

package com.pushwoosh.inbox.ui.presentation.view.style

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.graphics.drawable.Drawable
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.os.Build

fun getAdaptiveRippleDrawable(normalColor: Int, pressedColor: Int): Drawable {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        RippleDrawable(ColorStateList.valueOf(pressedColor),
                ColorDrawable(normalColor), getRippleMask(normalColor))
    } else {
        getStateListDrawable(normalColor, pressedColor)
    }
}

private fun getRippleMask(color: Int): Drawable {
    val outerRadii = FloatArray(8, { _ -> 3f })// 3 is radius of final ripple, instead of 3 you can give required final radius
    val r = RoundRectShape(outerRadii, null, null)

    val shapeDrawable = ShapeDrawable(r)
    shapeDrawable.paint.color = color

    return shapeDrawable
}

fun getStateListDrawable(normalColor: Int, pressedColor: Int): StateListDrawable {
    val states = StateListDrawable()
    states.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(pressedColor))
    states.addState(intArrayOf(android.R.attr.state_focused), ColorDrawable(pressedColor))
    states.addState(intArrayOf(android.R.attr.state_activated), ColorDrawable(pressedColor))
    states.addState(intArrayOf(), ColorDrawable(normalColor))
    return states
}
