package com.pushwoosh.pushmart.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentAccountBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.ResultBus

class AccountFragment : Fragment(R.layout.fragment_account) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentAccountBinding.bind(view)
        binding.header.stepPill2.setBackgroundResource(R.drawable.bg_pill_brand)
        binding.header.stepTitle.text = "Create your account"
        binding.header.stepSubtitle.text =
            "Sign in so your cart, orders and rewards follow you everywhere."

        binding.continueButton.setOnClickListener {
            services.accountManager.signIn(
                binding.nameInput.text.toString(), binding.emailInput.text.toString()) { error ->
                    ResultBus.fail("Sign-in failed", error)
                }
            finish()
        }
        binding.guestButton.setOnClickListener {
            services.accountManager.continueAsGuest()
            finish()
        }
    }

    private fun finish() {
        val options = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
        findNavController().navigate(R.id.homeFragment, null, options)
    }
}
