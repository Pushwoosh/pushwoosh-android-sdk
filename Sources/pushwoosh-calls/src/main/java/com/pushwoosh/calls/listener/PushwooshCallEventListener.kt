package com.pushwoosh.calls.listener

import android.os.Bundle
import com.pushwoosh.calls.PushwooshVoIPMessage
import com.pushwoosh.internal.utils.PWLog

class PushwooshCallEventListener : CallEventListener {
    override fun onAnswer(voIPMessage: PushwooshVoIPMessage, videoState: Int) {
        PWLog.info("onAnswer")
    }

    override fun onReject(voIPMessage: PushwooshVoIPMessage) {
        PWLog.info("onReject")
    }

    override fun onDisconnect(voIPMessage: PushwooshVoIPMessage) {
        PWLog.info("onDisconnect")
    }

    override fun onCallAdded(voIPMessage: PushwooshVoIPMessage) {
        PWLog.info("onCallAdded")

    }

    override fun onCallRemoved(voIPMessage: PushwooshVoIPMessage) {
        PWLog.info("onCallRemoved")
    }

    override fun onCreateIncomingConnection(payload: Bundle?) {
        PWLog.info("onCreateIncomingConnection")
    }
}