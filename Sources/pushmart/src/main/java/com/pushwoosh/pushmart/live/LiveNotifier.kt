package com.pushwoosh.pushmart.live

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pushwoosh.pushmart.MainActivity
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.account.AccountManager
import com.pushwoosh.pushmart.core.PushPermission
import java.util.concurrent.ConcurrentHashMap

/** What one PushMart ongoing notification shows. */
data class LiveContent(
    val title: CharSequence,
    val text: CharSequence,
    val tap: PendingIntent,
    val promoted: Boolean = false,
    val stage: OrderStage? = null,
    val header: CharSequence? = null,
    val largeIcon: Bitmap? = null,
    val countdownTo: Long? = null,
    val timeoutAfterMillis: Long? = null,
    val action: Pair<CharSequence, PendingIntent>? = null,
)

/**
 * PushMart's own ongoing notifications, the Android stand-in for iOS Live Activities: the order,
 * the drop countdown and the picks. Each is keyed by `tag` and `tag.hashCode()`, as the SDK keys a
 * Live Update, so a server Live Update with the same id replaces it in place. All of them share the
 * silent [CHANNEL_ID] channel.
 */
class LiveNotifier(
    context: Context,
    private val permission: PushPermission,
    private val account: AccountManager,
) {
    private val context = context.applicationContext
    private val manager = NotificationManagerCompat.from(this.context)
    private val postedAt = ConcurrentHashMap<String, Long>()
    private val cancelledAt = ConcurrentHashMap<String, Long>()

    /** As iOS `areActivitiesEnabled`: allowed or never asked, and the channel is not blocked. */
    val isAvailable: Boolean
        get() = (permission.isAllowed || permission.isUndetermined) && !isChannelBlocked

    /** A notification posted now would show. */
    val isAllowed: Boolean
        get() = permission.isAllowed && !isChannelBlocked

    private val isChannelBlocked: Boolean
        get() =
            manager.getNotificationChannelCompat(CHANNEL_ID)?.importance ==
                NotificationManagerCompat.IMPORTANCE_NONE

    /**
     * Calls [onDone] with true once notifications can show. Never asked or denied goes through
     * `setPushEnabled(true)`, so the Push Primer comes first; a blocked channel answers false.
     */
    fun whenAllowed(onDone: (granted: Boolean) -> Unit) {
        when {
            isAllowed -> onDone(true)
            isChannelBlocked -> onDone(false)
            else -> account.setPushEnabled(true) { on -> onDone(on && isAllowed) }
        }
    }

    /**
     * Posts or replaces the notification under [tag]. False, and nothing posted, when not allowed.
     */
    @SuppressLint("MissingPermission")
    fun show(tag: String, content: LiveContent): Boolean {
        if (!isAllowed) return false
        ensureChannel()
        val notification =
            if (content.promoted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                promoted(content)
            } else {
                compat(content)
            }
        manager.notify(tag, tag.hashCode(), notification)
        cancelledAt.remove(tag)
        postedAt[tag] = SystemClock.elapsedRealtime()
        return true
    }

    fun cancel(tag: String) {
        postedAt.remove(tag)
        cancelledAt[tag] = SystemClock.elapsedRealtime()
        manager.cancel(tag, tag.hashCode())
    }

    fun isActive(tag: String): Boolean = tag in activeTags(tag)

    /** Tags of PushMart notifications on screen that start with [prefix], the SDK's included. */
    fun activeTags(prefix: String): List<String> {
        val shown =
            try {
                context
                    .getSystemService(NotificationManager::class.java)
                    ?.activeNotifications
                    .orEmpty()
                    .mapNotNull { it.tag }
            } catch (e: RuntimeException) {
                emptyList()
            }
        val now = SystemClock.elapsedRealtime()
        // The system queues notify() and cancel(): its list catches up a moment later.
        val justPosted = postedAt.filterValues { now - it < POST_GRACE_MS }.keys
        val justCancelled = cancelledAt.filterValues { now - it < POST_GRACE_MS }.keys
        return (shown + justPosted)
            .filter { it.startsWith(prefix) && it !in justCancelled }
            .distinct()
    }

    fun openApp(): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PENDING_FLAGS,
        )

    fun openProduct(productId: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            productId.hashCode(),
            Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("pushwoosh://product/$productId"),
                    context,
                    MainActivity::class.java,
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PENDING_FLAGS,
        )

    /** A catalog photo by drawable name, downsampled for a notification. Null when missing. */
    @SuppressLint("DiscouragedApi")
    fun photo(drawableName: String): Bitmap? {
        val id = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
        if (id == 0) return null
        return BitmapFactory.decodeResource(
            context.resources, id, BitmapFactory.Options().apply { inSampleSize = 4 })
    }

    private fun ensureChannel() {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                    CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.live_channel_name))
                .setSound(null, null)
                .setVibrationEnabled(false)
                .build())
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun promoted(content: LiveContent): Notification {
        val builder =
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_pushmart)
                .setColor(ContextCompat.getColor(context, R.color.pm_coral))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentTitle(content.title)
                .setContentText(content.text)
                .setContentIntent(content.tap)
        // Raw key, as the SDK renderer: setRequestPromotedOngoing() is API 36.1.
        builder.addExtras(Bundle().apply { putBoolean("android.requestPromotedOngoing", true) })
        content.header?.let { builder.setSubText(it) }
        content.stage?.let {
            builder
                .setStyle(OrderProgressStyle.build(context, it))
                .setShortCriticalText(context.getString(it.label))
        }
        content.largeIcon?.let { builder.setLargeIcon(it) }
        content.countdownTo?.let {
            builder.setWhen(it).setUsesChronometer(true).setChronometerCountDown(true)
        }
        content.timeoutAfterMillis?.let { builder.setTimeoutAfter(it) }
        content.action?.let { (title, intent) ->
            builder.addAction(Notification.Action.Builder(null as Icon?, title, intent).build())
        }
        return builder.build()
    }

    private fun compat(content: LiveContent): Notification {
        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_pushmart)
                .setColor(ContextCompat.getColor(context, R.color.pm_coral))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentTitle(content.title)
                .setContentText(content.text)
                .setContentIntent(content.tap)
        val stage = content.stage
        if (stage != null) {
            builder
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(OrderCard.collapsed(context, content, stage))
                .setCustomBigContentView(OrderCard.expanded(context, content, stage))
        } else {
            content.largeIcon?.let { builder.setLargeIcon(it) }
        }
        content.countdownTo?.let {
            builder.setWhen(it).setUsesChronometer(true).setChronometerCountDown(true)
        }
        content.timeoutAfterMillis?.let { builder.setTimeoutAfter(it) }
        content.action?.let { (title, intent) -> builder.addAction(0, title, intent) }
        return builder.build()
    }

    companion object {
        const val CHANNEL_ID = "pushmart_live"
        private const val POST_GRACE_MS = 2_000L
        private const val PENDING_FLAGS =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    }
}
