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

package com.pushwoosh.inbox.ui.presentation.view.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import com.pushwoosh.inbox.ui.utils.getBitmap
import com.pushwoosh.inbox.ui.utils.pxFromDp


class SimpleItemTouchHelperCallback(private val adapter: ItemTouchHelperAdapter,
                                    context: Context?,
                                    colorSchemeProvider: ColorSchemeProvider,
                                    @DrawableRes swipeIcon: Int = R.drawable.inbox_ic_delete) : ItemTouchHelper.Callback() {
    private var touchable = true
    private val icon = getBitmap(context, swipeIcon)
    private val paint = Paint()

    init {
        paint.color = colorSchemeProvider.accentColor
    }

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = true

    override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
        adapter.onItemSwiped(viewHolder.adapterPosition)
    }

    override fun onChildDraw(canvas: Canvas, recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        when (actionState) {
            ItemTouchHelper.ACTION_STATE_SWIPE -> {
                if (icon == null) {
                    return
                }

                updateState(dX, dY, isCurrentlyActive, viewHolder)

                val itemView = viewHolder.itemView
                val height = itemView.bottom.toFloat() - itemView.top.toFloat()
                val leftMargin = itemView.context.pxFromDp(20f)

                val topMargin = if (height > icon.height) {
                    height / 2 - icon.height / 2f
                } else {
                    height / 3
                }

                val iconDest = if (dX > 0) {
                    val background = RectF(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat())
                    canvas.drawRect(background, paint)
                    RectF(itemView.left.toFloat() + leftMargin, itemView.top.toFloat() + topMargin, itemView.left.toFloat() + icon.width + leftMargin, itemView.bottom.toFloat() - topMargin)
                } else {
                    val background = RectF(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                    canvas.drawRect(background, paint)
                    RectF(itemView.right.toFloat() - leftMargin - icon.width, itemView.top.toFloat() + topMargin, itemView.right.toFloat() - leftMargin, itemView.bottom.toFloat() - topMargin)
                }

                canvas.drawBitmap(icon, null, iconDest, paint)
            }
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun updateState(dX: Float, dY: Float, isCurrentlyActive: Boolean, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
        if (dX == 0f && dY == 0f) {
            if (isCurrentlyActive) {
                adapter.startSwipe()
            } else {
                adapter.stopSwipe()
            }
        }

        if ((dX == viewHolder.itemView.width.toFloat() || dY == viewHolder.itemView.height.toFloat()) && !isCurrentlyActive) {
            adapter.stopSwipe()
        }
    }

    override fun getMovementFlags(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder): Int {
        if (!touchable) {
            return 0
        }

        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return ItemTouchHelper.Callback.makeMovementFlags(0, swipeFlags)
    }

    override fun onMove(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, target: androidx.recyclerview.widget.RecyclerView.ViewHolder): Boolean =
            false

    fun setTouchable(touchable: Boolean) {
        this.touchable = touchable
    }
}
