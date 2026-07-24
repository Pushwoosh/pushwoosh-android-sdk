package com.pushwoosh.inapp.ui.animation

import android.content.Context
import android.provider.Settings

internal object ReduceMotionUtil {

    /** True when the user has turned animations off (Animator duration scale == 0). */
    fun isReduceMotionEnabled(context: Context): Boolean = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (e: Exception) {
        false
    }
}
