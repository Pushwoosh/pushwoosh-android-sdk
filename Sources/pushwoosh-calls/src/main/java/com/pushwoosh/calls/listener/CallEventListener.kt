package com.pushwoosh.calls.listener

import android.os.Bundle
import com.pushwoosh.calls.PushwooshVoIPMessage

interface CallEventListener {
    // CallReceiver callbacks
    fun onAnswer(voIPMessage: PushwooshVoIPMessage, videoState: Int)
    fun onReject(voIPMessage: PushwooshVoIPMessage)
    fun onDisconnect(voIPMessage: PushwooshVoIPMessage)

    //ConnectionService callbacks
    fun onCreateIncomingConnection(payload: Bundle?)

    // InCallService callbacks
    fun onCallAdded(voIPMessage: PushwooshVoIPMessage)
    fun onCallRemoved(voIPMessage: PushwooshVoIPMessage)

    // Call cancellation callbacks
    fun onCallCancelled(voIPMessage: PushwooshVoIPMessage)
    fun onCallCancellationFailed(callId: String?, reason: String)
}