package com.pushwoosh.pushmart.ui.common

import android.widget.ImageView
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Product

object ProductImages {
    fun bind(view: ImageView, product: Product) {
        val context = view.context
        val id = context.resources.getIdentifier(product.imageName, "drawable", context.packageName)
        if (id != 0) {
            view.setImageResource(id)
            view.background = null
        } else {
            view.setBackgroundColor(product.tintArgb)
            view.setImageResource(R.drawable.ic_bag)
        }
    }
}
