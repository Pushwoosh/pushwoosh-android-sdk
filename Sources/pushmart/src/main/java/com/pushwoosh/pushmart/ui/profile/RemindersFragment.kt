package com.pushwoosh.pushmart.ui.profile

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentRemindersBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.DurationWheel
import com.pushwoosh.pushmart.ui.common.ResultBus
import kotlin.math.roundToInt

/**
 * Profile > Reminders, as iOS `NotificationsView`: pick what to be reminded about and when, and
 * PushMart schedules a local notification.
 */
class RemindersFragment : Fragment(R.layout.fragment_reminders) {
    private enum class Reminder(val chip: Int, val icon: Int, val title: Int, val body: Int) {
        CART(
            R.string.reminder_cart_chip,
            R.drawable.ic_shopping_cart,
            R.string.reminder_cart_title,
            R.string.reminder_cart_body),
        WISHLIST(
            R.string.reminder_wishlist_chip,
            R.drawable.ic_heart,
            R.string.reminder_wishlist_title,
            R.string.reminder_wishlist_body),
        NEW_IN(
            R.string.reminder_new_chip,
            R.drawable.ic_sparkles,
            R.string.reminder_new_title,
            R.string.reminder_new_body),
    }

    private var binding: FragmentRemindersBinding? = null
    private var reminder = Reminder.CART
    private var seconds = DEFAULT_SECONDS
    private val chips = mutableMapOf<Reminder, TextView>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentRemindersBinding.bind(view)
        this.binding = binding
        savedInstanceState?.let { state ->
            reminder =
                Reminder.values().firstOrNull { it.name == state.getString(KEY_REMINDER) }
                    ?: Reminder.CART
            seconds = state.getInt(KEY_SECONDS, DEFAULT_SECONDS)
        }
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }
        binding.enableButton.setOnClickListener {
            services.accountManager.setPushEnabled(true) { render() }
        }

        chips.clear()
        for (item in Reminder.values()) {
            val chip = makeChip(binding.reminderChips, item)
            chip.setOnClickListener {
                reminder = item
                render()
            }
            binding.reminderChips.addView(chip)
            chips[item] = chip
        }

        val wheel = DurationWheel(binding.wheel)
        wheel.seconds = seconds
        wheel.onChange {
            seconds = it
            render()
        }

        binding.remindMe.setOnClickListener {
            val label = delayLabel(seconds)
            services.reminders.schedule(
                getString(reminder.title), getString(reminder.body), seconds)
            ResultBus.success(
                getString(R.string.inbox_reminder_set),
                getString(R.string.reminders_set_message, label))
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_REMINDER, reminder.name)
        outState.putInt(KEY_SECONDS, seconds)
    }

    override fun onDestroyView() {
        chips.clear()
        binding = null
        super.onDestroyView()
    }

    private fun makeChip(parent: ViewGroup, item: Reminder): TextView {
        val chip = TextView(parent.context, null, 0, R.style.Widget_PushMart_Chip_Solid)
        chip.setText(item.chip)
        val density = resources.displayMetrics.density
        val iconSize = (CHIP_ICON_DP * density).roundToInt()
        val icon = AppCompatResources.getDrawable(parent.context, item.icon)?.mutate()
        icon?.setBounds(0, 0, iconSize, iconSize)
        chip.setCompoundDrawablesRelative(icon, null, null, null)
        TextViewCompat.setCompoundDrawableTintList(chip, chip.textColors)
        val params =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.marginEnd = (CHIP_SPACING_DP * density).roundToInt()
        chip.layoutParams = params
        return chip
    }

    private fun render() {
        val binding = binding ?: return
        binding.enableCard.isVisible = !services.pushPermission.isAllowed
        binding.previewDelay.text = getString(R.string.reminders_preview_in, delayLabel(seconds))
        binding.previewTitle.setText(reminder.title)
        binding.previewBody.setText(reminder.body)
        for ((item, chip) in chips) chip.isSelected = item == reminder
        val canSchedule = seconds > 0
        binding.remindMe.isEnabled = canSchedule
        binding.remindMe.alpha = if (canSchedule) 1f else 0.5f
    }

    /** "10 seconds", "1 minute, 5 seconds", "2 hours", as iOS `DurationWheel.label`. */
    private fun delayLabel(total: Int): String {
        val parts =
            listOf(
                    R.plurals.duration_hours to total / 3600,
                    R.plurals.duration_minutes to total / 60 % 60,
                    R.plurals.duration_seconds to total % 60)
                .filter { it.second > 0 }
        if (parts.isEmpty()) return resources.getQuantityString(R.plurals.duration_seconds, 0, 0)
        return parts.joinToString(getString(R.string.duration_separator)) { (plural, count) ->
            resources.getQuantityString(plural, count, count)
        }
    }

    companion object {
        private const val KEY_REMINDER = "reminder"
        private const val KEY_SECONDS = "seconds"
        private const val DEFAULT_SECONDS = 10
        private const val CHIP_ICON_DP = 16
        private const val CHIP_SPACING_DP = 8
    }
}
