package com.pushwoosh.calls

import android.os.Build
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import com.pushwoosh.calls.listener.CallAudioStateListener
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

    private var routeBeforeSpeaker: Int? = null
    private var reportedAudioState: CallAudioState? = null

    /**
     * Asks Telecom to route the call audio to the speaker, or back from it.
     *
     * Turning the speaker off restores the route that was active before the speaker was turned on,
     * otherwise the first available of earpiece, wired headset and Bluetooth.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("DEPRECATION")
    internal fun requestSpeaker(on: Boolean) {
        val state = callAudioState
        val currentRoute = state?.route
        val route = if (on) {
            if (currentRoute != null && currentRoute != CallAudioState.ROUTE_SPEAKER) {
                routeBeforeSpeaker = currentRoute
            }
            CallAudioState.ROUTE_SPEAKER
        } else {
            val supportedRoutes = state?.supportedRouteMask ?: 0
            listOfNotNull(
                currentRoute,
                routeBeforeSpeaker,
                CallAudioState.ROUTE_EARPIECE,
                CallAudioState.ROUTE_WIRED_HEADSET,
                CallAudioState.ROUTE_BLUETOOTH
            ).firstOrNull { it != CallAudioState.ROUTE_SPEAKER && supportedRoutes and it != 0 }
                ?: CallAudioState.ROUTE_EARPIECE
        }
        PWLog.info(TAG, "Audio route requested: ${CallAudioState.audioRouteToString(route)}")
        setAudioRoute(route)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onCallAudioStateChanged(state: CallAudioState?) {
        val previous = reportedAudioState
        reportedAudioState = state
        if (state == null) return
        PWLog.info(
            TAG,
            "Call audio state: muted=${state.isMuted}, route=${CallAudioState.audioRouteToString(state.route)}, " +
                "supported=${CallAudioState.audioRouteToString(state.supportedRouteMask)}"
        )

        val listener = callEventListener as? CallAudioStateListener ?: return
        try {
            if (previous == null || previous.isMuted != state.isMuted) {
                listener.onMuteChanged(state.isMuted)
            }
            if (previous == null || previous.route != state.route) {
                listener.onAudioRouteChanged(state.route)
            }
        } catch (e: Exception) {
            PWLog.error(TAG, "User callback for call audio state threw exception", e)
        }
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
