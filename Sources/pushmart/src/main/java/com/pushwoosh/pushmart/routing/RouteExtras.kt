package com.pushwoosh.pushmart.routing

import android.content.Intent

object RouteExtras {
    private const val EXTRA_TYPE = "pushmart.route.type"
    private const val EXTRA_VALUE = "pushmart.route.value"

    fun put(intent: Intent, route: Route): Intent {
        when (route) {
            is Route.Product ->
                intent.putExtra(EXTRA_TYPE, "product").putExtra(EXTRA_VALUE, route.id)
            Route.Sale -> intent.putExtra(EXTRA_TYPE, "sale")
            is Route.Voucher ->
                intent.putExtra(EXTRA_TYPE, "voucher").putExtra(EXTRA_VALUE, route.code)
        }
        return intent
    }

    fun get(intent: Intent?): Route? {
        val type = intent?.getStringExtra(EXTRA_TYPE) ?: return null
        val value = intent.getStringExtra(EXTRA_VALUE).orEmpty()
        return when (type) {
            "product" -> Route.Product(value)
            "sale" -> Route.Sale
            "voucher" -> Route.Voucher(value)
            else -> null
        }
    }

    fun clear(intent: Intent?) {
        intent?.removeExtra(EXTRA_TYPE)
        intent?.removeExtra(EXTRA_VALUE)
    }
}
