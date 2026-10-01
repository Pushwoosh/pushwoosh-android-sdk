package com.pushwoosh.pushmart.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchArgsTest {
    @Test
    fun noExtrasMeansNoArgs() {
        assertEquals(LaunchArgs.NONE, LaunchArgs.from(emptyMap()))
    }

    @Test
    fun skipFlagWithCode() {
        val args =
            LaunchArgs.from(mapOf("pw_skip_onboarding" to true, "pw_app_code" to "5DF3B-09018"))
        assertEquals(LaunchArgs(skipOnboarding = true, appCode = "5DF3B-09018"), args)
    }

    @Test
    fun skipFlagAcceptsStringTrue() {
        assertEquals(
            LaunchArgs(skipOnboarding = true, appCode = null),
            LaunchArgs.from(mapOf("pw_skip_onboarding" to "true")))
        assertEquals(
            LaunchArgs(skipOnboarding = true, appCode = null),
            LaunchArgs.from(mapOf("pw_skip_onboarding" to "TRUE")))
        assertEquals(LaunchArgs.NONE, LaunchArgs.from(mapOf("pw_skip_onboarding" to "yes")))
    }

    @Test
    fun codeAloneDoesNotSkip() {
        assertEquals(
            LaunchArgs(skipOnboarding = false, appCode = "5DF3B-09018"),
            LaunchArgs.from(mapOf("pw_app_code" to "5DF3B-09018")))
    }

    @Test
    fun blankCodeBecomesNull() {
        assertEquals(
            LaunchArgs(skipOnboarding = true, appCode = null),
            LaunchArgs.from(mapOf("pw_skip_onboarding" to true, "pw_app_code" to "   ")))
    }

    @Test
    fun unexpectedTypesAreIgnored() {
        assertEquals(
            LaunchArgs.NONE, LaunchArgs.from(mapOf("pw_skip_onboarding" to 1, "pw_app_code" to 42)))
    }
}
