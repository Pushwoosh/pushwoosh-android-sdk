package com.pushwoosh.pushmart.ui.common

import android.widget.ImageView
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Product

object ProductImages {
    fun bind(view: ImageView, product: Product) {
        view.setBackgroundColor(product.tintArgb)
        val context = view.context
        val id = context.resources.getIdentifier(product.imageName, "drawable", context.packageName)
        view.setImageResource(if (id != 0) id else R.drawable.ic_bag)
    }
}
