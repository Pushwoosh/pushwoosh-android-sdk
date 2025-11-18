package com.pushwoosh.calls

import android.telecom.Connection
import android.telecom.DisconnectCause
import com.pushwoosh.calls.listener.CallEventListener
import com.pushwoosh.calls.service.PushwooshConnectionService
import com.pushwoosh.internal.utils.PWLog

/**
 * Represents a VoIP call connection managed by Android Telecom Framework.
 *
 * This class handles the lifecycle of a single VoIP call and delegates business logic
 * to [CallEventListener]. Methods are called by Android Telecom Framework when user
 * interacts with the call (answer, reject, disconnect) or by [PushwooshCallReceiver].
 *
 * Lifecycle states: RINGING → ACTIVE → DISCONNECTED
 */
class PushwooshConnection(private val callEventListener: CallEventListener) : Connection() {
    companion object {
        private const val TAG = "PushwooshConnection"
    }
    override fun onDisconnect() {
        PWLog.noise(TAG, "onDisconnect()")

        try {
            val voIPMessage = PushwooshVoIPMessage(this.extras)
            callEventListener.onDisconnect(voIPMessage)
        } catch (e: Exception) {
            PWLog.error(TAG, "User callback onDisconnect() threw exception", e)
        }

        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onAnswer(videoState: Int) {
        PWLog.noise(TAG, "onAnswer(videoState=$videoState)")

        try {
            val voIPMessage = PushwooshVoIPMessage(this.extras)
            callEventListener.onAnswer(voIPMessage, videoState)
        } catch (e: Exception) {
            PWLog.error(TAG, "User callback onAnswer() threw exception", e)
        }

        setActive()
        PWLog.info(TAG, "Call transitioned to ACTIVE state")
    }

    override fun onReject() {
        PWLog.noise(TAG, "onReject()")

        try {
            val voIPMessage = PushwooshVoIPMessage(this.extras)
            callEventListener.onReject(voIPMessage)
        } catch (e: Exception) {
            PWLog.error(TAG, "User callback onReject() threw exception", e)
        }

        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    override fun onShowIncomingCallUi() {
        PWLog.noise(TAG, "onShowIncomingCallUi()")
        super.onShowIncomingCallUi()
    }

    override fun onStateChanged(state: Int) {
        PWLog.noise(TAG, "onStateChanged()")

        // Cancel timeout timer when call leaves RINGING state
        // (answered, rejected, timed out, or cancelled)
        if (state != STATE_RINGING) {
            PushwooshConnectionService.cancelTimeoutTimer()
        }

        if (state == STATE_DISCONNECTED) {
            PushwooshConnectionService.cleanupConnection(this)
        }
        super.onStateChanged(state)
    }
}
