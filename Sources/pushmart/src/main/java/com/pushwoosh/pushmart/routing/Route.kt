package com.pushwoosh.pushmart.routing

sealed class Route {
    data class Product(val id: String) : Route()

    object Sale : Route()

    data class Voucher(val code: String) : Route()
}
