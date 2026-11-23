package com.pushwoosh.pushmart.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.SheetPushPrimerBinding
import com.pushwoosh.pushmart.services

/**
 * The Push Primer sheet in the PushMart theme, with the iOS copy. Its answer goes to
 * [PushPrimer.finish]: accept with `true`; Not now, back, a swipe down or any other dismissal with
 * `false`.
 */
class PushPrimerSheet : BottomSheetDialogFragment() {
    private val primer: PushPrimer
        get() = requireContext().services.pushPermission

    override fun getTheme(): Int = R.style.ThemeOverlay_PushMart_BottomSheet_Primer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.sheet_push_primer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Restored after the process or the activity was gone: nobody waits for this answer.
        if (!primer.isPrompting) {
            dismissAllowingStateLoss()
            return
        }
        val binding = SheetPushPrimerBinding.bind(view)
        binding.primerAccept.setOnClickListener { answer(true) }
        binding.primerDecline.setOnClickListener { answer(false) }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onDestroy() {
        if (activity?.isChangingConfigurations != true) primer.finish(false)
        super.onDestroy()
    }

    private fun answer(accepted: Boolean) {
        primer.finish(accepted)
        dismissAllowingStateLoss()
    }
}
