package com.pushwoosh.pushmart.ui.profile

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pushwoosh.pushmart.BuildConfig
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentProfileBinding
import com.pushwoosh.pushmart.databinding.ViewProfileRowBinding
import com.pushwoosh.pushmart.sdk.Registration
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.PageSheet

/** The account hub as iOS `ProfileTabView`: identity, shortcuts, personal code, then settings. */
class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private var binding: FragmentProfileBinding? = null
    private var counts: Registration? = null
    private val onSwitchChanged: () -> Unit = { render() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentProfileBinding.bind(view)
        this.binding = binding
        binding.avatarPhoto.clipToOutline = true
        binding.identity.setOnClickListener {
            findNavController().navigate(R.id.yourDetailsFragment)
        }
        binding.shortcutOrders.setOnClickListener { selectTab(R.id.ordersFragment) }
        binding.shortcutOffers.setOnClickListener { selectTab(R.id.offersFragment) }
        val openCodeSheet =
            View.OnClickListener {
                PageSheet.showOnce(
                    parentFragmentManager, PersonalCodeSheet(), PersonalCodeSheet.TAG)
            }
        binding.personalCodeCard.setOnClickListener(openCodeSheet)
        binding.personalCodeAction.setOnClickListener(openCodeSheet)

        row(
            binding.rowDetails,
            R.drawable.ic_person_outline,
            R.string.profile_details,
            R.string.profile_details_subtitle,
            R.id.yourDetailsFragment)
        row(
            binding.rowPreferences,
            R.drawable.ic_tune,
            R.string.profile_preferences,
            R.string.profile_preferences_subtitle,
            R.id.preferencesFragment)
        row(
            binding.rowNotifications,
            R.drawable.ic_bell_outline,
            R.string.profile_notifications,
            R.string.profile_notifications_subtitle,
            R.id.notificationsFragment)
        row(
            binding.rowReminders,
            R.drawable.ic_alarm,
            R.string.reminders_title,
            R.string.profile_reminders_subtitle,
            R.id.remindersFragment)
        row(
            binding.rowAppSettings,
            R.drawable.ic_settings,
            R.string.settings_title,
            R.string.profile_settings_subtitle,
            R.id.appSettingsFragment)
        row(
            binding.rowAbout,
            R.drawable.ic_info,
            R.string.about_pushmart,
            R.string.profile_about_subtitle,
            R.id.aboutFragment)
        binding.developerSection.isVisible = BuildConfig.DEBUG
        if (BuildConfig.DEBUG) {
            row(
                binding.rowInapps,
                R.drawable.ic_forum,
                R.string.native_inapps,
                R.string.profile_inapps_subtitle,
                R.id.inAppShowcaseFragment)
            row(
                binding.rowDeveloperTools,
                R.drawable.ic_construction,
                R.string.developer_tools,
                R.string.profile_developer_tools_subtitle,
                R.id.developerToolsFragment)
        }
        binding.version.text = getString(R.string.profile_version, BuildConfig.VERSION_NAME)

        services.accountManager.addSwitchListener(onSwitchChanged)
        counts = services.inbox.observeCounts { _, unread -> renderOffers(unread) }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onDestroyView() {
        counts?.unregister()
        counts = null
        services.accountManager.removeSwitchListener(onSwitchChanged)
        binding = null
        super.onDestroyView()
    }

    private fun render() {
        val binding = binding ?: return
        val account = services.account
        val email = account.userEmail
        binding.profileEmail.text = email.ifEmpty { getString(R.string.profile_email_placeholder) }
        renderAvatar(binding, email)

        val code = account.personalCode
        val guest = code == null
        val context = binding.root.context
        binding.personalCodeIcon.setColorFilter(
            context.getColor(if (guest) R.color.pm_coral else R.color.pm_text_primary))
        binding.personalCodeSubtitle.text =
            if (guest) getString(R.string.personal_code_hint)
            else getString(R.string.personal_code_applied, code)
        binding.personalCodeAction.setText(
            if (guest) R.string.personal_code_enter else R.string.personal_code_change)
        binding.personalCodeAction.setBackgroundResource(
            if (guest) R.drawable.bg_pill_coral else R.drawable.bg_pill_surface)
        binding.personalCodeAction.setTextColor(
            context.getColor(if (guest) R.color.pm_white else R.color.pm_text_primary))
    }

    /** The photo, otherwise the email's first letter, otherwise the person glyph. */
    private fun renderAvatar(binding: FragmentProfileBinding, email: String) {
        val photo = services.avatar.load()
        val initial = email.firstOrNull()?.uppercase()
        binding.avatarPhoto.setImageBitmap(photo)
        binding.avatarPhoto.isVisible = photo != null
        binding.avatarInitial.text = initial
        binding.avatarInitial.isVisible = photo == null && initial != null
        binding.avatarPerson.isVisible = photo == null && initial == null
    }

    private fun renderOffers(unread: Int) {
        val binding = binding ?: return
        binding.offersDetail.text =
            if (unread <= 0) getString(R.string.profile_offers_detail)
            else resources.getQuantityString(R.plurals.inbox_new_offers, unread, unread)
    }

    private fun row(
        binding: ViewProfileRowBinding,
        @DrawableRes icon: Int,
        @StringRes title: Int,
        @StringRes subtitle: Int,
        @IdRes destination: Int,
    ) {
        binding.rowIcon.setImageResource(icon)
        binding.rowTitle.setText(title)
        binding.rowSubtitle.setText(subtitle)
        binding.root.setOnClickListener { findNavController().navigate(destination) }
    }

    private fun selectTab(@IdRes id: Int) {
        requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId = id
    }
}
