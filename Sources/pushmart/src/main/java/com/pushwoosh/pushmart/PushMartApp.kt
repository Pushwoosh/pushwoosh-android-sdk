package com.pushwoosh.pushmart

import android.app.Application
import com.pushwoosh.inapp.ui.PushwooshInAppUi
import com.pushwoosh.richmedia.RichMediaManager

class PushMartApp : Application() {
    lateinit var services: Services
        private set

    override fun onCreate() {
        super.onCreate()
        services = Services(this)
        PushwooshInAppUi.delegate = services.inAppDelegate
        RichMediaManager.setDelegate(services.richMediaDelegate)
    }
}
