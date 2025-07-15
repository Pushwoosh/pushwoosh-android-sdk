package com.pushwoosh.calls.util

import android.app.Notification
import android.app.Notification.Action
import android.app.Notification.VISIBILITY_PUBLIC
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.pushwoosh.calls.ui.IncomingCallActivity
import com.pushwoosh.calls.ui.PhoneNumbersPermissionActivity
import com.pushwoosh.calls.PushwooshCallPlugin
import com.pushwoosh.calls.PushwooshCallReceiver
import com.pushwoosh.calls.service.PushwooshConnectionService
import com.pushwoosh.calls.PushwooshVoIPMessage
import com.pushwoosh.calls.R
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.internal.utils.RequestPermissionHelper
import com.pushwoosh.notification.builder.AppIconHelper
import androidx.core.net.toUri


object PushwooshCallUtils {
    @RequiresApi(Build.VERSION_CODES.O)
    fun registerPhoneAccount() {
        val context = AndroidPlatformModule.getApplicationContext()
        val telecomManager = context?.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val callPrefs = PushwooshCallPlugin.instance.callPrefs

        val componentName = ComponentName(context, PushwooshConnectionService::class.java)
        val phoneAccountHandle = PhoneAccountHandle(componentName, callPrefs.getPhoneAccountHandle())
        val phoneAccount = PhoneAccount.Builder(phoneAccountHandle, callPrefs.getPhoneAccount())
            .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
            .build()
        telecomManager.registerPhoneAccount(phoneAccount)

        val isEnabled = telecomManager.getPhoneAccount(phoneAccountHandle)?.isEnabled == true
        if (!isEnabled) {
            val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    private fun getRingtoneUri(): Uri {
        if (PushwooshCallPlugin.instance.callPrefs.getCallSoundName() != "default") {
            val context = AndroidPlatformModule.getApplicationContext()
            val soundName = PushwooshCallPlugin.instance.callPrefs.getCallSoundName()

            val resName = soundName.substringBeforeLast('.') // "sound.wav" → "sound"
            val resId = context?.resources?.getIdentifier(resName, "raw", context.packageName)

            if (resId == 0) {
                PWLog.warn("Pushwoosh", "Sound resource '$soundName' not found in /res/raw")
                return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            val uri = "android.resource://${context?.packageName}/$resId".toUri()
            return uri
        } else {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }

    }

    fun registerCallNotificationsChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val context = AndroidPlatformModule.getApplicationContext()
        val callPrefs = PushwooshCallPlugin.instance.callPrefs

        val incomingCallChannel = NotificationChannel(
            callPrefs.getIncomingCallChannelName(), callPrefs.getIncomingCallChannelName(),
            NotificationManager.IMPORTANCE_HIGH
        )

        val ringtoneUri = getRingtoneUri()
        incomingCallChannel.setSound(
            ringtoneUri,
            AudioAttributes.Builder() // Setting the AudioAttributes is important as it identifies the purpose of your
                // notification sound.
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )

        incomingCallChannel.lockscreenVisibility = VISIBILITY_PUBLIC


        val ongoingCallChannel = NotificationChannel(
            callPrefs.getOngoingCallChannelName(), callPrefs.getOngoingCallChannelName(),
            NotificationManager.IMPORTANCE_NONE
        )
        ongoingCallChannel.setSound(null,null)


        val mgr: NotificationManager? = context?.getSystemService(NotificationManager::class.java)
        mgr?.createNotificationChannel(incomingCallChannel)
        mgr?.createNotificationChannel(ongoingCallChannel)
    }

    private fun getFullscreenIntent(payload: Bundle?) : Intent {
        val context = AndroidPlatformModule.getApplicationContext() ?: return Intent(Intent.ACTION_MAIN)
                                                                                .addCategory(Intent.CATEGORY_DEFAULT)
        val intent = Intent(context, IncomingCallActivity::class.java)
        if (payload != null) {
            intent.putExtras(payload)
        }

        val packageManager = context.packageManager
        val matches = packageManager.queryIntentActivities(intent, 0)

        return if (matches.isNotEmpty()) {
            intent
        } else {
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_DEFAULT)
        }
    }

    fun buildIncomingCallNotification(payload: Bundle?): Notification {
        val context = AndroidPlatformModule.getApplicationContext() as Context
        val intent = getFullscreenIntent(payload)
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_NEW_TASK)
        val fullscreenIntent =
            PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE)

        val acceptIntent = Intent(context, PushwooshCallReceiver::class.java).apply {
            action = "ACTION_ACCEPT_CALL"
        }
        val rejectIntent = Intent(context, PushwooshCallReceiver::class.java).apply {
            action = "ACTION_REJECT_CALL"
        }

