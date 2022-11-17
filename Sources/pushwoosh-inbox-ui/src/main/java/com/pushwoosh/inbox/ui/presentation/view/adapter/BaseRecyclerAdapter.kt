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
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import com.pushwoosh.inbox.ui.utils.clear

abstract class BaseRecyclerAdapter<VH : BaseRecyclerAdapter.ViewHolder<Model>, Model>(val context: Context) : androidx.recyclerview.widget.RecyclerView.Adapter<VH>() {
    private val mDataList = ArrayList<Model>()

    protected var mOnItemClickListener: ((View, Int) -> Unit) = { _, _ -> }

    protected val duration = 250L
    protected val interpolator: Interpolator = LinearInterpolator()

    private var lastPosition = -1

    fun setOnItemClickListener(onItemClickListener: (View, Int) -> Unit) {
        mOnItemClickListener = onItemClickListener
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vh = createViewHolderInstance(parent, viewType)
        vh.onCreate()
        return vh
    }

    protected abstract fun createViewHolderInstance(parent: ViewGroup, viewType: Int): VH

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bindView()

        holder.fillView(item, position)

        if (position > lastPosition) {
            animateItem(holder, position)
            lastPosition = position
        } else {
            holder.itemView.clear()
        }
    }

    protected open fun animateItem(holder: VH, position: Int) {}

    override fun getItemCount(): Int {
        return mDataList.size
    }

    fun setCollection(list: Collection<Model>?) {
        mDataList.clear()
        if (list != null) {
            mDataList.addAll(list)
        }
        notifyDataSetChanged()
    }

    fun addCollection(list: Collection<Model>?) {
        if (list != null) {
            mDataList.addAll(list)
        }
        notifyDataSetChanged()
    }

    val collection: MutableList<Model>
        get() = mDataList

    open fun getItem(position: Int): Model? {
        return mDataList[position]
    }

    fun setItem(position: Int, item: Model) {
        mDataList[position] = item
        notifyItemChanged(position)
    }

    fun clearCollection() {
        mDataList.clear()
        notifyDataSetChanged()
    }

    fun addItem(position: Int, model: Model) {
        mDataList.add(position, model)
        notifyItemInserted(position)
    }

    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)
        holder.onAttach()
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        super.onViewDetachedFromWindow(holder)
        holder.onDetach()
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holder.recycled()
    }

    abstract class ViewHolder<Model>(view: View, private var adapter: BaseRecyclerAdapter<*, *>) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view), View.OnClickListener {

        constructor(@LayoutRes layoutId: Int, parent: ViewGroup, adapter: BaseRecyclerAdapter<*, *>) : this(LayoutInflater.from(parent.context).inflate(layoutId, parent, false), adapter)

        fun onCreate() {
        }

        fun bindView() {
            itemView.setOnClickListener(this)
        }

        abstract fun fillView(model: Model?, position: Int)

        protected val context: Context
            get() = itemView.context

        override fun onClick(v: View) {
            adapter.mOnItemClickListener.invoke(v, adapterPosition)
        }

        internal fun onAttach() {
        }

        internal fun onDetach() {
        }

        internal fun recycled() {
        }
    }
}