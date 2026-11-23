package com.pushwoosh.pushmart.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentOnboardingBinding
import com.pushwoosh.pushmart.databinding.ViewPerkBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.Wordmark
import com.pushwoosh.pushmart.ui.common.setPushMartGraph

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentOnboardingBinding.bind(view)
        Wordmark.size(binding.wordmark, 22f)
        binding.wordmark.wordmarkText.setTextColor(view.context.getColor(R.color.pm_on_brand))
        perk(
            binding.perkTracking,
            R.drawable.ic_tab_orders_outline,
            R.string.perk_tracking_title,
            R.string.perk_tracking_subtitle)
        perk(
            binding.perkAlerts,
            R.drawable.ic_perk_alerts,
            R.string.perk_alerts_title,
            R.string.perk_alerts_subtitle)
        perk(
            binding.perkOffers,
            R.drawable.ic_perk_offers,
            R.string.perk_offers_title,
            R.string.perk_offers_subtitle)
        binding.onboardingGetStarted.setOnClickListener {
            services.account.onboarded = true
            // Home must stay the start, as after recreation, or tab popUpTo/restoreState break.
            findNavController().setPushMartGraph(onboarded = true)
        }
    }

    private fun perk(
        perk: ViewPerkBinding,
        @DrawableRes icon: Int,
        @StringRes title: Int,
        @StringRes subtitle: Int
    ) {
        perk.perkIcon.setImageResource(icon)
        perk.perkTitle.setText(title)
        perk.perkSubtitle.setText(subtitle)
    }
}
