package com.pushwoosh.pushmart.ui.common

import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.pushwoosh.pushmart.R

object Hearts {
    fun bind(view: ImageView, saved: Boolean) {
        view.setImageResource(if (saved) R.drawable.ic_heart else R.drawable.ic_heart_outline)
        view.setBackgroundResource(
            if (saved) R.drawable.bg_circle_white else R.drawable.bg_circle_black_22)
        view.imageTintList =
            ContextCompat.getColorStateList(
                view.context, if (saved) R.color.pm_coral else R.color.pm_white)
        view.contentDescription = if (saved) "Remove from wishlist" else "Save to wishlist"
    }
}
