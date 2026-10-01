package com.pushwoosh.pushmart.ui.common

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pushwoosh.pushmart.R

object Chips {
    fun make(parent: ViewGroup, text: String, selected: Boolean = false): TextView {
        val chip = TextView(parent.context, null, 0, R.style.Widget_PushMart_Chip)
        chip.text = text
        chip.isSelected = selected
        val spacing = (8 * parent.resources.displayMetrics.density).toInt()
        val params =
            ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.marginEnd = spacing
        params.bottomMargin = spacing
        chip.layoutParams = params
        return chip
    }

    fun selectOnly(group: ViewGroup, chip: View) {
        for (i in 0 until group.childCount) group.getChildAt(i).isSelected =
            group.getChildAt(i) === chip
    }
}
