package com.pushwoosh.pushmart.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButtonToggleGroup
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.core.ThemePreference
import com.pushwoosh.pushmart.databinding.FragmentAppSettingsBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.ResultBus

/**
 * Profile > App settings, as iOS `SettingsView`: the System / Light / Dark appearance and the Show
 * alerts in-app switch.
 */
class AppSettingsFragment : Fragment(R.layout.fragment_app_settings) {
    private var binding: FragmentAppSettingsBinding? = null

    private val onThemeChecked =
        MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@OnButtonCheckedListener
            val preference =
                when (checkedId) {
                    R.id.theme_light -> ThemePreference.LIGHT
                    R.id.theme_dark -> ThemePreference.DARK
                    else -> ThemePreference.SYSTEM
                }
            val theme = services.theme
            if (theme.preference == preference) return@OnButtonCheckedListener
            theme.preference = preference
            theme.apply()
        }

    private val onAlertsSwitch =
        CompoundButton.OnCheckedChangeListener { _, on ->
            services.foregroundAlerts.set(on)
            ResultBus.success(
                getString(if (on) R.string.settings_alerts_on else R.string.settings_alerts_off))
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentAppSettingsBinding.bind(view)
        this.binding = binding
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }
        val row = binding.alertsRow
        row.pageRowIcon.setImageResource(R.drawable.ic_app_badging)
        row.pageRowTitle.setText(R.string.settings_show_alerts)
        row.pageRowDetail.setText(R.string.settings_show_alerts_detail)
        row.pageRowSwitch.isVisible = true
        row.pageRowSwitch.contentDescription = getString(R.string.settings_show_alerts)
        render()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun render() {
        val binding = binding ?: return
        val checked =
            when (services.theme.preference) {
                ThemePreference.SYSTEM -> R.id.theme_system
                ThemePreference.LIGHT -> R.id.theme_light
                ThemePreference.DARK -> R.id.theme_dark
            }
        binding.appearance.removeOnButtonCheckedListener(onThemeChecked)
        binding.appearance.check(checked)
        binding.appearance.addOnButtonCheckedListener(onThemeChecked)

        val switch = binding.alertsRow.pageRowSwitch
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = services.foregroundAlerts.enabled
        switch.setOnCheckedChangeListener(onAlertsSwitch)
    }
}
