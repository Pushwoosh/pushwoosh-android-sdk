package com.pushwoosh.pushmart.ui.promo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentVoucherBinding

class VoucherFragment : FullBleedFragment(R.layout.fragment_voucher) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentVoucherBinding.bind(view)
        val code = requireArguments().getString(ARG_CODE).orEmpty()
        binding.voucherCode.text = code
        binding.close.setOnClickListener { findNavController().navigateUp() }
        binding.shopButton.setOnClickListener { findNavController().navigateUp() }
        binding.copy.setOnClickListener {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Voucher", code))
            binding.copy.text = "COPIED"
        }
    }

    companion object {
        const val ARG_CODE = "code"
    }
}
