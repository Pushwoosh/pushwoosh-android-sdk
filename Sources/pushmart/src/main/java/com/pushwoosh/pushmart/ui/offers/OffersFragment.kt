package com.pushwoosh.pushmart.ui.offers

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.PopupWindow
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.pushwoosh.inbox.ui.PushwooshInboxUi
import com.pushwoosh.inbox.ui.R as InboxR
import com.pushwoosh.inbox.ui.model.repository.InboxEvent
import com.pushwoosh.inbox.ui.model.repository.InboxRepository
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentOffersBinding
import com.pushwoosh.pushmart.sdk.Registration
import com.pushwoosh.pushmart.services

class OffersFragment : Fragment(R.layout.fragment_offers) {
    private var binding: FragmentOffersBinding? = null
    private var counts: Registration? = null
    private var menu: PopupWindow? = null
    private var total: Int? = null
    private var unread = 0
    /**
     * Null until the feed's first load result: the count observers can't tell an empty inbox from a
     * failed sync.
     */
    private var syncFailed: Boolean? = null
    private val feedEvents: (InboxEvent) -> Unit = { event ->
        when (event) {
            is InboxEvent.FailedLoading -> onSynced(true)
            is InboxEvent.SuccessLoading,
            is InboxEvent.InboxEmpty -> onSynced(false)
            else -> Unit
        }
    }

    override fun onAttach(context: Context) {
        InboxStyling.apply(context)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.registerFragmentLifecycleCallbacks(FeedClearance, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentOffersBinding.bind(view)
        this.binding = binding
        if (childFragmentManager.findFragmentById(R.id.inbox_container) == null) {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.inbox_container, PushwooshInboxUi.createInboxFragment())
                .commitNow()
        }
        val actions = OffersActions(services.inbox)
        binding.more.setOnClickListener { menu = InboxMenu.show(it, actions, ::onSynced) }
        binding.emptyRefresh.setOnClickListener { actions.refresh(::onSynced) }
        counts =
            services.inbox.observeCounts { total, unread ->
                this.total = total
                this.unread = unread
                render()
            }
        InboxRepository.addCallback(feedEvents)
    }

    override fun onDestroyView() {
        InboxRepository.removeCallback(feedEvents)
        menu?.dismiss()
        menu = null
        counts?.unregister()
        counts = null
        binding = null
        super.onDestroyView()
    }

    private fun onSynced(failed: Boolean) {
        syncFailed = failed
        render()
    }

    /** As on iOS, the empty overlay needs an empty inbox and a sync without error. */
    private fun render() {
        val binding = binding ?: return
        binding.subtitle.text = InboxCounter.subtitle(resources, unread)
        binding.empty.visibility =
            if (total == 0 && syncFailed == false) View.VISIBLE else View.GONE
    }

    /** Lets the SDK feed scroll under the floating tab bar, as iOS `tabBarClearance` does. */
    private object FeedClearance : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(
            fm: FragmentManager,
            f: Fragment,
            v: View,
            savedInstanceState: Bundle?,
        ) {
            val feed = v.findViewById<RecyclerView>(InboxR.id.inboxRecyclerView) ?: return
            feed.clipToPadding = false
            feed.updatePadding(bottom = v.resources.getDimensionPixelSize(R.dimen.pm_tab_clearance))
        }
    }
}
