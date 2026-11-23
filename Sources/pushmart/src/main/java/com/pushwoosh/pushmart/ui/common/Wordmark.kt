package com.pushwoosh.pushmart.ui.common

import androidx.core.view.updateLayoutParams
import com.pushwoosh.pushmart.databinding.ViewWordmarkBinding
import kotlin.math.roundToInt

object Wordmark {
    private const val DOT_RATIO = 0.16f

    /**
     * Sets the wordmark text to [sp] and its dot to `sp * 0.16` dp, as iOS
     * `PushMartWordmark(size:)`.
     */
    fun size(binding: ViewWordmarkBinding, sp: Float) {
        binding.wordmarkText.textSize = sp
        val dot = (sp * DOT_RATIO * binding.root.resources.displayMetrics.density).roundToInt()
        binding.wordmarkDot.updateLayoutParams {
            width = dot
            height = dot
        }
    }
}
