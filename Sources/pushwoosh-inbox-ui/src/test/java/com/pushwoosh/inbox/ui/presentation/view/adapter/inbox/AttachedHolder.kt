/*
 *
 * Copyright (c) 2026. Pushwoosh Inc. (http://www.pushwoosh.com)
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

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.presentation.view.adapter.BaseRecyclerAdapter
import org.robolectric.RuntimeEnvironment

/**
 * Lays [adapter] out in a real RecyclerView and returns the holder shown at [position]. A row tap
 * resolves its message through `adapterPosition`, which only an attached holder carries.
 */
@Suppress("UNCHECKED_CAST")
internal fun attachedHolder(adapter: InboxAdapter, position: Int = 0): BaseRecyclerAdapter.ViewHolder<InboxMessage> {
    val context = RuntimeEnvironment.getApplication()
    val recyclerView = RecyclerView(context)
    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.adapter = adapter
    recyclerView.measure(
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(10000, View.MeasureSpec.EXACTLY)
    )
    recyclerView.layout(0, 0, 1080, 10000)
    return recyclerView.findViewHolderForAdapterPosition(position) as BaseRecyclerAdapter.ViewHolder<InboxMessage>
}
