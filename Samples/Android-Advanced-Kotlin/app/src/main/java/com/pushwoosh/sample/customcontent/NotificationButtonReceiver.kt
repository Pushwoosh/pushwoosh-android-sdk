package com.pushwoosh.sample.customcontent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.app.NotificationManagerCompat
import android.util.Log

class NotificationButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Pushwoosh", "notification button clicked")

        val managerCompat = NotificationManagerCompat.from(context)
        managerCompat.cancelAll()

        val actionIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pushwoosh.com/"))
        actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(actionIntent)
    }
}
