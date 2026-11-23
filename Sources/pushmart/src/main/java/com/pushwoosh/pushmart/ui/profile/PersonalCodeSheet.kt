package com.pushwoosh.pushmart.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.account.AppCode
import com.pushwoosh.pushmart.account.CodeSwitch
import com.pushwoosh.pushmart.databinding.SheetPersonalCodeBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.PageSheet
import com.pushwoosh.pushmart.ui.common.ResultBus

class PersonalCodeSheet : BottomSheetDialogFragment() {
    private var binding: SheetPersonalCodeBinding? = null
    private var masking = false
    private var shownCode: String? = null
    private val onSwitchChanged: () -> Unit = {
        if (!services.accountManager.isSwitching && services.account.personalCode != shownCode) {
            dismissAllowingStateLoss()
        } else {
            render()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.sheet_personal_code, container, false)

    override fun getTheme(): Int = R.style.ThemeOverlay_PushMart_BottomSheet_Page

    override fun onStart() {
        super.onStart()
        PageSheet.expand(dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = SheetPersonalCodeBinding.bind(view)
        this.binding = binding
        val code = services.account.personalCode
        shownCode = code
        binding.codeSheetCurrent.isVisible = code != null
        binding.codeSheetRemove.isVisible = code != null
        binding.codeSheetRemoveNote.isVisible = code != null
        binding.codeSheetCurrent.text = code?.let { getString(R.string.code_sheet_current, it) }
        binding.codeSheetField.doAfterTextChanged { text ->
            if (masking || text == null) return@doAfterTextChanged
            val masked = AppCode.mask(text.toString())
            if (masked != text.toString()) {
                masking = true
                text.replace(0, text.length, masked)
                binding.codeSheetField.setSelection(masked.length)
                masking = false
            }
            render()
        }
        binding.codeSheetApply.setOnClickListener {
            val raw = binding.codeSheetField.text.toString()
            switch { services.accountManager.applyCode(raw, it) }
        }
        binding.codeSheetRemove.setOnClickListener {
            switch { services.accountManager.removeCode(it) }
        }
        services.accountManager.addSwitchListener(onSwitchChanged)
        render()
    }

    override fun onDestroyView() {
        services.accountManager.removeSwitchListener(onSwitchChanged)
        binding = null
        super.onDestroyView()
    }

    private fun switch(start: ((CodeSwitch) -> Unit) -> Unit) {
        val appContext = requireContext().applicationContext
        start { it.report(appContext) }
    }

    private fun render() {
        val binding = binding ?: return
        val idle = !services.accountManager.isSwitching
        val canApply = idle && binding.codeSheetField.text.isNotEmpty()
        binding.codeSheetApply.isEnabled = canApply
        binding.codeSheetApply.alpha = if (canApply) 1f else 0.5f
        binding.codeSheetRemove.isEnabled = idle
    }

    companion object {
        const val TAG = "personal-code"
    }
}

fun CodeSwitch.report(context: Context) {
    when (this) {
        is CodeSwitch.Applied ->
            ResultBus.success(
                context.getString(R.string.code_applied_title),
                context.getString(R.string.code_applied_message, code))
        CodeSwitch.Removed ->
            ResultBus.success(
                context.getString(R.string.code_removed_title),
                context.getString(R.string.code_removed_message))
        CodeSwitch.Invalid ->
            ResultBus.fail(
                context.getString(R.string.code_invalid_title),
                context.getString(R.string.code_invalid_message))
        CodeSwitch.Unchanged ->
            ResultBus.fail(
                context.getString(R.string.code_unchanged_title),
                context.getString(R.string.code_unchanged_message))
        CodeSwitch.NoConnection ->
            ResultBus.fail(
                context.getString(R.string.code_offline_title),
                context.getString(R.string.code_offline_message))
        CodeSwitch.Busy -> Unit
    }
}
