package com.pushwoosh.pushmart.ui.developer

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.Pushwoosh
import com.pushwoosh.inapp.InAppManager
import com.pushwoosh.internal.platform.utils.GeneralUtils
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentDeveloperToolsBinding
import com.pushwoosh.pushmart.ui.common.ResultBus
import kotlin.math.roundToInt

/**
 * Profile > Developer tools, as iOS `DeveloperView`: the SDK log level, the selected application
 * code and base URL, an in-app reload and the SDK version. Reached only from the debug-only
 * Developer section of Profile.
 */
class DeveloperToolsFragment : Fragment(R.layout.fragment_developer_tools) {
    private enum class Level(@StringRes val label: Int, val sdkName: String) {
        NONE(R.string.developer_log_none, "NONE"),
        ERROR(R.string.developer_log_error, "ERROR"),
        WARN(R.string.developer_log_warn, "WARN"),
        INFO(R.string.developer_log_info, "INFO"),
        DEBUG(R.string.developer_log_debug, "DEBUG"),
        VERBOSE(R.string.developer_log_verbose, "NOISE"),
    }

    private var binding: FragmentDeveloperToolsBinding? = null
    private val chips = mutableMapOf<Level, TextView>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentDeveloperToolsBinding.bind(view)
        this.binding = binding
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }
        buildLevelChips(binding.logLevels)
        binding.reloadInapps.setOnClickListener { reloadInApps() }
        binding.sdkVersion.text = GeneralUtils.SDK_VERSION
        renderLevels()
    }

    override fun onResume() {
        super.onResume()
        val binding = binding ?: return
        val pushwoosh = Pushwoosh.getInstance()
        binding.appCode.text = pushwoosh.applicationCode
        binding.baseUrl.text = pushwoosh.baseUrl ?: getString(R.string.developer_value_missing)
    }

    override fun onDestroyView() {
        chips.clear()
        binding = null
        super.onDestroyView()
    }

    private fun buildLevelChips(group: ViewGroup) {
        for (level in Level.values()) {
            val chip = TextView(group.context, null, 0, R.style.Widget_PushMart_Chip_Brand)
            chip.setText(level.label)
            chip.setOnClickListener { select(level) }
            group.addView(
                chip,
                ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            chips[level] = chip
        }
    }

    private fun select(level: Level) {
        selected = level
        PWLog.updateLogLevel(level.sdkName)
        renderLevels()
        ResultBus.success(getString(R.string.developer_log_level_set, getString(level.label)))
    }

    private fun renderLevels() {
        val size = (CHECK_ICON_DP * resources.displayMetrics.density).roundToInt()
        for ((level, chip) in chips) {
            val on = level == selected
            chip.isSelected = on
            val check =
                if (on) AppCompatResources.getDrawable(chip.context, R.drawable.ic_check)?.mutate()
                else null
            check?.setBounds(0, 0, size, size)
            chip.setCompoundDrawablesRelative(check, null, null, null)
            TextViewCompat.setCompoundDrawableTintList(chip, chip.textColors)
        }
    }

    private fun reloadInApps() {
        val reloaded = getString(R.string.developer_reloaded)
        val failed = getString(R.string.developer_reload_failed)
        InAppManager.getInstance().reloadInApps { result ->
            if (result.isSuccess) ResultBus.success(reloaded)
            else ResultBus.fail(failed, result.exception?.message.orEmpty())
        }
    }

    private companion object {
        const val CHECK_ICON_DP = 14

        /** The SDK has no log level getter; debug builds start at NOISE (manifest). */
        private var selected = Level.VERBOSE
    }
}
