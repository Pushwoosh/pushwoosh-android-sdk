package com.pushwoosh.pushmart.live

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.annotation.StringRes
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.core.KeyValueStore
import com.pushwoosh.pushmart.ui.common.categoryTitle
import java.util.Date
import java.util.concurrent.CopyOnWriteArraySet

/** One upcoming product drop; [id] keys its alarm and its countdown notification. */
data class Drop(
    val id: String,
    val name: String,
    @StringRes val tagline: Int,
    val photo: String,
    val category: String,
)

/**
 * Upcoming drops, as iOS `FlashDropController`: Remind me sets an inexact alarm after the picked
 * delay, and the alarm posts PushMart's ongoing countdown to the drop, [LEAD_MILLIS] later, under
 * the drop id. In Doze the alarm can come late. A scheduled drop stays in prefs until Cancel or
 * until it goes live.
 */
class DropReminders(
    context: Context,
    private val prefs: KeyValueStore,
    private val notifier: LiveNotifier,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val context = context.applicationContext
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    val drops: List<Drop> =
        listOf(
            Drop(
                "drop_dress_04",
                "Plait Halter Sundress",
                R.string.drop_tagline_colorway,
                "drop_sundress",
                "Dresses"),
            Drop(
                "drop_outwear_02",
                "Linen Trenchcoat",
                R.string.drop_tagline_limited,
                "drop_trench",
                "Outerwear"),
            Drop(
                "drop_jeans_01",
                "Skinny Low Ankle Jeans",
                R.string.drop_tagline_restock,
                "drop_jeans",
                "Jeans"),
            Drop(
                "drop_tops_02",
                "Women's Linen Tee Shirt",
                R.string.drop_tagline_season,
                "drop_tee",
                "Tops"),
        )

    fun byId(id: String): Drop? = drops.firstOrNull { it.id == id }

    /** The countdown can show: Live updates are available (see [LiveNotifier.isAvailable]). */
    val countdownAvailable: Boolean
        get() = notifier.isAvailable

    /** When the reminder of [drop] goes off, or null when none is set or the drop is live. */
    fun scheduledAt(drop: Drop): Long? {
        val entries = prefs.getStringList(KEY)
        val entry = entries.firstOrNull { it.substringBefore(SEPARATOR) == drop.id } ?: return null
        val start = entry.substringAfter(SEPARATOR).toLongOrNull()
        if (start == null || now() >= start + LEAD_MILLIS) {
            prefs.putStringList(KEY, entries - entry)
            return null
        }
        return start
    }

    /**
     * Sets the reminder of [drop] [seconds] from now, through the Push Primer when needed.
     * Listeners fire either way.
     */
    fun schedule(drop: Drop, seconds: Int) {
        notifier.whenAllowed { granted ->
            if (granted) {
                val start = now() + seconds * 1000L
                val dropsAt = start + LEAD_MILLIS
                alarms()
                    ?.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, start, alarmIntent(drop, dropsAt))
                val others =
                    prefs.getStringList(KEY).filter { it.substringBefore(SEPARATOR) != drop.id }
                prefs.putStringList(KEY, others + (drop.id + SEPARATOR + start))
                PWLog.debug(TAG, "${drop.id} reminder in ${seconds}s")
            }
            notifyListeners()
        }
    }

    /** Removes the alarm, the countdown on screen and the stored reminder of [drop]. */
    fun cancel(drop: Drop) {
        alarms()?.cancel(alarmIntent(drop, 0L))
        notifier.cancel(drop.id)
        prefs.putStringList(
            KEY, prefs.getStringList(KEY).filter { it.substringBefore(SEPARATOR) != drop.id })
        notifyListeners()
    }

    /** Posts the countdown of the drop the alarm [intent] names, unless it is unknown or live. */
    fun onAlarm(intent: Intent) {
        val drop = byId(intent.getStringExtra(EXTRA_ID).orEmpty()) ?: return
        val dropsAt = intent.getLongExtra(EXTRA_DROPS_AT, 0L)
        val left = dropsAt - now()
        if (left <= 0) return
        val time = DateFormat.getTimeFormat(context).format(Date(dropsAt))
        notifier.show(
            drop.id,
            LiveContent(
                context.getString(R.string.drop_alert_title, drop.name),
                context.getString(
                    R.string.drop_alert_body,
                    context.categoryTitle(drop.category),
                    context.getString(drop.tagline),
                    time),
                notifier.openApp(),
                promoted = true,
                largeIcon = notifier.photo(drop.photo),
                countdownTo = dropsAt,
                timeoutAfterMillis = left,
            ),
        )
    }

    fun addListener(l: () -> Unit) {
        listeners.add(l)
    }

    fun removeListener(l: () -> Unit) {
        listeners.remove(l)
    }

    private fun alarms(): AlarmManager? = context.getSystemService(AlarmManager::class.java)

    private fun alarmIntent(drop: Drop, dropsAt: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            drop.id.hashCode(),
            Intent(context, DropAlarmReceiver::class.java)
                .putExtra(EXTRA_ID, drop.id)
                .putExtra(EXTRA_DROPS_AT, dropsAt),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    companion object {
        const val KEY = "pushmart.drops"
        const val LEAD_MILLIS = 600_000L
        const val EXTRA_ID = "dropId"
        const val EXTRA_DROPS_AT = "dropsAt"
        private const val SEPARATOR = '|'
        private const val TAG = "DropReminders"
    }
}
