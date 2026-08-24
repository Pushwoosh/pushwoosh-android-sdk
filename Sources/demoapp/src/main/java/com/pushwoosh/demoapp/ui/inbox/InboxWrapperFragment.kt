package com.pushwoosh.demoapp.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.pushwoosh.sampleapp.R
import com.pushwoosh.function.Callback
import com.pushwoosh.function.Result
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.exception.InboxMessagesException
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.presentation.view.fragment.InboxFragment

class InboxWrapperFragment : Fragment() {

    private var meta: TextView? = null
    private var rail: LinearLayout? = null
    private var railRead: View? = null
    private var railUnread: View? = null

    private var totalCount = 0
    private var unreadCount = 0

    private val totalCountCallback =
        Callback<Int, InboxMessagesException> { result ->
            totalCount = result.countOrZero()
            renderCounts()
        }

    private val unreadCountCallback =
        Callback<Int, InboxMessagesException> { result ->
            unreadCount = result.countOrZero()
            renderCounts()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PushwooshInboxStyle.showToolbar = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_inbox_wrapper, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        meta = view.findViewById(R.id.inbox_meta)
        rail = view.findViewById(R.id.inbox_rail)
        railRead = view.findViewById(R.id.inbox_rail_read)
        railUnread = view.findViewById(R.id.inbox_rail_unread)

        if (childFragmentManager.findFragmentById(R.id.inbox_container) == null) {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.inbox_container, InboxFragment())
                .commitNow()
        }

        PushwooshInbox.registerMessagesCountObserver(totalCountCallback)
        PushwooshInbox.registerUnreadMessagesCountObserver(unreadCountCallback)
    }

    override fun onDestroyView() {
        PushwooshInbox.unregisterMessagesCountObserver(totalCountCallback)
        PushwooshInbox.unregisterUnreadMessagesCountObserver(unreadCountCallback)
        meta = null
        rail = null
        railRead = null
        railUnread = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        PushwooshInboxStyle.showToolbar = false
        PushwooshInbox.messagesCount(totalCountCallback)
        PushwooshInbox.unreadMessagesCount(unreadCountCallback)
    }

    override fun onPause() {
        super.onPause()
        PushwooshInboxStyle.showToolbar = null
    }

    private fun renderCounts() {
        val meta = meta ?: return
        val rail = rail ?: return

        val unread = unreadCount.coerceAtMost(totalCount)
        val read = totalCount - unread

        meta.text =
            when {
                totalCount == 0 -> getString(R.string.inbox_meta_empty)
                unread == 0 -> getString(R.string.inbox_meta_all_read, totalCount)
                else -> getString(R.string.inbox_meta_counts, totalCount, unread)
            }

        if (totalCount == 0) {
            rail.visibility = View.GONE
            return
        }

        rail.visibility = View.VISIBLE
        railRead?.setWeight(read.toFloat())
        railUnread?.setWeight(unread.toFloat())
    }

    private fun View.setWeight(weight: Float) {
        val params = layoutParams as LinearLayout.LayoutParams
        params.weight = weight
        layoutParams = params
    }

    private fun Result<Int, InboxMessagesException>.countOrZero(): Int =
        if (isSuccess) data ?: 0 else 0
}
