package com.pushwoosh.pushmart.ui.offers

import android.content.res.Resources
import com.pushwoosh.pushmart.R

object InboxCounter {
    fun subtitle(resources: Resources, unread: Int): String =
        if (unread <= 0) resources.getString(R.string.inbox_subtitle_default)
        else resources.getQuantityString(R.plurals.inbox_new_offers, unread, unread)
}
