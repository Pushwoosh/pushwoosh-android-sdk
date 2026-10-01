package com.pushwoosh.pushmart.ui.offers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.account.AccountOutcome
import com.pushwoosh.pushmart.databinding.SheetOffersActionsBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.ResultBus

class OffersActionsSheet : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.sheet_offers_actions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = SheetOffersActionsBinding.bind(view)
        val actions = OffersActions(services.inbox)
        binding.markAllRead.setOnClickListener { actions.markAllRead(::finish) }
        binding.clearRead.setOnClickListener { actions.clearRead(::finish) }
        binding.deleteAll.setOnClickListener { actions.deleteAll(::finish) }
    }

    private fun finish(outcome: AccountOutcome) {
        ResultBus.show(outcome)
        if (isAdded) dismissAllowingStateLoss()
    }
}
