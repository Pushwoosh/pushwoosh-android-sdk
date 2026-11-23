package com.pushwoosh.pushmart.ui.common

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

object ProductGrid {
    private const val COLUMNS = 2
    private const val EDGE_DP = 14

    /**
     * Lays out [header] full width above [products] in [COLUMNS] columns, all in [recycler], so
     * only the visible cards are bound. Single-span items get [EDGE_DP] on their outer side, which
     * with the card's own margins gives the iOS 20dp edges and 12dp gap.
     *
     * The header is one view that is never notified, so RecyclerView keeps a single holder for it.
     */
    fun attach(recycler: RecyclerView, header: View, products: RecyclerView.Adapter<*>) {
        val layout = GridLayoutManager(recycler.context, COLUMNS)
        layout.spanSizeLookup =
            object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = if (position == 0) COLUMNS else 1
            }
        recycler.layoutManager = layout
        val edge = (EDGE_DP * recycler.resources.displayMetrics.density).roundToInt()
        recycler.addItemDecoration(OuterEdge(edge))
        recycler.adapter = ConcatAdapter(HeaderAdapter(header), products)
    }

    private class HeaderAdapter(private val header: View) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount(): Int = 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            object : RecyclerView.ViewHolder(header) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
    }

    private class OuterEdge(private val edge: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val params = view.layoutParams as? GridLayoutManager.LayoutParams ?: return
            if (params.spanSize != 1) return
            val rtl = parent.layoutDirection == View.LAYOUT_DIRECTION_RTL
            if ((params.spanIndex == 0) != rtl) outRect.left = edge else outRect.right = edge
        }
    }
}
