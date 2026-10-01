package com.pushwoosh.pushmart.ui.playground

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentPlaygroundBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.ResultBus

class PlaygroundFragment : Fragment(R.layout.fragment_playground) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentPlaygroundBinding.bind(view)
        val playground = Playground(services.gateway)
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }
        binding.postEvent.setOnClickListener {
            ResultBus.show(
                playground.postEvent(
                    binding.eventName.text.toString(),
                    binding.attrKey.text.toString(),
                    binding.attrValue.text.toString()))
        }
        binding.setTag.setOnClickListener {
            ResultBus.show(
                playground.setTag(binding.tagKey.text.toString(), binding.tagValue.text.toString()))
        }
    }
}
