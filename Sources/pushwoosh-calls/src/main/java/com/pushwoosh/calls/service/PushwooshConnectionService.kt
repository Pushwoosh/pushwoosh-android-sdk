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

class PushwooshConnectionService : ConnectionService() {
    companion object {
        var activeConnection: PushwooshConnection? = null
    }
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        // Reject the second call if there is an ongoing call
        if (activeConnection != null && activeConnection?.state != Connection.STATE_DISCONNECTED) {
            return Connection.createFailedConnection(
                DisconnectCause(DisconnectCause.BUSY)
            )
        }

        val connection = PushwooshConnection(PushwooshCallPlugin.instance.callEventListener)
        connection.setRinging()
        activeConnection = connection

        PushwooshCallUtils.registerCallNotificationsChannels()

        val callIntent = Intent(
            AndroidPlatformModule.getApplicationContext(),
            PushwooshCallService::class.java)
        val payload = request?.extras
        payload?.let {
            callIntent.action = Constants.PW_POST_INCOMING_CALL_ACTION
            callIntent.putExtras(payload)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                startForegroundService(callIntent)
            } else {
                startService(callIntent)
            }
        }
        PushwooshCallPlugin.instance.callEventListener.onCreateIncomingConnection(payload)
        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        return super.onCreateOutgoingConnection(connectionManagerPhoneAccount, request)
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
    }
}