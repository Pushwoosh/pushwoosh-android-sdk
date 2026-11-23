package com.pushwoosh.pushmart.ui.common

import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.pushwoosh.pushmart.R

object Hearts {
    fun bind(view: ImageView, saved: Boolean) {
        view.setImageResource(if (saved) R.drawable.ic_heart else R.drawable.ic_heart_outline)
        view.setBackgroundResource(R.drawable.bg_circle_glass_light)
        view.imageTintList =
            ContextCompat.getColorStateList(
                view.context, if (saved) R.color.pm_coral else R.color.pm_on_brand)
        view.contentDescription =
            view.context.getString(if (saved) R.string.wishlist_remove else R.string.wishlist_save)
    }
}
