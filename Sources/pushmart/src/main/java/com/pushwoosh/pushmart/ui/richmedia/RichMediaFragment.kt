package com.pushwoosh.pushmart.ui.richmedia

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.account.AccountOutcome
import com.pushwoosh.pushmart.databinding.FragmentRichMediaBinding
import com.pushwoosh.pushmart.sdk.RichMediaSignals
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.ResultBus
import com.pushwoosh.richmedia.RichMediaColorScheme
import com.pushwoosh.richmedia.RichMediaManager
import com.pushwoosh.richmedia.RichMediaType

class RichMediaFragment : Fragment(R.layout.fragment_rich_media) {
    private var binding: FragmentRichMediaBinding? = null
    private var triggers: RichMediaTriggers? = null
    private var signalListener: ((RichMediaSignals.Signal) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentRichMediaBinding.bind(view)
        this.binding = binding
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }
        bindDisplayMode(binding)
        bindStyle(binding)
        bindTriggers(binding)
        bindSignals()
    }

    override fun onDestroyView() {
        // Another screen may have taken the slot already; only drop our own listener.
        if (RichMediaSignals.listener === signalListener) RichMediaSignals.listener = null
        signalListener = null
        triggers?.cancel()
        triggers = null
        binding = null
        super.onDestroyView()
    }

    private fun bindDisplayMode(binding: FragmentRichMediaBinding) {
        val modal = RichMediaManager.getRichMediaType() == RichMediaType.MODAL
        binding.modalSwitch.isChecked = modal
        renderMode(binding, modal)
        binding.modalSwitch.setOnCheckedChangeListener { _, checked ->
            RichMediaManager.setRichMediaType(
                if (checked) RichMediaType.MODAL else RichMediaType.DEFAULT)
            renderMode(binding, checked)
        }
    }

    private fun renderMode(binding: FragmentRichMediaBinding, modal: Boolean) {
        binding.modeStatus.text =
            if (modal) "Modal window (MODAL)" else "Legacy full screen (DEFAULT)"
    }

    private fun bindStyle(binding: FragmentRichMediaBinding) {
        val config = RichMediaManager.getDefaultRichMediaConfig()
        dropdown(binding.position, ModalRichMediaViewPosition.values(), config.viewPosition) {
            updateConfig { setViewPosition(it) }
        }
        dropdown(
            binding.presentAnimation,
            ModalRichMediaPresentAnimationType.values(),
            config.presentAnimationType) {
                updateConfig { setPresentAnimationType(it) }
            }
        dropdown(
            binding.dismissAnimation,
            ModalRichMediaDismissAnimationType.values(),
            config.dismissAnimationType) {
                updateConfig { setDismissAnimationType(it) }
            }
        dropdown(binding.windowWidth, ModalRichMediaWindowWidth.values(), config.windowWidth) {
            updateConfig { setWindowWidth(it) }
        }
        dropdown(
            binding.colorScheme,
            RichMediaColorScheme.values(),
            RichMediaManager.getRichMediaColorScheme()) {
                RichMediaManager.setRichMediaColorScheme(it)
            }

        val gestures = config.swipeGestures.orEmpty()
        binding.swipeUp.isChecked = ModalRichMediaSwipeGesture.UP in gestures
        binding.swipeLeft.isChecked = ModalRichMediaSwipeGesture.LEFT in gestures
        binding.swipeRight.isChecked = ModalRichMediaSwipeGesture.RIGHT in gestures
        binding.swipeDown.isChecked = ModalRichMediaSwipeGesture.DOWN in gestures
        val onGesture =
            CompoundButton.OnCheckedChangeListener { _, _ ->
                updateConfig {
                    setSwipeGestures(
                        RichMediaStyle.gestures(
                            binding.swipeUp.isChecked,
                            binding.swipeLeft.isChecked,
                            binding.swipeRight.isChecked,
                            binding.swipeDown.isChecked))
                }
            }
        binding.swipeUp.setOnCheckedChangeListener(onGesture)
        binding.swipeLeft.setOnCheckedChangeListener(onGesture)
        binding.swipeRight.setOnCheckedChangeListener(onGesture)
        binding.swipeDown.setOnCheckedChangeListener(onGesture)

        val durationIds = listOf(R.id.duration_300, R.id.duration_600, R.id.duration_1000)
        val index = RichMediaStyle.durationIndex(config.animationDuration)
        if (index >= 0) binding.durationGroup.check(durationIds[index])
        binding.durationGroup.setOnCheckedChangeListener { _, checkedId ->
            val picked = durationIds.indexOf(checkedId)
            if (picked >= 0)
                updateConfig { setAnimationDuration(RichMediaStyle.DURATIONS_MS[picked]) }
        }
    }

    private fun <T : Enum<T>> dropdown(
        view: MaterialAutoCompleteTextView,
        values: Array<T>,
        current: T?,
        onPick: (T) -> Unit,
    ) {
        val labels = values.map { it.name.lowercase().replace('_', ' ') }
        view.setSimpleItems(labels.toTypedArray())
        if (current != null) view.setText(labels[values.indexOf(current)], false)
        view.setOnItemClickListener { _, _, index, _ -> onPick(values[index]) }
    }

    // setDefaultRichMediaConfig overwrites every field, so each control edits the stored config.
    private fun updateConfig(change: ModalRichmediaConfig.() -> Unit) {
        val config = RichMediaManager.getDefaultRichMediaConfig()
        config.change()
        RichMediaManager.setDefaultRichMediaConfig(config)
    }

    private fun bindTriggers(binding: FragmentRichMediaBinding) {
        val appCode = services.account.appCode
        binding.triggersHint.text =
            "Each button posts an event. Content appears only if Control Panel has a campaign for that event in app $appCode. The default showcase app has none, so expect \"No content returned\"."
        val triggers =
            RichMediaTriggers(
                services.gateway, HandlerDelayedRunner(Handler(Looper.getMainLooper()))) {
                    report(it)
                }
        this.triggers = triggers
        binding.triggerShowRichMedia.setOnClickListener { triggers.trigger("showRichMedia") }
        binding.triggerShowRichMediaClient.setOnClickListener {
            triggers.trigger("showRichMediaClient")
        }
        binding.triggerShowRichMediaServer.setOnClickListener {
            triggers.trigger("showRichMediaServer")
        }
    }

    private fun bindSignals() {
        val listener: (RichMediaSignals.Signal) -> Unit = { signal ->
            val triggers = this.triggers
            when (signal) {
                is RichMediaSignals.Signal.Presented -> triggers?.onContentShown(signal.code)
                is RichMediaSignals.Signal.Failed ->
                    triggers?.onContentFailed(signal.code, signal.error)
            }
        }
        signalListener = listener
        RichMediaSignals.listener = listener
    }

    /** The banner disappears after a couple of seconds; the outcome line keeps the last result. */
    private fun report(outcome: AccountOutcome) {
        ResultBus.show(outcome)
        val view = binding?.triggerOutcome ?: return
        view.text = outcomeText(outcome)
        view.visibility = View.VISIBLE
    }

    private fun outcomeText(outcome: AccountOutcome): String {
        val (title, message) =
            when (outcome) {
                is AccountOutcome.Success -> outcome.title to outcome.message
                is AccountOutcome.Failure -> outcome.title to outcome.message
            }
        return if (message.isEmpty()) title else "$title — $message"
    }
}
