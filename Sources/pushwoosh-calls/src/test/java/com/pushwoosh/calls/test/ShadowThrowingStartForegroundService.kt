package com.pushwoosh.calls.test

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowService

/**
 * Stand-in for the OS foreground-service policy used by crash repro #15.
 *
 * On API 31+ the platform Service.startForeground(id, notification, type) throws
 * ForegroundServiceStartNotAllowedException when a FGS is started from the background without an
 * allowed exemption. Robolectric's default ShadowService no-ops that call, which is why the crash
 * cannot surface with the stock shadow. This shadow makes the 3-arg startForeground throw every time,
 * turning the OEM/version/race-dependent OS denial into a deterministic condition while leaving the
 * real PushwooshCallService reach path (startForegroundNotification -> onStartCommand) unmodified.
 *
 * Applied only to the repro methods via method-level @Config(shadows = ...), so the "exemption granted"
 * barrier control runs with the stock non-throwing ShadowService.
 */
@Implements(Service::class)
class ShadowThrowingStartForegroundService : ShadowService() {

    @Implementation
    override fun startForeground(id: Int, notification: Notification, foregroundServiceType: Int) {
        throw ForegroundServiceStartNotAllowedException(
            "stand-in: startForeground() denied — FGS start from background not allowed")
    }
}
