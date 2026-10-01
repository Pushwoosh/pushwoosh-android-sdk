package com.pushwoosh.pushmart.core

data class LaunchArgs(val skipOnboarding: Boolean, val appCode: String?) {
    companion object {
        const val EXTRA_SKIP_ONBOARDING = "pw_skip_onboarding"
        const val EXTRA_APP_CODE = "pw_app_code"
        val NONE = LaunchArgs(skipOnboarding = false, appCode = null)

        fun from(extras: Map<String, Any?>): LaunchArgs {
            val skip =
                when (val raw = extras[EXTRA_SKIP_ONBOARDING]) {
                    is Boolean -> raw
                    is String -> raw.equals("true", ignoreCase = true)
                    else -> false
                }
            val code = (extras[EXTRA_APP_CODE] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            return LaunchArgs(skip, code)
        }
    }
}
