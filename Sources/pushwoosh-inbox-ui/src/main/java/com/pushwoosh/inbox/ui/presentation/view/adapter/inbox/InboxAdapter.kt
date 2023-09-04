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

package com.pushwoosh.inbox.ui.presentation.view.adapter.inbox

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.data.InboxMessageType
import com.pushwoosh.inbox.ui.databinding.PwItemInboxBinding
import com.pushwoosh.inbox.ui.presentation.view.adapter.BaseRecyclerAdapter
import com.pushwoosh.inbox.ui.presentation.view.adapter.ItemTouchHelperAdapter
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider


class InboxAdapter(context: Context,
                   private val colorSchemeProvider: ColorSchemeProvider,
                   attachmentClickListener: ((String, View) -> Unit)) : BaseRecyclerAdapter<BaseRecyclerAdapter.ViewHolder<InboxMessage>, InboxMessage>(context), ItemTouchHelperAdapter {

    companion object {
        const val TEXT_VIEW_TYPE = 0
    }

    var onItemRemoved: ((InboxMessage?) -> Unit)? = null
    var onItemStartSwipe: (() -> Unit)? = null
    var onItemStopSwipe: (() -> Unit)? = null
    var onItemClick: ((InboxMessage?) -> Unit)? = null
    var attachmentClickListener = attachmentClickListener
    private var lastPosition = -1

    override fun startSwipe() {
        onItemStartSwipe?.invoke()
    }

    override fun stopSwipe() {
        onItemStopSwipe?.invoke()
    }

    override fun onItemSwiped(position: Int) {
        onItemRemoved?.invoke(getItem(position))

        collection.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onBindViewHolder(holder: ViewHolder<InboxMessage>, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(getItem(position))
        }

    }

    override fun createViewHolderInstance(parent: ViewGroup, viewType: Int): ViewHolder<InboxMessage> = when (viewType) {
        TEXT_VIEW_TYPE -> {
            val inboxItemBinding = PwItemInboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            InboxViewHolder(binding = inboxItemBinding, adapter = this, colorSchemeProvider = colorSchemeProvider, attachmentClickListener = attachmentClickListener)
        }
        else -> throw IllegalArgumentException("Unknown type: $viewType")
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)?.type ?: InboxMessageType.PLAIN) {
        InboxMessageType.PLAIN,
        InboxMessageType.RICH_MEDIA,
        InboxMessageType.URL,
        InboxMessageType.DEEP_LINK -> TEXT_VIEW_TYPE
    }
}