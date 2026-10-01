package com.pushwoosh.pushmart.ui.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.account.AccountOutcome
import com.pushwoosh.pushmart.account.AppCode
import com.pushwoosh.pushmart.databinding.SheetSwitchAccountBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.ResultBus

class SwitchAccountSheet : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.sheet_switch_account, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = SheetSwitchAccountBinding.bind(view)
        if (savedInstanceState == null) {
            binding.codeInput.setText(services.gateway.getApplicationCode())
        }
        binding.codeInput.addTextChangedListener(
            object : TextWatcher {
                private var applying = false

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable) {
                    if (applying) return
                    val masked = AppCode.mask(s.toString())
                    if (masked != s.toString()) {
                        applying = true
                        s.replace(0, s.length, masked)
                        applying = false
                    }
                }
            })
        binding.codeInput.doAfterTextChanged {
            binding.switchButton.isEnabled = it.toString().trim().isNotEmpty()
        }
        binding.switchButton.isEnabled = binding.codeInput.text.toString().trim().isNotEmpty()
        binding.switchButton.setOnClickListener {
            services.accountManager.switchAccount(
                binding.codeInput.text.toString(),
                binding.nameInput.text.toString(),
                binding.emailInput.text.toString()) { outcome ->
                    ResultBus.show(outcome)
                    if (outcome is AccountOutcome.Success && isAdded) dismissAllowingStateLoss()
                }
        }
    }
}
