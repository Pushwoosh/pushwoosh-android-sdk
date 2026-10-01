package com.pushwoosh.pushmart.ui.promo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentSaleBinding

class SaleFragment : FullBleedFragment(R.layout.fragment_sale) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSaleBinding.bind(view)
        binding.close.setOnClickListener { findNavController().navigateUp() }
        binding.shopButton.setOnClickListener { findNavController().navigateUp() }
        binding.copy.setOnClickListener {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Promo code", binding.promoCode.text))
            binding.copy.text = "COPIED ✓"
        }
    }
}
