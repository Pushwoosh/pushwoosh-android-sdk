package com.pushwoosh.pushmart.ui.profile

import android.os.Bundle
import android.view.View
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentNotificationsBinding
import com.pushwoosh.pushmart.databinding.ViewPushSampleBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.ResultBus

class NotificationsFragment : Fragment(R.layout.fragment_notifications) {
    private var binding: FragmentNotificationsBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentNotificationsBinding.bind(view)
        this.binding = binding
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }

        sample(
            binding.sample1,
            "Order shipped 📦",
            "Your AeroKnit Runner is on the way — track it live.",
            "now")
        sample(
            binding.sample2,
            "Flash sale is live 🔥",
            "Members get up to 40% off for the next 3 hours.",
            "2m ago")
        sample(binding.sample3, "Back in stock", "Pulse Buds just returned in Midnight.", "1h ago")
        binding.rowPermission.detailLabel.text = "PERMISSION"
        binding.rowToken.detailLabel.text = "PUSH TOKEN"
        binding.rowHwid.detailLabel.text = "DEVICE ID"

        binding.pushSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == services.account.pushNotificationEnabled)
                return@setOnCheckedChangeListener
            setPushEnabled(checked)
        }
        binding.turnOn.setOnClickListener { setPushEnabled(true) }
        binding.reregister.setOnClickListener { setPushEnabled(true) }
        binding.unregister.setOnClickListener { setPushEnabled(false) }
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun setPushEnabled(on: Boolean) {
        services.accountManager.setPushEnabled(on) { outcome ->
            ResultBus.show(outcome)
            render()
        }
    }

    private fun render() {
        val binding = binding ?: return
        val enabled = services.account.pushNotificationEnabled
        binding.previews.alpha = if (enabled) 1f else 0.32f
        binding.previewsOff.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.statusIcon.setImageResource(
            if (enabled) R.drawable.ic_bell else R.drawable.ic_bell_off)
        binding.statusText.text = if (enabled) "On · you're all set" else "Off"
        if (binding.pushSwitch.isChecked != enabled) binding.pushSwitch.isChecked = enabled

        val granted = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        binding.rowPermission.detailValue.text = if (granted) "Granted" else "Denied"
        binding.rowPermission.detailValue.setTextColor(
            ContextCompat.getColor(
                requireContext(), if (granted) R.color.pm_success else R.color.pm_warning))
        binding.rowToken.detailValue.text =
            if (enabled) services.gateway.getPushToken() ?: "Not registered" else "Not registered"
        binding.rowHwid.detailValue.text = services.gateway.getHwid()
    }

    private fun sample(sample: ViewPushSampleBinding, title: String, body: String, time: String) {
        sample.sampleTitle.text = title
        sample.sampleBody.text = body
        sample.sampleTime.text = time
    }
}
