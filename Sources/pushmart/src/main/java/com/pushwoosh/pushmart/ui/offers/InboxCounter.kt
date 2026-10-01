package com.pushwoosh.pushmart.ui.offers

object InboxCounter {
    fun format(total: Int, unread: Int): String {
        if (total <= 0) return "No messages yet"
        val clamped = unread.coerceIn(0, total)
        return if (clamped == 0) "$total messages · all read"
        else "$total messages · $clamped unread"
    }
}