        if (payload != null) {
            acceptIntent.putExtras(payload)
            rejectIntent.putExtras(payload)
        }

        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification : Notification
        val voIPMessage = PushwooshVoIPMessage(payload)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = Notification.Builder(
                context,
                PushwooshCallPlugin.instance.callPrefs.getIncomingCallChannelName()
            )
            builder.setOngoing(true)
            builder.setCategory(Notification.CATEGORY_CALL)
            builder.setPriority(Notification.PRIORITY_MAX)

            builder.setContentIntent(null)
            builder.setFullScreenIntent(fullscreenIntent, true)

            builder.setSmallIcon(
                AppIconHelper.getAppIcon(
                AndroidPlatformModule.getApplicationContext(),
                AndroidPlatformModule.getAppInfoProvider().getPackageName()))
            builder.setContentTitle(voIPMessage.callerName)
            builder.setContentText("Incoming call")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val caller = Person.Builder()
                    .setIcon(AppIconHelper.getAppIcon(
                        AndroidPlatformModule.getApplicationContext(),
                        AndroidPlatformModule.getAppInfoProvider().getPackageName()))
                    .setName(voIPMessage.callerName)
                    .setImportant(true)
                    .build()

                builder.setStyle(Notification.CallStyle.forIncomingCall(caller, rejectPendingIntent, acceptPendingIntent))

            } else {
                builder.addAction(
                    Action.Builder(
                    Icon.createWithResource(context, R.drawable.round_call_24_white),
                    "ACCEPT",
                    acceptPendingIntent)
                    .build()
                )

                builder.addAction(
                    Action.Builder(
                    Icon.createWithResource(context, R.drawable.round_call_end_24_white),
                    "REJECT",
                    rejectPendingIntent)
                    .build()
                )
            }

            notification = builder.build()

        } else {
            val builder = NotificationCompat.Builder(context)
            builder.setOngoing(true)
            builder.setCategory(Notification.CATEGORY_CALL)
            builder.setPriority(Notification.PRIORITY_MAX)

            builder.setContentIntent(null)
            builder.setFullScreenIntent(fullscreenIntent, true)

            builder.setSmallIcon(AppIconHelper.getAppIconResId(context))
            builder.setContentTitle(voIPMessage.callerName)
            builder.setContentText("Incoming call")
            builder.addAction(R.drawable.round_call_24_white, "ACCEPT", acceptPendingIntent)
            builder.addAction(R.drawable.round_call_end_24_white, "DECLINE", rejectPendingIntent)

            notification = builder.build()
        }
        notification.flags = notification.flags or Notification.FLAG_INSISTENT

        return notification
    }

    fun buildOngoingCallNotification(payload: Bundle?): Notification {
        val context = AndroidPlatformModule.getApplicationContext() as Context
        val voIPMessage = PushwooshVoIPMessage(payload)

        val endCallIntent = Intent(context, PushwooshCallReceiver::class.java).apply {
            action = "ACTION_END_CALL"
        }

        if (payload != null) {
            endCallIntent.putExtras(payload)
        }

        val endCallPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            endCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = Notification.Builder(
                context,
                PushwooshCallPlugin.instance.callPrefs.getOngoingCallChannelName()
            )
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_CALL)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(
                    AppIconHelper.getAppIcon(
                        context,
                        AndroidPlatformModule.getAppInfoProvider().getPackageName()
                    )
                )
                .setContentTitle(voIPMessage.callerName)
                .setContentText("Ongoing call")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val caller = Person.Builder()
                    .setIcon(
                        AppIconHelper.getAppIcon(
                            context,
                            AndroidPlatformModule.getAppInfoProvider().getPackageName()
                        )
                    )
                    .setName(voIPMessage.callerName)
                    .setImportant(true)
                    .build()

                builder.setStyle(Notification.CallStyle.forOngoingCall(caller, endCallPendingIntent))
            } else {
                builder.addAction(
                    Action.Builder(
                        Icon.createWithResource(context, R.drawable.round_call_end_24_white),
                        "END",
                        endCallPendingIntent
                    ).build()
                )
            }

            notification = builder.build()
        } else {
            val builder = NotificationCompat.Builder(context)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(AppIconHelper.getAppIconResId(context))
                .setContentTitle("Call in progress")
                .setContentText("Connected to ${voIPMessage.callerName}")
                .addAction(R.drawable.round_call_end_24_white, "END", endCallPendingIntent)

            notification = builder.build()
        }

        return notification
    }

}