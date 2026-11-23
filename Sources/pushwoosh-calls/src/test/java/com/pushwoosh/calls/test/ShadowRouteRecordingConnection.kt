package com.pushwoosh.calls.test

import android.os.Build
import android.telecom.Connection
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

// Records the routes requested from Telecom; Robolectric has no Telecom behind Connection.setAudioRoute.
@Implements(Connection::class)
class ShadowRouteRecordingConnection {
    val requestedRoutes = mutableListOf<Int>()

    @Implementation(minSdk = Build.VERSION_CODES.O)
    fun setAudioRoute(route: Int) {
        requestedRoutes += route
    }
}
