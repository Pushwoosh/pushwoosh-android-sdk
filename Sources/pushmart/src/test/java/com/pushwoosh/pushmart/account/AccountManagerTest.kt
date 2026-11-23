package com.pushwoosh.pushmart.account

import com.pushwoosh.pushmart.core.FakePushPermission
import com.pushwoosh.pushmart.core.InMemoryKeyValueStore
import com.pushwoosh.pushmart.core.PrimerOutcome
import com.pushwoosh.pushmart.sdk.FakeInboxGateway
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
import com.pushwoosh.pushmart.sdk.GatewayResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountManagerTest {
    private val prefs = InMemoryKeyValueStore()
    private val store = AccountStore(prefs)
    private val gateway = FakePushwooshGateway()
    private val inbox = FakeInboxGateway()
    private val permission = FakePushPermission()
    private var online = true
    private val manager = AccountManager(store, gateway, inbox, permission) { online }

    @Test
    fun saveDetailsSetsEmailAndRegistersChangedNumbers() {
        var outcome: AccountOutcome? = null
        manager.saveDetails(" shopper@example.com ", "+15550100", "+15550101") { outcome = it }
        assertEquals(
            listOf(
                "setEmail:shopper@example.com",
                "registerSms:+15550100",
                "registerWhatsapp:+15550101"),
            gateway.calls)
        assertEquals("shopper@example.com", store.userEmail)
        assertEquals("+15550100", store.smsNumber)
        assertEquals("+15550101", store.whatsappNumber)
        assertEquals(AccountOutcome.Success("Saved", "Your details are up to date."), outcome)

        gateway.calls.clear()
        manager.saveDetails("shopper@example.com", " +15550100 ", "+15550101") {}
        assertEquals(listOf("setEmail:shopper@example.com"), gateway.calls)

        gateway.calls.clear()
        outcome = null
        manager.saveDetails("", "", "+15550101") { outcome = it }
        assertTrue(gateway.calls.isEmpty())
        assertEquals("", store.smsNumber)
        assertEquals("+15550101", store.whatsappNumber)
        assertEquals("shopper@example.com", store.userEmail)
        assertEquals(AccountOutcome.Success("Saved", "Your details are up to date."), outcome)
    }

    @Test
    fun saveDetailsWithoutEmailSavesNothing() {
        var outcome: AccountOutcome? = null
        manager.saveDetails("  ", " ", "") { outcome = it }
        assertTrue(gateway.calls.isEmpty())
        assertEquals("", store.userEmail)
        assertEquals("", store.smsNumber)
        assertEquals(
            AccountOutcome.Failure("Nothing to save", "Add your email or a phone number first."),
            outcome)

        gateway.deferCallbacks = true
        outcome = null
        manager.saveDetails(" ", "+15550100", "") { outcome = it }
        assertEquals(listOf("registerSms:+15550100"), gateway.calls)
        assertEquals("", store.userEmail)
        assertEquals("+15550100", store.smsNumber)
        assertEquals(AccountOutcome.Success("Saved", "Your details are up to date."), outcome)
    }

    @Test
    fun saveDetailsReportsSdkFailure() {
        gateway.identityResult = GatewayResult.failure("boom")
        var outcome: AccountOutcome? = null
        manager.saveDetails("shopper@example.com", "+15550100", "+15550101") { outcome = it }
        assertEquals(AccountOutcome.Failure("Save failed", "boom"), outcome)
        assertEquals(
            listOf(
                "setEmail:shopper@example.com",
                "registerSms:+15550100",
                "registerWhatsapp:+15550101"),
            gateway.calls)
    }

    @Test
    fun saveDetailsReportsOnceAfterEmailCallback() {
        gateway.deferCallbacks = true
        val outcomes = mutableListOf<AccountOutcome>()
        manager.saveDetails("shopper@example.com", "", "") { outcomes.add(it) }
        assertTrue(outcomes.isEmpty())
        gateway.flush()
        assertEquals(
            listOf(AccountOutcome.Success("Saved", "Your details are up to date.")), outcomes)
    }

    @Test
    fun enablingPushRegistersAndStoresFlagOnSuccess() {
        var on: Boolean? = null
        manager.setPushEnabled(true) { on = it }
        assertEquals(listOf("register"), gateway.calls)
        assertTrue(store.pushNotificationEnabled)
        assertEquals(true, on)
        assertEquals(0, permission.prompts)
    }

    @Test
    fun enablingPushKeepsFlagOffOnFailure() {
        gateway.registerResult = GatewayResult.failure("no play services")
        var on: Boolean? = null
        manager.setPushEnabled(true) { on = it }
        assertFalse(store.pushNotificationEnabled)
        assertEquals(false, on)

        gateway.calls.clear()
        store.pushNotificationEnabled = true
        permission.allowed = false
        permission.outcome = PrimerOutcome.DECLINED
        on = null
        manager.setPushEnabled(true) { on = it }
        assertTrue(gateway.calls.isEmpty())
        assertEquals(false, on)
        assertEquals(1, permission.prompts)
        assertTrue(store.pushNotificationEnabled)
    }

    @Test
    fun disablingPushUnregistersAndClearsFlag() {
        store.pushNotificationEnabled = true
        var on: Boolean? = null
        manager.setPushEnabled(false) { on = it }
        assertEquals(listOf("unregister"), gateway.calls)
        assertFalse(store.pushNotificationEnabled)
        assertEquals(false, on)

        gateway.calls.clear()
        gateway.deferCallbacks = true
        manager.applyCode("ABCDE-12345") {}
        val busy = mutableListOf<Boolean>()
        manager.setPushEnabled(false) { busy.add(it) }
        manager.setPushEnabled(true) { busy.add(it) }
        assertEquals(listOf(false, false), busy)
        assertEquals(listOf("unregister"), gateway.calls)
    }

    @Test
    fun applyOnLaunchUsesStoredPersonalCode() {
        store.personalCode = "ABCDE-12345"
        manager.applyOnLaunch()
        assertEquals(listOf("setAppId:ABCDE-12345", "resetUserId"), gateway.calls)

        gateway.calls.clear()
        manager.skipOnboarding("fghij67890")
        assertTrue(store.onboarded)
        assertEquals("FGHIJ-67890", store.personalCode)
        assertEquals(listOf("setAppId:FGHIJ-67890", "resetUserId"), gateway.calls)
    }

    @Test
    fun applyOnLaunchFallsBackToGuest() {
        for (raw in listOf("", "5DF3B-09018", "nope")) {
            val prefs = InMemoryKeyValueStore()
            prefs.putString("nuvo.appCode", raw)
            val gateway = FakePushwooshGateway()
            AccountManager(AccountStore(prefs), gateway, FakeInboxGateway(), FakePushPermission()) {
                    true
                }
                .applyOnLaunch()
            assertEquals(raw, listOf("setAppId:5DF3B-09018", "resetUserId"), gateway.calls)
        }

        for (raw in listOf(null, "nope", "5DF3B-09018")) {
            val prefs = InMemoryKeyValueStore()
            prefs.putString("nuvo.appCode", "ABCDE-12345")
            val store = AccountStore(prefs)
            val gateway = FakePushwooshGateway()
            AccountManager(store, gateway, FakeInboxGateway(), FakePushPermission()) { true }
                .skipOnboarding(raw)
            assertTrue(raw, store.onboarded)
            assertNull(raw, store.personalCode)
            assertEquals(raw, listOf("setAppId:5DF3B-09018", "resetUserId"), gateway.calls)
        }
    }

    @Test
    fun applyCodeUnregistersSetsAppIdRegistersAndReloadsInbox() {
        var result: CodeSwitch? = null
        manager.applyCode("abcde12345") { result = it }
        assertEquals(CodeSwitch.Applied("ABCDE-12345"), result)
        assertEquals(listOf("unregister", "setAppId:ABCDE-12345", "register"), gateway.calls)
        assertEquals(listOf("resync"), inbox.calls)
        assertEquals("ABCDE-12345", store.personalCode)
        assertTrue(store.pushNotificationEnabled)
        assertEquals(0, permission.prompts)

        for ((outcome, registers) in
            listOf(PrimerOutcome.ACCEPTED to true, PrimerOutcome.DECLINED to false)) {
            val gateway = FakePushwooshGateway()
            val permission = FakePushPermission()
            permission.undetermined = true
            permission.allowed = false
            permission.outcome = outcome
            AccountManager(
                    AccountStore(InMemoryKeyValueStore()),
                    gateway,
                    FakeInboxGateway(),
                    permission) {
                        true
                    }
                .applyCode("FGHIJ-67890") {}
            assertEquals("$outcome", 1, permission.prompts)
            val applied = listOf("unregister", "setAppId:FGHIJ-67890")
            assertEquals(
                "$outcome", if (registers) applied + "register" else applied, gateway.calls)
        }
    }

    @Test
    fun applyCodeRejectsInvalidWithoutSdkCalls() {
        var result: CodeSwitch? = null
        manager.applyCode("5DF3B0901") { result = it }
        assertEquals(CodeSwitch.Invalid, result)
        assertTrue(gateway.calls.isEmpty())
        assertTrue(inbox.calls.isEmpty())
    }

    @Test
    fun applyingCurrentCodeIsUnchanged() {
        val results = mutableListOf<CodeSwitch>()
        manager.applyCode("5df3b-09018") { results.add(it) }
        store.personalCode = "ABCDE-12345"
        manager.applyCode("ABCDE-12345") { results.add(it) }
        assertEquals(listOf(CodeSwitch.Unchanged, CodeSwitch.Unchanged), results)
        assertTrue(gateway.calls.isEmpty())
        assertTrue(inbox.calls.isEmpty())
    }

    @Test
    fun applyingGuestCodeRemovesPersonalCode() {
        store.personalCode = "ABCDE-12345"
        var result: CodeSwitch? = null
        manager.applyCode("5DF3B-09018") { result = it }
        assertEquals(CodeSwitch.Removed, result)
        assertEquals(listOf("unregister", "setAppId:5DF3B-09018", "register"), gateway.calls)
        assertNull(store.personalCode)
        assertEquals("", prefs.getString("nuvo.appCode"))
    }

    @Test
    fun removeCodeReturnsToGuest() {
        store.personalCode = "ABCDE-12345"
        var result: CodeSwitch? = null
        manager.removeCode { result = it }
        assertEquals(CodeSwitch.Removed, result)
        assertEquals(listOf("unregister", "setAppId:5DF3B-09018", "register"), gateway.calls)
        assertNull(store.personalCode)
        assertEquals("", prefs.getString("nuvo.appCode"))
    }

    @Test
    fun removeCodeAsGuestIsUnchanged() {
        var result: CodeSwitch? = null
        manager.removeCode { result = it }
        assertEquals(CodeSwitch.Unchanged, result)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun unregisterFailureKeepsCurrentProject() {
        store.personalCode = "ABCDE-12345"
        store.pushNotificationEnabled = true
        val states = mutableListOf<Boolean>()
        manager.addSwitchListener { states.add(manager.isSwitching) }
        online = false
        val offline = mutableListOf<CodeSwitch>()
        manager.applyCode("FGHIJ-67890") { offline.add(it) }
        manager.removeCode { offline.add(it) }
        assertEquals(listOf(CodeSwitch.NoConnection, CodeSwitch.NoConnection), offline)
        assertTrue(gateway.calls.isEmpty())
        assertFalse(manager.isSwitching)
        assertTrue(states.isEmpty())
        assertTrue(store.pushNotificationEnabled)

        online = true
        gateway.unregisterResult = GatewayResult.failure("offline")
        var result: CodeSwitch? = null
        manager.applyCode("FGHIJ-67890") { result = it }
        assertEquals(CodeSwitch.NoConnection, result)
        assertEquals(listOf("unregister"), gateway.calls)
        assertEquals("ABCDE-12345", store.personalCode)
        assertFalse(manager.isSwitching)
        assertEquals(listOf(true, false), states)
        assertFalse(store.pushNotificationEnabled)
        assertTrue(inbox.calls.isEmpty())
    }

    @Test
    fun secondSwitchWhilePendingIsBusy() {
        gateway.deferCallbacks = true
        val states = mutableListOf<Pair<Boolean, String?>>()
        val listener: () -> Unit = { states.add(manager.isSwitching to store.personalCode) }
        manager.addSwitchListener(listener)
        var first: CodeSwitch? = null
        var second: CodeSwitch? = null
        manager.applyCode("ABCDE-12345") { first = it }
        manager.applyCode("FGHIJ-67890") { second = it }
        assertEquals(CodeSwitch.Busy, second)
        assertNull(first)
        assertTrue(manager.isSwitching)
        assertEquals(listOf(true to null), states)
        gateway.flush()
        assertEquals(CodeSwitch.Applied("ABCDE-12345"), first)
        assertEquals(1, gateway.calls.count { it == "unregister" })
        assertFalse(manager.isSwitching)
        assertEquals(listOf(true to null, false to "ABCDE-12345"), states)

        manager.removeSwitchListener(listener)
        manager.removeCode {}
        gateway.flush()
        assertEquals(2, states.size)
    }

    @Test
    fun registerFailureStillAppliesCode() {
        gateway.registerResult = GatewayResult.failure("denied")
        var result: CodeSwitch? = null
        manager.applyCode("ABCDE-12345") { result = it }
        assertEquals(CodeSwitch.Applied("ABCDE-12345"), result)
        assertEquals("ABCDE-12345", store.personalCode)
        assertFalse(store.pushNotificationEnabled)
        assertEquals(listOf("resync"), inbox.calls)
    }
}
