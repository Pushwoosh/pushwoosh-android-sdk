package com.pushwoosh.pushmart.sdk

import java.math.BigDecimal

class FakePushwooshGateway : PushwooshGateway {
    val calls = mutableListOf<String>()
    val events = mutableListOf<Pair<String, Map<String, Any>>>()
    val tags = mutableListOf<TagOp>()
    val purchases = mutableListOf<Triple<String, BigDecimal, String>>()
    val badges = mutableListOf<Int>()

    var registerResult: GatewayResult<String?> = GatewayResult.success("fake-token")
    var unregisterResult: GatewayResult<Unit> = GatewayResult.success(Unit)
    var identityResult: GatewayResult<Unit> = GatewayResult.success(Unit)
    var postEventResult: GatewayResult<Unit> = GatewayResult.success(Unit)
    var setTagsResult: GatewayResult<Unit> = GatewayResult.success(Unit)
    var tagsResult: GatewayResult<Map<String, Any?>> = GatewayResult.success(emptyMap())
    var appCode = ""

    /** When true, SDK callbacks queue into [pending] instead of firing inline; call [flush]. */
    var deferCallbacks = false
    private val pending = mutableListOf<() -> Unit>()

    override fun setAppId(code: String) {
        appCode = code
        calls.add("setAppId:$code")
    }

    override fun getApplicationCode(): String = appCode

    override fun resetUserId() {
        calls.add("resetUserId")
    }

    override fun registerForPushNotifications(callback: (GatewayResult<String?>) -> Unit) {
        calls.add("register")
        val deliver = { callback(registerResult) }
        if (deferCallbacks) pending.add(deliver) else deliver()
    }

    override fun unregisterForPushNotifications(callback: ((GatewayResult<Unit>) -> Unit)?) {
        calls.add("unregister")
        if (callback == null) return
        val deliver = { callback(unregisterResult) }
        if (deferCallbacks) pending.add(deliver) else deliver()
    }

    override fun setEmail(email: String, callback: (GatewayResult<Unit>) -> Unit) {
        calls.add("setEmail:$email")
        val deliver = { callback(identityResult) }
        if (deferCallbacks) pending.add(deliver) else deliver()
    }

    /** Runs queued callbacks in FIFO order, including ones enqueued while flushing. */
    fun flush() {
        while (pending.isNotEmpty()) {
            pending.removeAt(0)()
        }
    }

    override fun registerSmsNumber(number: String) {
        calls.add("registerSms:$number")
    }

    override fun registerWhatsappNumber(number: String) {
        calls.add("registerWhatsapp:$number")
    }

    override fun setTags(ops: List<TagOp>) {
        calls.add("setTags")
        tags.addAll(ops)
    }

    override fun setTags(ops: List<TagOp>, callback: (GatewayResult<Unit>) -> Unit) {
        setTags(ops)
        respond { callback(setTagsResult) }
    }

    /** Records the ops in [tags] too; [setTagsResult] drives the result. */
    override fun setEmailTags(
        email: String,
        ops: List<TagOp>,
        callback: (GatewayResult<Unit>) -> Unit
    ) {
        calls.add("setEmailTags:$email")
        tags.addAll(ops)
        respond { callback(setTagsResult) }
    }

    override fun getTags(callback: (GatewayResult<Map<String, Any?>>) -> Unit) {
        calls.add("getTags")
        respond { callback(tagsResult) }
    }

    private fun respond(deliver: () -> Unit) {
        if (deferCallbacks) pending.add(deliver) else deliver()
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

    override fun scheduleLocalNotification(title: String, body: String, delaySeconds: Int) {
        calls.add("scheduleLocalNotification:$title:$delaySeconds")
    }

    override fun setShowForegroundAlerts(show: Boolean) {
        calls.add("foregroundAlerts:$show")
    }
}
