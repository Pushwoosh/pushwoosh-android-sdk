package com.pushwoosh.calls

import android.telecom.Connection
import android.telecom.DisconnectCause
import com.pushwoosh.calls.listener.CallEventListener

class PushwooshConnection(private val callEventListener: CallEventListener) : Connection() {
    override fun onDisconnect() {
        val voIPMessage = PushwooshVoIPMessage(this.extras)
        callEventListener.onDisconnect(voIPMessage)
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onAnswer(videoState: Int) {
        val voIPMessage = PushwooshVoIPMessage(this.extras)
        callEventListener.onAnswer(voIPMessage, videoState)
        super.onAnswer(videoState)
    }

    override fun onReject() {
        val voIPMessage = PushwooshVoIPMessage(this.extras)
        callEventListener.onReject(voIPMessage)
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    override fun onShowIncomingCallUi() {
        super.onShowIncomingCallUi()
    }
}