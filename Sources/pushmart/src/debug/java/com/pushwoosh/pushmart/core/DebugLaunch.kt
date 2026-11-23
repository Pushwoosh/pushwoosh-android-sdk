package com.pushwoosh.pushmart.core

import android.os.Bundle
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.pushmart.Services

object DebugLaunch {
    private const val TAG = "DebugLaunch"

    fun apply(services: Services, extras: Bundle?) {
        val args = LaunchArgs.from(extras?.toMap().orEmpty())
        if (!args.skipOnboarding || services.account.onboarded) return
        services.accountManager.skipOnboarding(args.appCode)
        PWLog.debug(
            TAG,
            "onboarding skipped by launch args, app code ${services.gateway.getApplicationCode()}")
    }

    @Suppress("DEPRECATION")
    private fun Bundle.toMap(): Map<String, Any?> = keySet().associateWith { get(it) }
}
