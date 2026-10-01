package com.pushwoosh.pushmart.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentYourDetailsBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.ResultBus

class YourDetailsFragment : Fragment(R.layout.fragment_your_details) {
    private var binding: FragmentYourDetailsBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentYourDetailsBinding.bind(view)
        this.binding = binding
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }
        if (savedInstanceState == null) {
            binding.nameInput.setText(services.account.userName)
            binding.memberIdInput.setText(services.gateway.getUserId().orEmpty())
            binding.emailInput.setText(services.account.userEmail)
        }
        binding.save.setOnClickListener {
            services.accountManager.saveDetails(
                binding.nameInput.text.toString(),
                binding.memberIdInput.text.toString(),
                binding.emailInput.text.toString()) { outcome ->
                    ResultBus.show(outcome)
                    renderCard()
                }
        }
        renderCard()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun renderCard() {
        val binding = binding ?: return
        val memberId = services.gateway.getUserId().orEmpty()
        binding.memberId.text = memberId.ifEmpty { "—— —— ——" }
        binding.cardEmail.text = services.account.userEmail.ifEmpty { "not set" }
    }
}
