package com.pushwoosh.pushmart.ui.orders

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.SheetDropTimeBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.DurationWheel
import java.util.Date

/**
 * Remind me in, as iOS `ReminderTimeSheet`: the delay on the shared hours, minutes and seconds
 * wheels (5 minutes by default), the time the reminder lands, and Remind me.
 */
class DropTimeSheet : BottomSheetDialogFragment() {
    private var binding: SheetDropTimeBinding? = null
    private var seconds = DEFAULT_SECONDS

    override fun getTheme(): Int = R.style.ThemeOverlay_PushMart_BottomSheet_Page

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.sheet_drop_time, container, false)

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val drop = services.drops.byId(requireArguments().getString(ARG_ID).orEmpty())
        if (drop == null) {
            dismiss()
            return
        }
        val binding = SheetDropTimeBinding.bind(view)
        this.binding = binding
        seconds = savedInstanceState?.getInt(STATE_SECONDS, DEFAULT_SECONDS) ?: DEFAULT_SECONDS
        DropsFragment.bindPhoto(binding.dropTimePhoto, drop)
        binding.dropTimeName.text = drop.name
        val wheel = DurationWheel(binding.dropTimeWheel)
        wheel.seconds = seconds
        wheel.onChange {
            seconds = it
            render()
        }
        binding.dropTimeRemind.setOnClickListener {
            it.isEnabled = false
            services.drops.schedule(drop, seconds)
            dismiss()
        }
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SECONDS, seconds)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun render() {
        val binding = binding ?: return
        val at =
            DateFormat.getTimeFormat(requireContext())
                .format(Date(System.currentTimeMillis() + seconds * 1000L))
        binding.dropTimeNote.text =
            getString(
                if (services.drops.countdownAvailable) R.string.drop_note_countdown
                else R.string.drop_note_reminder,
                at)
        val canSchedule = seconds > 0
        binding.dropTimeRemind.isEnabled = canSchedule
        binding.dropTimeRemind.alpha = if (canSchedule) 1f else 0.5f
    }

    companion object {
        const val TAG = "drop-time"
        private const val ARG_ID = "dropId"
        private const val STATE_SECONDS = "seconds"
        private const val DEFAULT_SECONDS = 300

        fun newInstance(dropId: String): DropTimeSheet =
            DropTimeSheet().apply { arguments = bundleOf(ARG_ID to dropId) }
    }
}
