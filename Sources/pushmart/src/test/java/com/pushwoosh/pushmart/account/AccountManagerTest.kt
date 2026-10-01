package com.pushwoosh.pushmart.account

import com.pushwoosh.pushmart.core.InMemoryKeyValueStore
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
import com.pushwoosh.pushmart.sdk.GatewayResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountManagerTest {
    private val store = AccountStore(InMemoryKeyValueStore())
    private val gateway = FakePushwooshGateway()
    private val manager = AccountManager(store, gateway)

    @Test
    fun connectAppliesMaskedCodeAndStoresIt() {
        assertNull(manager.connect(" 5df3b-09018 "))
        assertEquals("5DF3B-09018", store.appCode)
        assertEquals(listOf("setAppId:5DF3B-09018"), gateway.calls)
    }

    @Test
    fun connectRejectsInvalidCodeWithoutSdkCall() {
        assertEquals(
            "Enter your access code in the format XXXXX-XXXXX.", manager.connect("5DF3B0901"))
        assertEquals("", store.appCode)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun signInWithEmailSetsEmailThenUserIdAndOnboards() {
        val errors = mutableListOf<String>()
        manager.signIn("Ada Lovelace", "ada@example.com") { errors.add(it) }
        assertEquals(listOf("setEmail:ada@example.com", "setUserId:ada@example.com"), gateway.calls)
        assertEquals("Ada Lovelace", store.userName)
        assertEquals("ada@example.com", store.userEmail)
        assertTrue(store.onboarded)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun signInTrimsEmailBeforeIdentityCalls() {
        manager.signIn("  Ada ", "  Ada@Example.com ") {}
        assertEquals(listOf("setEmail:Ada@Example.com", "setUserId:Ada@Example.com"), gateway.calls)
        assertEquals("Ada", store.userName)
        assertEquals("Ada@Example.com", store.userEmail)
    }

    @Test
    fun signInWithoutEmailMakesNoIdentityCalls() {
        manager.signIn("Ada", "") {}
        assertTrue(gateway.calls.isEmpty())
        assertTrue(store.onboarded)
        assertEquals("Ada", store.userName)
    }

    @Test
    fun signInReportsIdentityFailures() {
        gateway.identityResult = GatewayResult.failure("network down")
        val errors = mutableListOf<String>()
        manager.signIn("Ada", "ada@example.com") { errors.add(it) }
        assertEquals(listOf("network down", "network down"), errors)
        assertTrue(store.onboarded)
    }

    @Test
    fun guestOnboardsWithoutIdentity() {
        manager.continueAsGuest()
        assertTrue(store.onboarded)
        assertTrue(store.isGuest)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun switchAccountUnregistersSetsAppIdIdentityThenRegisters() {
        var outcome: AccountOutcome? = null
        manager.switchAccount("abcde-12345", "Grace", "grace@example.com") { outcome = it }
        assertEquals(
            listOf(
                "unregister",
                "setAppId:ABCDE-12345",
                "setEmail:grace@example.com",
                "setUserId:grace@example.com",
                "register"),
            gateway.calls)
        assertEquals("ABCDE-12345", store.appCode)
        assertEquals("Grace", store.userName)
        assertEquals("grace@example.com", store.userEmail)
        assertEquals(
            AccountOutcome.Success("Account switched", "Connected to app ABCDE-12345."), outcome)
    }

    @Test
    fun switchReportsIdentityFailure() {
        gateway.identityResult = GatewayResult.failure("network down")
        var outcome: AccountOutcome? = null
        manager.switchAccount("abcde-12345", "Grace", "grace@example.com") { outcome = it }
        assertEquals(
            AccountOutcome.Failure(
                "Switch incomplete", "Connected to app ABCDE-12345, but network down."),
            outcome)
        assertTrue(gateway.calls.contains("register"))
    }

    @Test
    fun switchReportsRegisterFailure() {
        gateway.registerResult = GatewayResult.failure("no play services")
        var outcome: AccountOutcome? = null
        manager.switchAccount("abcde-12345", "Grace", "grace@example.com") { outcome = it }
        assertEquals(
            AccountOutcome.Failure(
                "Switch incomplete", "Connected to app ABCDE-12345, but no play services."),
            outcome)
    }

    @Test
    fun switchWithoutEmailSkipsIdentity() {
        manager.switchAccount("ABCDE-12345", "", "") {}
        assertEquals(listOf("unregister", "setAppId:ABCDE-12345", "register"), gateway.calls)
        assertEquals("", store.userEmail)
    }

    @Test
    fun switchWithInvalidCodeMakesNoSdkCalls() {
        var outcome: AccountOutcome? = null
        manager.switchAccount("bad", "Grace", "grace@example.com") { outcome = it }
        assertTrue(gateway.calls.isEmpty())
        assertEquals(
            AccountOutcome.Failure("Invalid app code", "Use the format XXXXX-XXXXX."), outcome)
    }

    @Test
    fun signOutUnregistersClearsIdentityAndPushFlagButKeepsAppCode() {
        store.appCode = "5DF3B-09018"
        store.userName = "Ada"
        store.userEmail = "ada@example.com"
        store.onboarded = true
        store.pushNotificationEnabled = true
        manager.signOut()
        assertEquals(listOf("unregister"), gateway.calls)
        assertTrue(store.isGuest)
        assertFalse(store.pushNotificationEnabled)
        assertFalse(store.onboarded)
        assertEquals("5DF3B-09018", store.appCode)
    }

    @Test
    fun saveDetailsSetsUserIdAndEmailWhenPresent() {
        var outcome: AccountOutcome? = null
        manager.saveDetails("Ada", "member-42", "ada@example.com") { outcome = it }
        assertEquals(listOf("setUserId:member-42", "setEmail:ada@example.com"), gateway.calls)
        assertEquals("Ada", store.userName)
        assertEquals("ada@example.com", store.userEmail)
        assertEquals(AccountOutcome.Success("Saved", "Your details are up to date."), outcome)
    }

    @Test
    fun saveDetailsWithNothingToSyncFails() {
        var outcome: AccountOutcome? = null
        manager.saveDetails("Ada", "", "  ") { outcome = it }
        assertTrue(gateway.calls.isEmpty())
        assertEquals("Ada", store.userName)
        assertEquals(
            AccountOutcome.Failure("Nothing to save", "Add a member ID or email first."), outcome)
    }

    @Test
    fun saveDetailsWithMemberIdOnlySetsUserIdOnly() {
        var outcome: AccountOutcome? = null
        manager.saveDetails("Ada", "member-42", "") { outcome = it }
        assertEquals(listOf("setUserId:member-42"), gateway.calls)
        assertEquals("", store.userEmail)
        assertEquals(AccountOutcome.Success("Saved", "Your details are up to date."), outcome)
    }

    @Test
    fun saveDetailsWithEmailOnlySetsEmailOnly() {
        var outcome: AccountOutcome? = null
        manager.saveDetails("Ada", "", "ada@example.com") { outcome = it }
        assertEquals(listOf("setEmail:ada@example.com"), gateway.calls)
        assertEquals("ada@example.com", store.userEmail)
        assertEquals(AccountOutcome.Success("Saved", "Your details are up to date."), outcome)
    }

    @Test
    fun saveDetailsReportsSdkFailure() {
        gateway.identityResult = GatewayResult.failure("boom")
        var outcome: AccountOutcome? = null
        manager.saveDetails("", "member-42", "") { outcome = it }
        assertEquals(AccountOutcome.Failure("Save failed", "boom"), outcome)
    }

    @Test
    fun switchReportsNothingBeforeRegisterCallback() {
        gateway.deferCallbacks = true
        var outcome: AccountOutcome? = null
        manager.switchAccount("abcde-12345", "Grace", "grace@example.com") { outcome = it }
        assertNull(outcome)
        gateway.flush()
        assertEquals(
            AccountOutcome.Success("Account switched", "Connected to app ABCDE-12345."), outcome)
    }

    @Test
    fun switchReportsRegisterFailureAfterIdentitySuccess() {
        gateway.deferCallbacks = true
        gateway.registerResult = GatewayResult.failure("no play services")
        var outcome: AccountOutcome? = null
        manager.switchAccount("abcde-12345", "Grace", "grace@example.com") { outcome = it }
        gateway.flush()
        assertEquals(
            AccountOutcome.Failure(
                "Switch incomplete", "Connected to app ABCDE-12345, but no play services."),
            outcome)
        assertFalse(store.pushNotificationEnabled)
    }

    @Test
    fun switchKeepsPushFlagOnWhenRegisterSucceedsAfterIdentityFailure() {
        gateway.deferCallbacks = true
        gateway.identityResult = GatewayResult.failure("network down")
        var outcome: AccountOutcome? = null
        manager.switchAccount("abcde-12345", "Grace", "grace@example.com") { outcome = it }
        gateway.flush()
        assertEquals(
            AccountOutcome.Failure(
                "Switch incomplete", "Connected to app ABCDE-12345, but network down."),
            outcome)
        assertTrue(store.pushNotificationEnabled)
    }

    @Test
    fun switchSuccessTurnsPushFlagOn() {
        gateway.deferCallbacks = true
        var outcome: AccountOutcome? = null
        manager.switchAccount("abcde-12345", "Grace", "grace@example.com") { outcome = it }
        gateway.flush()
        assertTrue(outcome is AccountOutcome.Success)
        assertTrue(store.pushNotificationEnabled)
    }

    @Test
    fun saveDetailsReportsExactlyOnceOnFailure() {
        gateway.deferCallbacks = true
        gateway.identityResult = GatewayResult.failure("boom")
        val outcomes = mutableListOf<AccountOutcome>()
        manager.saveDetails("Ada", "member-42", "ada@example.com") { outcomes.add(it) }
        gateway.flush()
        assertEquals(1, outcomes.size)
        assertTrue(outcomes.single() is AccountOutcome.Failure)
    }

    @Test
    fun enablingPushRegistersAndStoresFlagOnSuccess() {
        var outcome: AccountOutcome? = null
        manager.setPushEnabled(true) { outcome = it }
        assertEquals(listOf("register"), gateway.calls)
        assertTrue(store.pushNotificationEnabled)
        assertEquals(AccountOutcome.Success("Registered", "fake-token"), outcome)
    }

    @Test
    fun enablingPushKeepsFlagOffOnFailure() {
        gateway.registerResult = GatewayResult.failure("no play services")
        var outcome: AccountOutcome? = null
        manager.setPushEnabled(true) { outcome = it }
        assertFalse(store.pushNotificationEnabled)
        assertEquals(AccountOutcome.Failure("Register failed", "no play services"), outcome)
    }

    @Test
    fun disablingPushUnregistersAndClearsFlag() {
        store.pushNotificationEnabled = true
        var outcome: AccountOutcome? = null
        manager.setPushEnabled(false) { outcome = it }
        assertEquals(listOf("unregister"), gateway.calls)
        assertFalse(store.pushNotificationEnabled)
        assertEquals(AccountOutcome.Success("Notifications off", ""), outcome)
    }

    @Test
    fun enterWithoutOnboardingUsesDemoCodeAndContinuesAsGuest() {
        assertTrue(manager.enterWithoutOnboarding(null))
        assertEquals(listOf("setAppId:5DF3B-09018"), gateway.calls)
        assertEquals("5DF3B-09018", store.appCode)
        assertTrue(store.onboarded)
        assertTrue(store.isGuest)
    }

    @Test
    fun enterWithoutOnboardingUsesValidCode() {
        assertTrue(manager.enterWithoutOnboarding("ABCDE-12345"))
        assertEquals(listOf("setAppId:ABCDE-12345"), gateway.calls)
        assertEquals("ABCDE-12345", store.appCode)
    }

    @Test
    fun enterWithoutOnboardingMasksRawCode() {
        assertTrue(manager.enterWithoutOnboarding(" 5df3b09018 "))
        assertEquals(listOf("setAppId:5DF3B-09018"), gateway.calls)
    }

    @Test
    fun enterWithoutOnboardingFallsBackToDemoOnInvalidCode() {
        assertTrue(manager.enterWithoutOnboarding("nope"))
        assertEquals(listOf("setAppId:5DF3B-09018"), gateway.calls)
    }

    @Test
    fun enterWithoutOnboardingIsNoOpWhenAlreadyOnboarded() {
        store.onboarded = true
        store.appCode = "ABCDE-12345"
        store.userName = "Ada"
        assertFalse(manager.enterWithoutOnboarding("5DF3B-09018"))
        assertTrue(gateway.calls.isEmpty())
        assertEquals("ABCDE-12345", store.appCode)
        assertEquals("Ada", store.userName)
    }
}
