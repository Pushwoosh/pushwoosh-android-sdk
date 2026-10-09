package com.pushwoosh.demoapp.ui.featureflags

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pushwoosh.Pushwoosh
import com.pushwoosh.featureflags.FeatureFlagsUpdateListener
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.sampleapp.databinding.FragmentFeatureFlagsBinding

/**
 * Minimal test screen for the Feature Flags API: get a flag, refresh, log an impression and count
 * update-listener calls. Every action and result is mirrored to logcat under [TAG].
 */
class FeatureFlagsFragment : Fragment() {

    private var binding: FragmentFeatureFlagsBinding? = null
    private var listenerCalls = 0

    private val updateListener = FeatureFlagsUpdateListener {
        listenerCalls++
        PWLog.info(TAG, "listener: onFeatureFlagsUpdated calls=$listenerCalls")
        binding?.flagListenerCalls?.text = "listener calls: $listenerCalls"
    }

    private val flags
        get() = Pushwoosh.getInstance().featureFlags

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeatureFlagsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding!!
        b.flagListenerCalls.text = "listener calls: $listenerCalls"
        b.flagGetButton.setOnClickListener { get() }
        b.flagRefreshButton.setOnClickListener { refresh() }
        b.flagImpressionButton.setOnClickListener {
            val key = key()
            PWLog.info(TAG, "logImpression key=$key")
            flags.logImpression(key)
        }
    }

    override fun onStart() {
        super.onStart()
        flags.addUpdateListener(updateListener)
        PWLog.info(TAG, "listener registered")
    }

    override fun onStop() {
        flags.removeUpdateListener(updateListener)
        PWLog.info(TAG, "listener removed")
        super.onStop()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun key() = binding?.flagKeyInput?.text?.toString().orEmpty()

    private fun get() {
        val key = key()
        val prop = binding?.flagPropertyInput?.text?.toString().orEmpty()
        val flag = flags.get(key)
        val text =
            if (flag == null) {
                "null"
            } else {
                "enabled=${flag.isEnabled} ruleId=${flag.ruleId} " +
                    "$prop=${flag.getString(prop, "<def>")} limit=${flag.getNumber("limit", -1.0)}"
            }
        PWLog.info(TAG, "get key=$key result: $text")
        binding?.flagResult?.text = text
    }

    private fun refresh() {
        PWLog.info(TAG, "refresh requested")
        flags.refresh { result ->
            val text =
                if (result.isSuccess) {
                    "refresh: OK"
                } else {
                    val e = result.exception
                    "refresh: ERROR http=${e?.httpStatus} ${e?.message}"
                }
            PWLog.info(TAG, text)
            binding?.flagRefreshResult?.text = text
        }
    }

    private companion object {
        const val TAG = "FeatureFlagsFragment"
    }
}
