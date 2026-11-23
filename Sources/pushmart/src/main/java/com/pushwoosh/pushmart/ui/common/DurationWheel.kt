package com.pushwoosh.pushmart.ui.common

import android.widget.NumberPicker
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.ViewDurationWheelBinding

/**
 * Hours, minutes and seconds wheels, as iOS `DurationWheel`: 0-23 h, 0-59 min and 0-59 s. The
 * pickers do not save their own state, so the screen saves [seconds] and sets it back.
 */
class DurationWheel(private val binding: ViewDurationWheelBinding) {
    private var listener: ((Int) -> Unit)? = null

    var seconds: Int
        get() =
            binding.wheelHours.value * 3600 +
                binding.wheelMinutes.value * 60 +
                binding.wheelSeconds.value
        set(value) {
            binding.wheelHours.value = value / 3600
            binding.wheelMinutes.value = value / 60 % 60
            binding.wheelSeconds.value = value % 60
        }

    init {
        setUp(binding.wheelHours, 23, R.string.duration_hours_short)
        setUp(binding.wheelMinutes, 59, R.string.duration_minutes_short)
        setUp(binding.wheelSeconds, 59, R.string.duration_seconds_short)
    }

    /** Calls [listener] with the new total whenever the shopper turns a wheel. */
    fun onChange(listener: (Int) -> Unit) {
        this.listener = listener
    }

    private fun setUp(picker: NumberPicker, max: Int, unit: Int) {
        // Three pickers share the inner input id: their saved states would overwrite each other.
        picker.isSaveFromParentEnabled = false
        picker.minValue = 0
        picker.maxValue = max
        picker.displayedValues = Array(max + 1) { picker.context.getString(unit, it) }
        picker.setOnValueChangedListener { _, _, _ -> listener?.invoke(seconds) }
    }
}
