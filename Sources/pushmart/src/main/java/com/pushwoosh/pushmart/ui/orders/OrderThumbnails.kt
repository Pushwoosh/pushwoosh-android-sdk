package com.pushwoosh.pushmart.ui.orders

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Product
import com.pushwoosh.pushmart.ui.common.ProductImages
import kotlin.math.roundToInt

/** A row of portrait product photos for an order card, as iOS `OrderThumbnails`. */
object OrderThumbnails {
    private const val MAX_PHOTOS = 4
    private const val GAP_DP = 8
    private const val ICON_SHARE = 0.36f

    /**
     * Fills [row] with photos of [sizeDp] × [sizeDp]·4/3 once its parent is laid out: all of them
     * when they fit the parent's width, otherwise as many as fit (up to [MAX_PHOTOS]) next to a
     * "+N" tile for the rest. With no [products], it shows one placeholder tile with the orders
     * icon.
     */
    fun bind(row: LinearLayout, products: List<Product>, sizeDp: Int) {
        row.removeAllViews()
        (row.parent as View).doOnLayout { parent ->
            fill(row, products, sizeDp, parent.width - parent.paddingStart - parent.paddingEnd)
        }
    }

    private fun fill(row: LinearLayout, products: List<Product>, sizeDp: Int, available: Int) {
        val density = row.resources.displayMetrics.density
        val width = (sizeDp * density).roundToInt()
        val height = (sizeDp * 4 / 3 * density).roundToInt()
        val gap = (GAP_DP * density).roundToInt()
        val photos = photoCount(products.size, width, gap, available)
        val inflater = LayoutInflater.from(row.context)
        for (product in products.take(photos)) {
            val photo = inflater.inflate(R.layout.view_order_thumb, row, false) as ImageView
            ProductImages.bind(photo, product)
            add(row, photo, width, height, gap)
        }
        if (products.size > photos) {
            val more = inflater.inflate(R.layout.view_order_thumb_more, row, false) as TextView
            more.text = row.context.getString(R.string.order_thumbs_more, products.size - photos)
            add(row, more, width, height, gap)
        }
        if (products.isEmpty()) {
            val placeholder = ImageView(row.context)
            placeholder.setBackgroundResource(R.drawable.bg_tile_surface_hi)
            placeholder.setImageResource(R.drawable.ic_tab_orders_outline)
            placeholder.imageTintList =
                ColorStateList.valueOf(
                    ContextCompat.getColor(row.context, R.color.pm_text_secondary))
            placeholder.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            val inset = ((width - width * ICON_SHARE) / 2).roundToInt()
            placeholder.setPadding(inset, inset, inset, inset)
            add(row, placeholder, width, height, gap)
        }
    }

    private fun photoCount(count: Int, tile: Int, gap: Int, available: Int): Int {
        fun fits(tiles: Int) = tiles * tile + (tiles - 1) * gap <= available
        if (count <= MAX_PHOTOS && fits(count)) return count
        return (minOf(MAX_PHOTOS, count - 1) downTo 1).firstOrNull { fits(it + 1) } ?: 0
    }

    private fun add(row: LinearLayout, view: View, width: Int, height: Int, gap: Int) {
        val params = LinearLayout.LayoutParams(width, height)
        if (row.childCount > 0) params.marginStart = gap
        row.addView(view, params)
    }
}
