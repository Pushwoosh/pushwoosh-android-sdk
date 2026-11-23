package com.pushwoosh.pushmart.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pushwoosh.pushmart.services

/** The reminder alarm of an upcoming drop: posts its countdown (see [DropReminders.onAlarm]). */
class DropAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.services.drops.onAlarm(intent)
    }
}
