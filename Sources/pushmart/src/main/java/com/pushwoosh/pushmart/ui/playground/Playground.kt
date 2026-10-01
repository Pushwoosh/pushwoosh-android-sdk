package com.pushwoosh.pushmart.ui.playground

import com.pushwoosh.pushmart.account.AccountOutcome
import com.pushwoosh.pushmart.sdk.PushwooshGateway
import com.pushwoosh.pushmart.sdk.TagOp

class Playground(private val gateway: PushwooshGateway) {
    fun postEvent(name: String, attrKey: String, attrValue: String): AccountOutcome {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            return AccountOutcome.Failure("Event name required", "Type an event name first.")
        }
        val key = attrKey.trim()
        val value = attrValue.trim()
        val attributes: Map<String, Any> = if (key.isEmpty()) emptyMap() else mapOf(key to value)
        gateway.postEvent(cleanName, attributes)
        val detail = if (key.isEmpty()) "" else "$key = $value"
        return AccountOutcome.Success("Posted $cleanName", detail)
    }

    fun setTag(key: String, value: String): AccountOutcome {
        val cleanKey = key.trim()
        if (cleanKey.isEmpty()) {
            return AccountOutcome.Failure("Tag key required", "Type a tag key first.")
        }
        val cleanValue = value.trim()
        gateway.setTags(listOf(TagOp.PutString(cleanKey, cleanValue)))
        return AccountOutcome.Success("Tag set", "$cleanKey = $cleanValue")
    }
}
