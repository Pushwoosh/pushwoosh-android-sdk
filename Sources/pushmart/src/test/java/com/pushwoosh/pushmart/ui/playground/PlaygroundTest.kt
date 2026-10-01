package com.pushwoosh.pushmart.ui.playground

import com.pushwoosh.pushmart.account.AccountOutcome
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
import com.pushwoosh.pushmart.sdk.TagOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaygroundTest {
    private val gateway = FakePushwooshGateway()
    private val playground = Playground(gateway)

    @Test
    fun postEventWithoutAttributesSendsEmptyMap() {
        val outcome = playground.postEvent("claudeCodeCheck", "", "")
        assertEquals(listOf("claudeCodeCheck" to emptyMap<String, Any>()), gateway.events)
        assertEquals(AccountOutcome.Success("Posted claudeCodeCheck", ""), outcome)
    }

    @Test
    fun postEventWithAttributeSendsStringAttribute() {
        val outcome = playground.postEvent("purchase", "sku", "chrono")
        assertEquals(listOf("purchase" to mapOf<String, Any>("sku" to "chrono")), gateway.events)
        assertEquals(AccountOutcome.Success("Posted purchase", "sku = chrono"), outcome)
    }

    @Test
    fun postEventTrimsEveryField() {
        playground.postEvent("  purchase ", " sku ", " chrono ")
        assertEquals(listOf("purchase" to mapOf<String, Any>("sku" to "chrono")), gateway.events)
    }

    @Test
    fun postEventIgnoresValueWhenKeyIsBlank() {
        playground.postEvent("purchase", "  ", "chrono")
        assertEquals(listOf("purchase" to emptyMap<String, Any>()), gateway.events)
    }

    @Test
    fun postEventWithBlankNameFailsWithoutSdkCall() {
        val outcome = playground.postEvent("  ", "sku", "chrono")
        assertTrue(gateway.calls.isEmpty())
        assertEquals(
            AccountOutcome.Failure("Event name required", "Type an event name first."), outcome)
    }

    @Test
    fun setTagSendsPutString() {
        val outcome = playground.setTag(" plan ", " gold ")
        assertEquals(listOf(TagOp.PutString("plan", "gold")), gateway.tags)
        assertEquals(AccountOutcome.Success("Tag set", "plan = gold"), outcome)
    }

    @Test
    fun setTagWithBlankKeyFailsWithoutSdkCall() {
        val outcome = playground.setTag("", "gold")
        assertTrue(gateway.calls.isEmpty())
        assertEquals(AccountOutcome.Failure("Tag key required", "Type a tag key first."), outcome)
    }
}
