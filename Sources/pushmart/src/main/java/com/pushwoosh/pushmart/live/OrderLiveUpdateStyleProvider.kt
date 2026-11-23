package com.pushwoosh.pushmart.live

import android.app.Notification
import android.os.Build
import androidx.annotation.RequiresApi
import com.pushwoosh.liveupdates.LiveUpdateProgressStyleProvider
import com.pushwoosh.liveupdates.LiveUpdateState
import com.pushwoosh.pushmart.PushMartApp

/**
 * Draws a server Live Update as PushMart's order bar, the one the app posts after checkout. The
 * phase comes from the push title with the iOS widget's keyword rule; the payload's segments and
 * progress are ignored.
 *
 * Stateless. The SDK creates it by reflection with no context and calls [createStyle] on a worker
 * thread once per rendered push, after `Application.onCreate`, so resources come from
 * [PushMartApp.instance].
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class OrderLiveUpdateStyleProvider : LiveUpdateProgressStyleProvider {
    override fun createStyle(state: LiveUpdateState): Notification.ProgressStyle =
        OrderProgressStyle.build(PushMartApp.instance, OrderStage.from(state.title))
}
