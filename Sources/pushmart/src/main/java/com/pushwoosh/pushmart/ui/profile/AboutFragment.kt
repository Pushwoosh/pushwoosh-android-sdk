package com.pushwoosh.pushmart.ui.profile

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentAboutBinding
import com.pushwoosh.pushmart.databinding.ViewPageRowBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.Wordmark

/**
 * Profile > About PushMart, as iOS `MonetizationView`: with Live order tracking while it is
 * available. Documentation only, no SDK calls.
 */
class AboutFragment : Fragment(R.layout.fragment_about) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentAboutBinding.bind(view)
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }
        Wordmark.size(binding.wordmark, 22f)
        binding.wordmark.wordmarkText.setTextColor(view.context.getColor(R.color.pm_on_brand))
        row(
            binding.priceAlertsRow,
            R.drawable.ic_bell_outline,
            R.string.about_price_alerts,
            R.string.about_price_alerts_detail)
        row(binding.dealsRow, R.drawable.ic_tag, R.string.about_deals, R.string.about_deals_detail)
        val live = services.liveOrders.isAvailable
        binding.subtitle.setText(
            if (live) R.string.about_subtitle_live else R.string.about_subtitle)
        binding.trackingRow.root.isVisible = live
        binding.trackingDivider.isVisible = live
        if (live) {
            row(
                binding.trackingRow,
                R.drawable.ic_tab_orders_outline,
                R.string.perk_tracking_title,
                R.string.about_perk_tracking_subtitle)
        }
    }

    private fun row(
        row: ViewPageRowBinding,
        @DrawableRes icon: Int,
        @StringRes title: Int,
        @StringRes detail: Int
    ) {
        row.pageRowIcon.setImageResource(icon)
        row.pageRowTitle.setText(title)
        row.pageRowDetail.setText(detail)
    }
}
