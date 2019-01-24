package com.pushwoosh.sample

import android.app.Application


import android.util.Log
import com.pushwoosh.Pushwoosh

import com.pushwoosh.location.PushwooshLocation
import com.pushwoosh.notification.LocalNotification
import com.pushwoosh.tags.Tags

class PushwooshSampleApp : Application() {
    companion object {
        const val LTAG = "PushwooshSample"
    }

    override fun onCreate() {
        super.onCreate()

        Pushwoosh.getInstance().registerForPushNotifications { result ->
            when(result.isSuccess) {
                true -> Log.d(LTAG, "Successfully registered for push notifications with token: " + result.data!!)
                false -> Log.d(LTAG, "Failed to register for push notifications:u " + result.exception!!.message)
            }
        }

        PushwooshLocation.startLocationTracking()

        Pushwoosh.getInstance().sendTags(Tags.intTag("fav_number", 42))

        val notification = LocalNotification.Builder()
                .setMessage("Local notification")
                .setDelay(5)
                .build()

        Pushwoosh.getInstance().scheduleLocalNotification(notification)
    }
}
