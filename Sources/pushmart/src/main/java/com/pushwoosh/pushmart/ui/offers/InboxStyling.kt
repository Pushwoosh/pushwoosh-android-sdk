package com.pushwoosh.pushmart.ui.offers

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.pushmart.R

/**
 * Maps PushMart theme colors onto the global, mutable inbox-ui style; call on every screen create.
 */
object InboxStyling {
    fun apply(context: Context) {
        fun color(id: Int): Int = ContextCompat.getColor(context, id)
        PushwooshInboxStyle.accentColor = color(R.color.pm_coral)
        PushwooshInboxStyle.backgroundColor = Color.TRANSPARENT
        PushwooshInboxStyle.highlightColor = color(R.color.pm_coral_14)
        PushwooshInboxStyle.titleColor = color(R.color.pm_text_primary)
        PushwooshInboxStyle.descriptionColor = color(R.color.pm_text_secondary)
        PushwooshInboxStyle.dateColor = color(R.color.pm_coral)
        PushwooshInboxStyle.imageTypeColor = color(R.color.pm_on_brand)
        PushwooshInboxStyle.readTitleColor = color(R.color.pm_text_secondary)
        PushwooshInboxStyle.readDescriptionColor = color(R.color.pm_text_tertiary)
        PushwooshInboxStyle.readDateColor = color(R.color.pm_text_tertiary)
        PushwooshInboxStyle.readImageTypeColor = color(R.color.pm_text_tertiary)
        PushwooshInboxStyle.showToolbar = false
        PushwooshInboxStyle.listEmptyText = "You're all caught up.\nNew messages will appear here."
        PushwooshInboxStyle.listErrorMessage = "Couldn't load your offers.\nPull down to try again."
        PushwooshInboxStyle.setTitleFont(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD))
    }
}
