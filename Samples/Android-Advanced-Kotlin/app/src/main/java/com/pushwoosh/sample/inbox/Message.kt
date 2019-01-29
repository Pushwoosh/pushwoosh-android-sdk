package com.pushwoosh.sample.inbox

import java.io.Serializable

class Message(val text: CharSequence, val ts: Long, val sender: String) : Serializable {
    companion object {
        internal const val serialVersionUID = 503643406077571017L
    }
}
