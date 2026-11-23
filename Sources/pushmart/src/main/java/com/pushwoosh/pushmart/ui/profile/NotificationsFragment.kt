package com.pushwoosh.pushmart.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentNotificationsBinding
import com.pushwoosh.pushmart.services

/**
 * Profile > Offers & order updates, as iOS `RegistrationView`: one switch that turns push on or
 * off.
 */
class NotificationsFragment : Fragment(R.layout.fragment_notifications) {
    private var binding: FragmentNotificationsBinding? = null
    private val onSwitch =
        CompoundButton.OnCheckedChangeListener { _, checked ->
            render(checked)
            services.accountManager.setPushEnabled(checked) { pushOn -> render(pushOn) }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentNotificationsBinding.bind(view)
        this.binding = binding
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }
        val row = binding.pushRow
        row.pageRowTitle.setText(R.string.push_notifications)
        row.pageRowSwitch.isVisible = true
        row.pageRowSwitch.contentDescription = getString(R.string.push_notifications)
        render(isPushOn())
    }

    override fun onResume() {
        super.onResume()
        render(isPushOn())
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun isPushOn(): Boolean =
        services.account.pushNotificationEnabled && services.pushPermission.isAllowed

    private fun render(on: Boolean) {
        val binding = binding ?: return
        val row = binding.pushRow
        binding.offNotice.isVisible = !on
        row.pageRowIcon.setImageResource(
            if (on) R.drawable.ic_notifications_active else R.drawable.ic_bell_off)
        row.pageRowDetail.setText(
            if (on) R.string.notifications_push_on else R.string.notifications_push_off)
        row.pageRowSwitch.setOnCheckedChangeListener(null)
        row.pageRowSwitch.isChecked = on
        row.pageRowSwitch.setOnCheckedChangeListener(onSwitch)
    }
}
