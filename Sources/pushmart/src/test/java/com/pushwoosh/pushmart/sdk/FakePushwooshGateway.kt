package com.pushwoosh.pushmart.sdk

import java.math.BigDecimal

class FakePushwooshGateway : PushwooshGateway {
    val calls = mutableListOf<String>()
    val events = mutableListOf<Pair<String, Map<String, Any>>>()
    val tags = mutableListOf<TagOp>()
    val purchases = mutableListOf<Triple<String, BigDecimal, String>>()
    val badges = mutableListOf<Int>()

    var registerResult: GatewayResult<String?> = GatewayResult.success("fake-token")
    var identityResult: GatewayResult<Unit> = GatewayResult.success(Unit)
    var postEventResult: GatewayResult<Unit> = GatewayResult.success(Unit)
    var appCode = ""
    var fakeUserId: String? = null
    var fakePushToken: String? = null
    var fakeHwid = "fake-hwid"

    /** When true, SDK callbacks queue into [pending] instead of firing inline; call [flush]. */
    var deferCallbacks = false
    private val pending = mutableListOf<() -> Unit>()

    override fun setAppId(code: String) {
        appCode = code
        calls.add("setAppId:$code")
    }

    override fun getApplicationCode(): String = appCode

    override fun registerForPushNotifications(callback: (GatewayResult<String?>) -> Unit) {
        calls.add("register")
        val deliver = {
            if (registerResult.isSuccess) fakePushToken = registerResult.data
            callback(registerResult)
        }
        if (deferCallbacks) pending.add(deliver) else deliver()
    }

    override fun unregisterForPushNotifications() {
        calls.add("unregister")
        fakePushToken = null
    }

    override fun setEmail(email: String, callback: (GatewayResult<Unit>) -> Unit) {
        calls.add("setEmail:$email")
        val deliver = { callback(identityResult) }
        if (deferCallbacks) pending.add(deliver) else deliver()
    }

    override fun setUserId(userId: String, callback: (GatewayResult<Unit>) -> Unit) {
        calls.add("setUserId:$userId")
        val deliver = {
            if (identityResult.isSuccess) fakeUserId = userId
            callback(identityResult)
        }
        if (deferCallbacks) pending.add(deliver) else deliver()
    }

    /** Runs queued callbacks in FIFO order, including ones enqueued while flushing. */
    fun flush() {
        while (pending.isNotEmpty()) {
            pending.removeAt(0)()
        }
    }

    override fun getUserId(): String? = fakeUserId

    override fun getPushToken(): String? = fakePushToken

    override fun getHwid(): String = fakeHwid

    override fun setTags(ops: List<TagOp>) {
        calls.add("setTags")
        tags.addAll(ops)
    }

    override fun postEvent(name: String, attributes: Map<String, Any>) {
        calls.add("postEvent:$name")
        events.add(name to attributes)
    }

    override fun postEvent(
        name: String,
        attributes: Map<String, Any>,
        callback: (GatewayResult<Unit>) -> Unit
    ) {
        postEvent(name, attributes)
        val deliver = { callback(postEventResult) }
        if (deferCallbacks) pending.add(deliver) else deliver()
    }

    override fun sendInappPurchase(sku: String, price: BigDecimal, currency: String) {
        calls.add("purchase:$sku")
        purchases.add(Triple(sku, price, currency))
    }

    override fun setBadgeNumber(count: Int) {
        calls.add("badge:$count")
        badges.add(count)
    }
}
