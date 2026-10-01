package com.pushwoosh.pushmart.ui.onboarding

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.account.AppCode
import com.pushwoosh.pushmart.databinding.FragmentConnectBinding
import com.pushwoosh.pushmart.services

class ConnectFragment : Fragment(R.layout.fragment_connect) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentConnectBinding.bind(view)
        binding.header.stepTitle.text = "Connect your store"
        binding.header.stepSubtitle.text =
            "Enter your store access code to load your catalog and offers."

        if (savedInstanceState == null && binding.codeInput.text.isEmpty()) {
            binding.codeInput.setText(services.account.appCode)
        }
        binding.codeInput.addTextChangedListener(
            object : TextWatcher {
                private var applying = false

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable) {
                    if (applying) return
                    val masked = AppCode.mask(s.toString())
                    if (masked != s.toString()) {
                        applying = true
                        s.replace(0, s.length, masked)
                        applying = false
                    }
                    binding.codeError.visibility = View.GONE
                }
            })
        binding.useDemo.setOnClickListener { binding.codeInput.setText(AppCode.DEMO) }
        binding.continueButton.setOnClickListener {
            val error = services.accountManager.connect(binding.codeInput.text.toString())
            if (error != null) {
                binding.codeError.text = error
                binding.codeError.visibility = View.VISIBLE
            } else {
                findNavController().navigate(R.id.accountFragment)
            }
        }
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
    }
}
