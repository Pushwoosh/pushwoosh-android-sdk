package com.pushwoosh.calls.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import com.pushwoosh.calls.PushwooshCallPlugin
import com.pushwoosh.calls.PushwooshConnection
import com.pushwoosh.calls.PushwooshVoIPMessage
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
 */
class PushwooshConnectionService : ConnectionService() {
    companion object {
        private const val TAG = "PushwooshConnectionService"

        // Lock for synchronizing access to activeConnection
        private val connectionLock = Any()

        @Volatile
        private var _activeConnection: PushwooshConnection? = null

        private val callIdToConnectionMap = mutableMapOf<String, PushwooshConnection>()

        fun getActiveConnection(): PushwooshConnection? = _activeConnection

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

        /**
         * Stores mapping between callId and Connection for cancellation support.
         *
         * @param callId Server-provided call identifier
         * @param connection The PushwooshConnection to map
         */
        fun storeCallIdMapping(callId: String?, connection: PushwooshConnection) {
            callId?.let {
                synchronized(connectionLock) {
                    callIdToConnectionMap[it] = connection
                    PWLog.debug(TAG, "Stored callId mapping: $it")
                }
            }
        }

        /**
         * Finds connection by callId for cancellation.
         *
         * @param callId Server-provided call identifier
         * @return Connection if found, null otherwise
         */
        fun getConnectionByCallId(callId: String?): PushwooshConnection? {
            callId?.let {
                synchronized(connectionLock) {
                    return callIdToConnectionMap[it]
                }
            }
            return null
        }

        /**
         * Clears callId mapping when call ends.
         *
         * @param callId Server-provided call identifier to remove
         */
        fun clearCallIdMapping(callId: String?) {
            callId?.let {
                synchronized(connectionLock) {
                    callIdToConnectionMap.remove(it)
                    PWLog.debug(TAG, "Cleared callId mapping: $it")
                }
            }
        }

        /**
         * Atomically clears callId mapping only if it equals the expected connection.
         * This prevents race conditions where a new call arrives while clearing an old one.
         *
         * @param callId Server-provided call identifier
         * @param expected The connection that should be cleared
         * @return true if mapping was cleared, false if it was already different
         */
        fun clearCallIdMappingIfEquals(callId: String?, expected: PushwooshConnection?): Boolean {
            if (callId == null || expected == null) return false
            synchronized(connectionLock) {
                if (callIdToConnectionMap[callId] === expected) {
                    callIdToConnectionMap.remove(callId)
                    PWLog.debug(TAG, "Cleared callId mapping (matched): $callId")
                    return true
                }
                PWLog.debug(TAG, "Skip clearing callId mapping (mismatch): $callId")
                return false
            }
        }

        /**
         * Starts PushwooshCallService which shows call notification.
         *
         * @param action Service action (e.g. PW_POST_INCOMING_CALL_ACTION, PW_POST_ONGOING_CALL_ACTION)
         * @param payload Optional Bundle with call data (callerName, callId, etc.)
         */
        fun startCallNotificationService(action: String, payload: android.os.Bundle? = null) {
            PWLog.noise(TAG, "startCallNotificationService() action=$action")
            val context = AndroidPlatformModule.getApplicationContext()
            if (context == null) {
                PWLog.error(TAG, "Cannot start call notification service: context is null")
                return
            }

            val intent = Intent(context, PushwooshCallService::class.java)
            intent.action = action
            if (payload != null) {
                intent.putExtras(payload)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stops PushwooshCallService and removes call notification.
         *
         * @param payload Optional Bundle with call data for cleanup
         */
        fun stopCallNotificationService(payload: android.os.Bundle? = null) {
            PWLog.noise(TAG, "stopCallNotificationService()")
            val context = AndroidPlatformModule.getApplicationContext()
            if (context == null) {
                PWLog.error(TAG, "Cannot stop call notification service: context is null")
                return
            }

            val stopIntent = Intent(context, PushwooshCallService::class.java)
            if (payload != null) {
                stopIntent.putExtras(payload)
            }
            context.stopService(stopIntent)
        }

        private fun stopCallNotificationServiceAndCancelNotifications() {
            PWLog.noise(TAG, "stopCallNotificationServiceAndCancelNotifications()")
            val context = AndroidPlatformModule.getApplicationContext()

            // Cancel incoming call notification (defensive - Service should handle this too)
            val nm = context?.getSystemService(android.content.Context.NOTIFICATION_SERVICE)
                as? android.app.NotificationManager
            nm?.cancel(Constants.PW_NOTIFICATION_ID_INCOMING)
            PWLog.debug(TAG, "Cancelled incoming call notification")

            // Stop foreground service that shows call notification
            stopCallNotificationService()

            // Close IncomingCallActivity if it's visible on screen
            cancelCallActivity()
        }

        /**
         * Sends broadcast to close IncomingCallActivity.
         *
         * This is needed when call is cancelled remotely to dismiss FullScreenIntent activity
         * that may be visible on the screen.
         */
        private fun cancelCallActivity() {
            PWLog.noise(TAG, "cancelCallActivity()")
            val context = AndroidPlatformModule.getApplicationContext()
            if (context == null) {
                PWLog.error(TAG, "Cannot send broadcast to close call activity: context is null")
                return
            }

            val finishIntent = Intent(Constants.ACTION_FINISH_CALL_ACTIVITY).apply {
                setPackage(context.packageName)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(finishIntent)
        }

        private val timeoutHandler = Handler(Looper.getMainLooper())
        private var timeoutRunnable: Runnable? = null

        @Synchronized
        internal fun cancelTimeoutTimer() {
            PWLog.noise(TAG, "CancelTimeoutTimer()")

            timeoutRunnable?.let {
                timeoutHandler.removeCallbacks(it)
                timeoutRunnable = null
                PWLog.debug(TAG, "Cancelled timeout timer for incoming call")
            }
        }

        private fun handleTimeout() {
            PWLog.noise(TAG, "handleTimeout()")

            val timeoutSeconds = PushwooshCallPlugin.instance.callPrefs.getIncomingCallTimeout()
            PWLog.info(TAG, "Call timeout reached ($timeoutSeconds seconds). Reporting as unanswered.")

            val connection = getActiveConnection()
            if (connection == null) {
                PWLog.warn(TAG, "Cannot handle timeout: no active connection")
                return
            }

            if (connection.state != Connection.STATE_RINGING) {
                PWLog.warn(TAG, "Cannot timeout: no ringing connection")
                return
            }

            stopCallNotificationServiceAndCancelNotifications()

            if (connection.state != Connection.STATE_RINGING) {
                PWLog.warn(TAG, "Call state changed during timeout")
                return
            }


            connection.setDisconnected(DisconnectCause(DisconnectCause.MISSED))
            connection.destroy()
        }

        @Synchronized
        private fun startTimeoutTimer() {
            PWLog.noise(TAG, "startTimeoutTimer()")

            cancelTimeoutTimer()

            val timeoutSeconds = PushwooshCallPlugin.instance.callPrefs.getIncomingCallTimeout()
            PWLog.info(TAG, "Call timeout started: ($timeoutSeconds seconds).")
            val timeoutMillis = (timeoutSeconds * 1000).toLong()

            timeoutRunnable = Runnable { handleTimeout() }
            timeoutHandler.postDelayed(timeoutRunnable!!, timeoutMillis)
        }

        private fun isCallRingingOrFail(connection: PushwooshConnection, callId: String, reason: String): Boolean {
            if (connection.state != Connection.STATE_RINGING) {
                PWLog.warn(TAG, "Cannot cancel call: $reason (state=${connection.state})")
                PushwooshCallPlugin.instance.callEventListener.onCallCancellationFailed(callId, reason)
                return false
            }
            return true
        }

        /**
         * Closes the connection for a cancelled call.
         */
        private fun closeConnectionForCancelledCall(connection: PushwooshConnection, callId: String) {
            PWLog.noise(TAG, "closeConnectionForCancelledCall()")

            val voipMessage = PushwooshVoIPMessage(connection.extras)

            connection.setDisconnected(DisconnectCause(DisconnectCause.CANCELED, "Call cancelled by remote party"))
            connection.destroy()
            clearActiveConnectionIfEquals(connection)
            clearCallIdMappingIfEquals(callId, connection)

            try {
                PushwooshCallPlugin.instance.callEventListener.onCallCancelled(voipMessage)
            } catch (e: Exception) {
                PWLog.error(TAG, "User callback onCallCancelled() threw exception", e)
            }
        }

        /**
         * Cancels an incoming call by callId.
         */
        fun cancelIncomingCall(callId: String?) {
            PWLog.noise(TAG, "cancelIncomingCall(callId=$callId)")

            if (callId == null) {
                PWLog.warn(TAG, "Cannot cancel call: callId is null")
                PushwooshCallPlugin.instance.callEventListener.onCallCancellationFailed(
                    callId, "Missing callId"
                )
                return
            }

            val connection = getConnectionByCallId(callId)
            if (connection == null) {
                PWLog.warn(TAG, "Cannot cancel call: no connection found for callId=$callId")
                PushwooshCallPlugin.instance.callEventListener.onCallCancellationFailed(
                    callId, "No active call found"
                )
                return
            }

            if (!isCallRingingOrFail(connection, callId, "Call already answered")) return

            stopCallNotificationServiceAndCancelNotifications()

            if (!isCallRingingOrFail(connection, callId, "Call state changed during cancellation")) return

            closeConnectionForCancelledCall(connection, callId)
        }

        /**
         * Accepts the current ringing call.
         *
         * Delegates to [PushwooshConnection.onAnswer] which handles:
         * - Notifying the app via callback
         * - Transitioning connection to ACTIVE state
         */
        fun acceptCall() {
            PWLog.noise(TAG, "acceptCall()")
            val connection = getActiveConnection()
            if (connection == null) {
                PWLog.warn(TAG, "Cannot accept call: activeConnection is null")
                return
            }

            connection.onAnswer(1)
        }

        /**
         * Rejects the current ringing call.
         *
         * Delegates to [PushwooshConnection.onReject] which handles:
         * - Notifying the app via callback
         * - Disconnecting with REJECTED cause
         * - Destroying the connection
         */
        fun rejectCall() {
            PWLog.noise(TAG, "rejectCall()")
            val connection = getActiveConnection()
            if (connection == null) {
                PWLog.warn(TAG, "Cannot reject call: activeConnection is null")
                return
            }

            connection.onReject()
        }

        /**
         * Ends the current active call.
         *
         * Delegates to [PushwooshConnection.onDisconnect] which handles:
         * - Notifying the app via callback
         * - Disconnecting with LOCAL cause
         * - Destroying the connection
         */
        fun endCall() {
            PWLog.noise(TAG, "endCall()")
            val connection = getActiveConnection()
            if (connection == null) {
                PWLog.warn(TAG, "Cannot end call: activeConnection is null")
                return
            }

            connection.onDisconnect()
        }

        /**
         * Cleans up all references to the given connection.
         *
         * Called automatically from [PushwooshConnection.onStateChanged] when connection
         * transitions to STATE_DISCONNECTED. This ensures cleanup happens regardless of
         * how the disconnection was triggered (user action, Bluetooth, Car mode, timeout, etc.).
         *
         * This method:
         * - Clears callId mapping if it matches the given connection
         * - Clears activeConnection if it matches the given connection
         *
         * @param connection The connection to clean up
         */
        internal fun cleanupConnection(connection: PushwooshConnection) {
            PWLog.debug(TAG, "cleanupConnection() called for connection")
            val voIPMessage = PushwooshVoIPMessage(connection.extras)
            clearCallIdMappingIfEquals(voIPMessage.callId, connection)
            clearActiveConnectionIfEquals(connection)
            PWLog.debug(TAG, "Connection cleanup completed")
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

            val payload = request?.extras
            if (payload != null) {
                startCallNotificationService(Constants.PW_POST_INCOMING_CALL_ACTION, payload)
            }

            val voipMessage = PushwooshVoIPMessage(payload)
            newConnection.setExtras(payload)
            storeCallIdMapping(voipMessage.callId, newConnection)

            try {
                PushwooshCallPlugin.instance.callEventListener.onCreateIncomingConnection(payload)
            } catch (e: Exception) {
                PWLog.error(TAG, "User callback onCreateIncomingConnection() threw exception", e)
            }

            startTimeoutTimer()

            PWLog.info(TAG, "Incoming call connection created successfully")
            newConnection

        } catch (e: Exception) {
            PWLog.error(TAG, "Failed to create incoming connection", e)

            // Cleanup callId mapping if it was stored before exception
            val payload = request?.extras
            val voipMessage = PushwooshVoIPMessage(payload)
            clearCallIdMapping(voipMessage.callId)

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

        // Cleanup callId mapping if it was registered before failure
        val payload = request?.extras
        val voipMessage = PushwooshVoIPMessage(payload)
        clearCallIdMapping(voipMessage.callId)

        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
    }
}
