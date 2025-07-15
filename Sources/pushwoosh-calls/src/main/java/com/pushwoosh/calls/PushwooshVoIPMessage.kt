package com.pushwoosh.calls

import android.os.Bundle

class PushwooshVoIPMessage(payload: Bundle?) {
    val callerName: String? = payload?.getString("callerName", "Unknown Caller")
    val hasVideo: Boolean? = payload?.getBoolean("video", false)
    val supportsHolding: Boolean? = payload?.getBoolean("supportsHolding", false)
    val supportsDMTF: Boolean? = payload?.getBoolean("supportsDMTF", false)
    val rawPayload: Bundle? = payload
}