package com.pushwoosh.pushmart.ui.inapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.inapp.ui.PushwooshInAppUi
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentInappShowcaseBinding
import com.pushwoosh.pushmart.databinding.ViewShowcaseButtonBinding

class InAppShowcaseFragment : Fragment(R.layout.fragment_inapp_showcase) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentInappShowcaseBinding.bind(view)
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }
        val inflater = LayoutInflater.from(requireContext())
        val presets = InAppPresets(requireContext().packageName)
        for (preset in presets.all) {
            val button = ViewShowcaseButtonBinding.inflate(inflater, binding.buttons, false).root
            button.text = preset.label
            button.contentDescription = "Show ${preset.label}"
            button.setOnClickListener { PushwooshInAppUi.present(preset.config) }
            binding.buttons.addView(button)
        }
    }
}
