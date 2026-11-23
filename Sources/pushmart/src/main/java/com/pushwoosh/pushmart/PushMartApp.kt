package com.pushwoosh.pushmart

import android.app.Application
import com.pushwoosh.inapp.InAppManager
import com.pushwoosh.inapp.ui.PushwooshInAppUi
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition
import com.pushwoosh.inbox.ui.PushwooshInboxUi
import com.pushwoosh.pushmart.core.AppForeground
import com.pushwoosh.pushmart.sdk.PushMartJsBridge
import com.pushwoosh.pushmart.ui.offers.InboxButtonActions
import com.pushwoosh.richmedia.RichMediaColorScheme
import com.pushwoosh.richmedia.RichMediaManager
import com.pushwoosh.richmedia.RichMediaType

class PushMartApp : Application() {
    lateinit var services: Services
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        services = Services(this)
        val inboxActions = InboxButtonActions(this, services.wishlist, services.reminders)
        PushwooshInboxUi.onButtonClickListener = inboxActions
        PushwooshInboxUi.onMessageClickListener = inboxActions
        services.theme.apply()
        services.accountManager.applyOnLaunch()
        // The SDK keeps its own copy of the flag; the app's toggle is the source of truth.
        services.foregroundAlerts.let { it.set(it.enabled) }
        PushwooshInAppUi.delegate = services.inAppDelegate
        RichMediaManager.setDelegate(services.richMediaDelegate)
        registerActivityLifecycleCallbacks(
            AppForeground { foreground ->
                services.launcherIcon.setForeground(foreground)
                if (foreground) services.gateway.postEvent("appOpen", emptyMap())
            })
        RichMediaManager.setRichMediaType(RichMediaType.MODAL)
        RichMediaManager.setDefaultRichMediaConfig(
            ModalRichmediaConfig()
                .setViewPosition(ModalRichMediaViewPosition.BOTTOM)
                .setPresentAnimationType(ModalRichMediaPresentAnimationType.DROP_DOWN)
                .setDismissAnimationType(ModalRichMediaDismissAnimationType.SLIDE_RIGHT))
        // Follows the App settings theme and replaces a scheme stored by an earlier build.
        RichMediaManager.setRichMediaColorScheme(RichMediaColorScheme.APP)
        InAppManager.getInstance().addJavascriptInterface(PushMartJsBridge(), "PushMart")
    }

    companion object {
        /** Set first in [onCreate], for code the SDK creates without a context. */
        lateinit var instance: PushMartApp
            private set
    }
}
