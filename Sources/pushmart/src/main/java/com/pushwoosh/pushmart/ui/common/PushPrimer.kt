package com.pushwoosh.pushmart.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentActivity
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.pushmart.core.KeyValueStore
import com.pushwoosh.pushmart.core.PrimerOutcome
import com.pushwoosh.pushmart.core.PushPermission
import java.lang.ref.WeakReference

/**
 * The Push Primer, PushMart's own bottom sheet in place of the iOS SDK `pushPrimer` (D3). It is
 * skipped when notifications are allowed. Accept while the system has not asked yet means register:
 * the SDK shows the system dialog itself. Accept after a denial opens the app's notification
 * settings, as iOS `fallbackToSettings(true)`.
 */
class PushPrimer(context: Context, private val prefs: KeyValueStore) : PushPermission {
    private val context = context.applicationContext
    private var host: WeakReference<FragmentActivity>? = null
    private var pending: ((PrimerOutcome) -> Unit)? = null

    override val isAllowed: Boolean
        get() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    override val isUndetermined: Boolean
        get() =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !isAllowed &&
                !prefs.getBoolean(REQUESTED_KEY)

    /** True while a shown primer waits for its answer. */
    val isPrompting: Boolean
        get() = pending != null

    fun attach(activity: FragmentActivity) {
        host = WeakReference(activity)
    }

    fun detach(activity: FragmentActivity) {
        if (host?.get() === activity) host = null
    }

    override fun enable(onOutcome: (PrimerOutcome) -> Unit) {
        if (isAllowed) {
            onOutcome(PrimerOutcome.SUPPRESSED)
            return
        }
        val fm = host?.get()?.supportFragmentManager
        if (fm == null || pending != null) {
            onOutcome(PrimerOutcome.DECLINED)
            return
        }
        pending = onOutcome
        if (!PageSheet.showOnce(fm, PushPrimerSheet(), TAG)) finish(false)
    }

    /** Delivers the shown primer's answer once; later calls do nothing. */
    fun finish(accepted: Boolean) {
        val onOutcome = pending ?: return
        pending = null
        val outcome =
            when {
                !accepted -> PrimerOutcome.DECLINED
                isUndetermined -> {
                    prefs.putBoolean(REQUESTED_KEY, true)
                    PrimerOutcome.ACCEPTED
                }
                !isAllowed -> {
                    openSettings()
                    PrimerOutcome.DECLINED
                }
                else -> PrimerOutcome.SUPPRESSED
            }
        PWLog.debug(LOG_TAG, "primer: $outcome")
        onOutcome(outcome)
    }

    private fun openSettings() {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null))
            }
        val activity = host?.get()
        try {
            if (activity != null) {
                activity.startActivity(intent)
            } else {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } catch (e: ActivityNotFoundException) {
            PWLog.error(LOG_TAG, "no notification settings screen", e)
        }
    }

    companion object {
        const val TAG = "push-primer"
        private const val LOG_TAG = "PushPrimer"
        private const val REQUESTED_KEY = "pushmart.pushPermissionRequested"
    }
}
