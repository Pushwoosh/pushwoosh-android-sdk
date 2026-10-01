package com.pushwoosh.pushmart.ui.offers

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.pushwoosh.inbox.ui.PushwooshInboxUi
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentOffersBinding
import com.pushwoosh.pushmart.sdk.Registration
import com.pushwoosh.pushmart.services

class OffersFragment : Fragment(R.layout.fragment_offers) {
    private var binding: FragmentOffersBinding? = null
    private var counts: Registration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InboxStyling.apply(requireContext())
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
        binding.actions.setOnClickListener {
            OffersActionsSheet().show(parentFragmentManager, "offers-actions")
        }
        counts = services.inbox.observeCounts { total, unread -> render(total, unread) }
    }

    override fun onResume() {
        super.onResume()
        InboxStyling.apply(requireContext())
        services.inbox.fetchCounts { total, unread -> render(total, unread) }
    }

    override fun onDestroyView() {
        counts?.unregister()
        counts = null
        binding = null
        super.onDestroyView()
    }

    private fun render(total: Int, unread: Int) {
        binding?.counter?.text = InboxCounter.format(total, unread)
    }
}
