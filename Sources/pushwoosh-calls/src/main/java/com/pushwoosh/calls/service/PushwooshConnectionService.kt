package com.pushwoosh.calls.service

import android.content.Intent
import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import com.pushwoosh.calls.PushwooshCallPlugin
import com.pushwoosh.calls.PushwooshConnection
import com.pushwoosh.calls.util.Constants
import com.pushwoosh.calls.util.PushwooshCallUtils
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.utils.PWLog

/**
 * Manages VoIP call connections through Android Telecom Framework.
 *
 * This service is invoked by Android system after [android.telecom.TelecomManager.addNewIncomingCall]
 * to create and manage VoIP call connections. Handles incoming call creation, blocks concurrent calls,
 * and launches foreground service for call notifications.
 *
 * @todo Missing [onConnectionServiceFocusLost] and [onConnectionServiceFocusGained] for API 28+ (concurrent calls)
 */
class PushwooshConnectionService : ConnectionService() {
    companion object {
        private const val TAG = "PushwooshConnectionService"

        // Lock for synchronizing access to activeConnection
        private val connectionLock = Any()

        @Volatile
        private var _activeConnection: PushwooshConnection? = null

        // Thread-safe getter (volatile guarantees atomic read and visibility)
        fun getActiveConnection(): PushwooshConnection? = _activeConnection

        // Thread-safe setter
        fun setActiveConnection(connection: PushwooshConnection?) {
            synchronized(connectionLock) {
                _activeConnection = connection
            }
        }

        /**
         * Atomically clears activeConnection only if it equals the expected connection.
         * This prevents race conditions where a new call arrives while clearing an old one.
         *
         * @param expected The connection that should be cleared
         * @return true if connection was cleared, false if it was already different
         */
        fun clearActiveConnectionIfEquals(expected: PushwooshConnection?): Boolean {
            synchronized(connectionLock) {
                if (_activeConnection === expected) {
                    _activeConnection = null
                    return true
                }
                return false
            }
        }
    }
    /**
     * Creates a VoIP call connection when incoming call arrives.
     *
     * Called by Android Telecom after [android.telecom.TelecomManager.addNewIncomingCall].
     * Creates a [PushwooshConnection] to represent the call, starts [PushwooshCallService]
     * foreground service to display persistent notification, and notifies the app via callback.
     *
     * The foreground service keeps the process alive and shows call UI (Accept/Reject buttons).
     * VoIP push payload is passed to all components: Connection → Service → App callback.
     *
     * Rejects concurrent calls with BUSY status - only one active call supported.
     *
     * @param request contains VoIP push payload in extras (caller info, metadata)
     * @return [PushwooshConnection] representing the call, or failed connection if rejected
     */
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        PWLog.noise(TAG, "onCreateIncomingConnection()")

        return try {
            // Atomic check and set new connection
            val newConnection = synchronized(connectionLock) {
                val current = _activeConnection
                if (current != null && current.state != Connection.STATE_DISCONNECTED) {
                    PWLog.warn(TAG, "Rejecting incoming call: another call is active (state=${current.state})")
                    return@synchronized null
                }

                val connection = PushwooshConnection(PushwooshCallPlugin.instance.callEventListener)
                connection.setRinging()
                _activeConnection = connection
                connection
            }

            if (newConnection == null) {
                return Connection.createFailedConnection(
                    DisconnectCause(DisconnectCause.BUSY)
                )
            }

            PushwooshCallUtils.registerCallNotificationsChannels()

            val callIntent = Intent(
                AndroidPlatformModule.getApplicationContext(),
                PushwooshCallService::class.java)
            val payload = request?.extras
            payload?.let {
                callIntent.action = Constants.PW_POST_INCOMING_CALL_ACTION
                callIntent.putExtras(payload)
                PWLog.debug(TAG, "Starting PushwooshCallService")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(callIntent)
                } else {
                    startService(callIntent)
                }
            }

            try {
                PushwooshCallPlugin.instance.callEventListener.onCreateIncomingConnection(payload)
            } catch (e: Exception) {
                PWLog.error(TAG, "User callback onCreateIncomingConnection() threw exception", e)
            }

            PWLog.info(TAG, "Incoming call connection created successfully")
            newConnection

        } catch (e: Exception) {
            PWLog.error(TAG, "Failed to create incoming connection", e)

            // Return failed connection instead of crashing
            Connection.createFailedConnection(
                DisconnectCause(DisconnectCause.ERROR, e.message)
            )
        }
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        PWLog.noise(TAG, "onCreateOutgoingConnection()")
        return super.onCreateOutgoingConnection(connectionManagerPhoneAccount, request)
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        PWLog.noise(TAG, "onCreateIncomingConnectionFailed()")
        PWLog.warn(TAG, "Failed to create incoming connection: account=$connectionManagerPhoneAccount, extras=${request?.extras}, address=${request?.address}")
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
    }
}
