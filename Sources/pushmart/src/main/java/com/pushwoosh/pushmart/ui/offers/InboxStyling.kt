package com.pushwoosh.pushmart.ui.offers

import android.content.Context
import androidx.core.content.ContextCompat
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.model.customizing.formatter.DefaultDateFormatter
import com.pushwoosh.pushmart.R
import java.text.DateFormat

/**
 * Maps the PushMart theme onto the global, mutable inbox-ui style, as iOS `applyAppearance` does
 * for InboxKit. Runs before the inbox fragments attach, so restored children see the current theme.
 */
object InboxStyling {
    fun apply(context: Context) {
        fun color(id: Int): Int = ContextCompat.getColor(context, id)
        PushwooshInboxStyle.accentColor = color(R.color.pm_coral)
        PushwooshInboxStyle.backgroundColor = color(R.color.pm_ink)
        PushwooshInboxStyle.highlightColor = color(R.color.pm_coral_14)
        PushwooshInboxStyle.dividerColor = color(R.color.pm_stroke)
        PushwooshInboxStyle.titleColor = color(R.color.pm_text_primary)
        PushwooshInboxStyle.readTitleColor = color(R.color.pm_text_primary)
        PushwooshInboxStyle.descriptionColor = color(R.color.pm_text_secondary)
        PushwooshInboxStyle.readDescriptionColor = color(R.color.pm_text_secondary)
        PushwooshInboxStyle.dateColor = color(R.color.pm_text_tertiary)
        PushwooshInboxStyle.readDateColor = color(R.color.pm_text_tertiary)
        PushwooshInboxStyle.imageTypeColor = color(R.color.pm_coral)
        PushwooshInboxStyle.readImageTypeColor = color(R.color.pm_text_tertiary)
        PushwooshInboxStyle.showToolbar = false
        PushwooshInboxStyle.richCardsHeuristicEnabled = true
        PushwooshInboxStyle.automaticReadOnDisplay = true
        PushwooshInboxStyle.listEmptyText = context.getString(R.string.inbox_empty_title)
        PushwooshInboxStyle.listEmptyImage = R.drawable.ic_gift
        PushwooshInboxStyle.listErrorMessage = context.getString(R.string.inbox_error)
        PushwooshInboxStyle.listErrorImage = R.drawable.ic_warning_outline
        PushwooshInboxStyle.dateFormatter =
            DefaultDateFormatter(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT))
    }
}
