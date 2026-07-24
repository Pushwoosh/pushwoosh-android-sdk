package com.pushwoosh.inapp.ui.presentation

import android.content.Context
import androidx.core.content.edit
import com.pushwoosh.inapp.ui.model.InAppMessage

/**
 * Opt-in display-frequency caps persisted per in-app id (SharedPreferences): max
 * displays (show-once / N times), cooldown between shows, and expiry. Survives
 * relaunch, unlike the in-memory queue dedupe.
 *
 * Expiry is always enforced (an expired config is never shown). The count/cooldown
 * caps apply ONLY when [enabled] is set AND the config provides them — so default
 * behaviour is unchanged for integrators who don't opt in.
 */
internal class InAppFrequencyStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Master switch for the count/cooldown caps.
     *
     * When `false` (the default), [canShow] enforces only message expiry, leaving
     * display frequency unrestricted. When `true`, per-message [InAppMessage.maxDisplays]
     * and [InAppMessage.cooldownSec] are additionally applied. Volatile because it may
     * be toggled and read from different threads.
     */
    @Volatile
    var enabled = false

    /**
     * Decides whether the given in-app message is allowed to be displayed right now.
     *
     * Checks are applied in order, returning `false` on the first cap that is hit:
     * - **Expiry** — always enforced; an expired message ([InAppMessage.expireEpochSec]
     *   in the past) is never shown, regardless of [enabled].
     * - **Enabled gate** — if [enabled] is `false`, or the message has no [InAppMessage.id],
     *   no further caps apply and the message is allowed.
     * - **Max displays** — blocked when the recorded display count reaches
     *   [InAppMessage.maxDisplays].
     * - **Cooldown** — blocked if the last show was less than [InAppMessage.cooldownSec]
     *   seconds ago.
     *
     * @param message the in-app message whose frequency caps are being evaluated
     * @param nowSec current time in epoch seconds; overridable for testing
     * @return `true` if the message may be shown, `false` if any active cap blocks it
     */
    fun canShow(message: InAppMessage, nowSec: Long = nowSeconds()): Boolean {
        message.expireEpochSec?.let { if (nowSec > it) return false }
        if (!enabled) return true
        val id = message.id ?: return true
        message.maxDisplays?.let { if (displayCount(id) >= it) return false }
        message.cooldownSec?.let { cooldown ->
            val last = lastShown(id)
            if (last > 0 && nowSec - last < cooldown) return false
        }
        return true
    }

    /**
     * Persists that the message was just displayed, so future [canShow] calls honour
     * its count and cooldown caps.
     *
     * Increments the stored display count and records the timestamp of this show,
     * both keyed by [InAppMessage.id]. Messages without an id are ignored (nothing to
     * track). Writes are committed asynchronously (the `edit` block defaults to `apply`).
     *
     * @param message the in-app message that was displayed
     * @param nowSec current time in epoch seconds; overridable for testing
     */
    fun recordShown(message: InAppMessage, nowSec: Long = nowSeconds()) {
        val id = message.id ?: return
        prefs.edit {
            putInt(KEY_COUNT + id, displayCount(id) + 1)
            putLong(KEY_LAST + id, nowSec)
        }
    }

    private fun displayCount(id: String) = prefs.getInt(KEY_COUNT + id, 0)

    private fun lastShown(id: String) = prefs.getLong(KEY_LAST + id, 0L)

    private fun nowSeconds() = System.currentTimeMillis() / 1000

    companion object {
        private const val PREFS = "pw_inapp_freq"
        private const val KEY_COUNT = "count_"
        private const val KEY_LAST = "last_"
    }
}
