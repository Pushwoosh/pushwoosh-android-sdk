package com.pushwoosh.calls.service

import android.telecom.Call
import android.telecom.InCallService
import com.pushwoosh.calls.PushwooshCallPlugin
import com.pushwoosh.calls.PushwooshVoIPMessage


class PushwooshInCallService : InCallService() {
    override fun onCallAdded(call: Call?) {
        val voIPMessage = PushwooshVoIPMessage(call?.details?.extras)
        PushwooshCallPlugin.instance.callEventListener.onCallAdded(voIPMessage)
        super.onCallAdded(call)
    }

    override fun onCallRemoved(call: Call?) {
        val voIPMessage = PushwooshVoIPMessage(call?.details?.extras)
        PushwooshCallPlugin.instance.callEventListener.onCallRemoved(voIPMessage)
        super.onCallRemoved(call)
    }
}