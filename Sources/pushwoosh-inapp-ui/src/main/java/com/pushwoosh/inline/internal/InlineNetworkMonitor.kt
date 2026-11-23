package com.pushwoosh.inline.internal

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import com.pushwoosh.internal.platform.utils.GeneralUtils
import com.pushwoosh.internal.utils.PWLog

/**
 * Calls [onAvailable] on the main thread whenever a network with internet access becomes available, and
 * again once it is validated: the first signal can come before the connectivity state shows it as active.
 */
internal class InlineNetworkMonitor(context: Context, private val onAvailable: () -> Unit) {
    private val connectivity: ConnectivityManager? = context.getSystemService(ConnectivityManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    /** Networks already reported as validated; touched only on the connectivity callback thread. */
    private val validated = HashSet<Network>()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            handler.post { onAvailable() }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                validated.remove(network)
                return
            }
            if (validated.add(network)) handler.post { onAvailable() }
        }

        override fun onLost(network: Network) {
            validated.remove(network)
        }
    }

    // ACCESS_NETWORK_STATE comes from the core manifest; an app that strips it gets a SecurityException, caught below.
    @SuppressLint("MissingPermission")
    fun start() {
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        try {
            connectivity?.registerNetworkCallback(request, callback)
        } catch (e: RuntimeException) {
            PWLog.warn(TAG, "Network changes are not observed, queued inline events wait for the next session: ${e.message}")
        }
    }

    fun stop() {
        try {
            connectivity?.unregisterNetworkCallback(callback)
        } catch (e: RuntimeException) {
            PWLog.warn(TAG, "Could not stop observing network changes: ${e.message}")
        }
    }

    /** `null` when the connectivity state cannot be read (an app without ACCESS_NETWORK_STATE). */
    fun isOnline(): Boolean? = try {
        GeneralUtils.isNetworkAvailable()
    } catch (e: RuntimeException) {
        null
    }

    private companion object {
        const val TAG = "InlineNetworkMonitor"
    }
}
