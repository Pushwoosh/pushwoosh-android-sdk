package com.pushwoosh.pushmart.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentWelcomeBinding
import com.pushwoosh.pushmart.databinding.ViewPerkBinding

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentWelcomeBinding.bind(view)
        binding.wordmark.wordmarkText.textSize = 40f
        perk(
            binding.perkDelivery,
            R.drawable.ic_bolt,
            "Same-day delivery",
            "Track every order live on your Lock Screen")
        perk(
            binding.perkDrops,
            R.drawable.ic_sparkles,
            "Members-only drops",
            "Be first when limited items land")
        perk(
            binding.perkDeals,
            R.drawable.ic_tag,
            "Personalized deals",
            "Offers tuned to what you love")
        binding.getStarted.setOnClickListener { findNavController().navigate(R.id.connectFragment) }
    }

    private fun perk(perk: ViewPerkBinding, icon: Int, title: String, subtitle: String) {
        perk.perkIcon.setImageResource(icon)
        perk.perkTitle.text = title
        perk.perkSubtitle.text = subtitle
    }
}
