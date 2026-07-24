package com.pushwoosh.inapp.ui.presentation

import android.graphics.Color
import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.inapp.ui.model.ModalContent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InAppFrequencyStoreTest {

    private lateinit var store: InAppFrequencyStore

    @Before
    fun setup() {
        store = InAppFrequencyStore(RuntimeEnvironment.getApplication())
        store.enabled = true
    }

    private fun message(
        id: String?,
        maxDisplays: Int? = null,
        cooldownSec: Long? = null,
        expireEpochSec: Long? = null
    ) = InAppMessage(id, modalLayout(), maxDisplays, cooldownSec, expireEpochSec, "{}")

    private fun modalLayout() =
        InAppLayout.Modal(ModalContent(Color.WHITE, null, null, null, true, emptyList(), true))

    /// Verifies a message with no caps is always allowed.
    @Test
    fun allowsWhenNoCaps() {
        assertTrue(store.canShow(message("a")))
    }

    /// Verifies maxDisplays blocks the message once the count is reached.
    @Test
    fun enforcesMaxDisplays() {
        val m = message("a", maxDisplays = 1)
        assertTrue(store.canShow(m))
        store.recordShown(m)
        assertFalse(store.canShow(m))
    }

    /// Verifies cooldown blocks a repeat within the window and allows it after.
    @Test
    fun enforcesCooldown() {
        val m = message("a", cooldownSec = 60)
        store.recordShown(m, nowSec = 1000)
        assertFalse(store.canShow(m, nowSec = 1030))
        assertTrue(store.canShow(m, nowSec = 1100))
    }

    /// Verifies expiry is enforced even when the opt-in caps are disabled.
    @Test
    fun expiryEnforcedEvenWhenDisabled() {
        store.enabled = false
        val m = message("a", expireEpochSec = 500)
        assertFalse(store.canShow(m, nowSec = 600))
        assertTrue(store.canShow(m, nowSec = 400))
    }

    /// Verifies count/cooldown caps are ignored while the opt-in flag is off.
    @Test
    fun capsIgnoredWhenDisabled() {
        store.enabled = false
        val m = message("a", maxDisplays = 1)
        store.recordShown(m)
        assertTrue(store.canShow(m))
    }

    /// Verifies a message without an id is never capped.
    @Test
    fun nullIdSkipsCaps() {
        val m = message(null, maxDisplays = 1)
        store.recordShown(m)
        assertTrue(store.canShow(m))
    }

    /// Verifies expiry is exclusive: a message is shown at its exact expiry second, blocked only after.
    @Test
    fun expiryIsExclusiveAtExactSecond() {
        val m = message("a", expireEpochSec = 500)
        assertTrue(store.canShow(m, nowSec = 500))
        assertFalse(store.canShow(m, nowSec = 501))
    }

    /// Verifies the cooldown compares strictly: a repeat exactly cooldown seconds later is allowed.
    @Test
    fun cooldownBoundaryIsExclusive() {
        val m = message("a", cooldownSec = 60)
        store.recordShown(m, nowSec = 1000)
        assertFalse(store.canShow(m, nowSec = 1059))
        assertTrue(store.canShow(m, nowSec = 1060))
    }

    /// Verifies a never-shown message has no cooldown floor (the last-shown guard skips the check).
    @Test
    fun neverShownIgnoresCooldown() {
        val m = message("a", cooldownSec = 60)
        assertTrue(store.canShow(m, nowSec = 10))
    }

    /// Verifies canShow with no explicit time consults the real clock — an ancient expiry is blocked.
    @Test
    fun usesRealClockWhenNowOmitted() {
        val m = message("a", expireEpochSec = 1)
        assertFalse(store.canShow(m))
    }
}
