package com.pushwoosh.pushmart.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.databinding.FragmentPreferencesBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.Chips
import com.pushwoosh.pushmart.ui.common.ResultBus

class PreferencesFragment : Fragment(R.layout.fragment_preferences) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentPreferencesBinding.bind(view)
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }
        val preferences = services.preferences
        for (category in Catalog.preferenceCategories) {
            val chip = Chips.make(binding.chips, category, preferences.isSelected(category))
            decorate(chip)
            chip.setOnClickListener {
                val on = preferences.toggle(category)
                chip.isSelected = on
                decorate(chip)
                if (on) {
                    ResultBus.success("Following $category", "You'll see more $category deals.")
                } else {
                    ResultBus.success("Unfollowed $category", "Removed from your favorites.")
                }
            }
            binding.chips.addView(chip)
        }
    }

    private fun decorate(chip: TextView) {
        chip.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (chip.isSelected) R.drawable.ic_check else 0, 0, 0, 0)
        chip.compoundDrawablePadding = (6 * resources.displayMetrics.density).toInt()
        chip.compoundDrawableTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.pm_on_brand)
    }
}
