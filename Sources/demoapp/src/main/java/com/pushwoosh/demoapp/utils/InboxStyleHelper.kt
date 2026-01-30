/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.demoapp.utils

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.pushwoosh.demoapp.R
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.model.customizing.formatter.InboxDateFormatter
import java.text.SimpleDateFormat
import java.util.*

class InboxStyleHelper {

    companion object {
        fun setupCustomInboxStyle(context: Context) {
            setupColors(context)
            setupTexts()
            setupAnimations()
            setupImages()
            setupFonts()
            setupDateFormatter()
        }

        private fun setupColors(context: Context) {
            // Accent color - primary theme color
            PushwooshInboxStyle.accentColor =
                ContextCompat.getColor(context, R.color.md_theme_primary)

            // Background colors
            PushwooshInboxStyle.backgroundColor =
                ContextCompat.getColor(context, R.color.md_theme_surface)
            PushwooshInboxStyle.highlightColor =
                ContextCompat.getColor(context, R.color.md_theme_surfaceContainerHigh)

            // Unread message colors
            PushwooshInboxStyle.titleColor =
                ContextCompat.getColor(context, R.color.md_theme_onSurface)
            PushwooshInboxStyle.descriptionColor =
                ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant)
            PushwooshInboxStyle.dateColor =
                ContextCompat.getColor(context, R.color.md_theme_outline)
            PushwooshInboxStyle.imageTypeColor =
                ContextCompat.getColor(context, R.color.md_theme_primary)

            // Read message colors - use muted variants
            PushwooshInboxStyle.readTitleColor =
                ContextCompat.getColor(context, R.color.md_theme_outline)
            PushwooshInboxStyle.readDescriptionColor =
                ContextCompat.getColor(context, R.color.md_theme_outline)
            PushwooshInboxStyle.readDateColor =
                ContextCompat.getColor(context, R.color.md_theme_outlineVariant)
            PushwooshInboxStyle.readImageTypeColor =
                ContextCompat.getColor(context, R.color.md_theme_outlineVariant)

            // Divider and bar colors
            PushwooshInboxStyle.dividerColor =
                ContextCompat.getColor(context, R.color.md_theme_outlineVariant)
            PushwooshInboxStyle.barBackgroundColor =
                ContextCompat.getColor(context, R.color.md_theme_surface)
            PushwooshInboxStyle.barAccentColor =
                ContextCompat.getColor(context, R.color.md_theme_primary)
            PushwooshInboxStyle.barTextColor =
                ContextCompat.getColor(context, R.color.md_theme_onSurface)
        }

        private fun setupTexts() {
            // Toolbar title
            PushwooshInboxStyle.barTitle = "Inbox"

            // Text sizes (in SP)
            PushwooshInboxStyle.titleTextSize = 16f
            PushwooshInboxStyle.descriptionTextSize = 14f
            PushwooshInboxStyle.dateTextSize = 12f
        }

        private fun setupAnimations() {
            PushwooshInboxStyle.listAnimationResource = android.R.anim.fade_in
        }

        private fun setupImages() {
            PushwooshInboxStyle.defaultImageIcon = R.drawable.ic_inbox_message_icon
        }

        private fun setupFonts() {
            PushwooshInboxStyle.setTitleFont(Typeface.DEFAULT_BOLD)
            PushwooshInboxStyle.setDescriptionFont(Typeface.DEFAULT)
            PushwooshInboxStyle.setDateFont(Typeface.DEFAULT)
        }

        private fun setupDateFormatter() {
            PushwooshInboxStyle.dateFormatter =
                object : InboxDateFormatter {
                    private val todayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    private val thisWeekFormat = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
                    private val olderFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

                    override fun transform(date: Date): String {
                        val now = Calendar.getInstance()
                        val messageDate = Calendar.getInstance().apply { time = date }

                        return when {
                            // Today - show only time
                            now.get(Calendar.DAY_OF_YEAR) ==
                                messageDate.get(Calendar.DAY_OF_YEAR) &&
                                now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) -> {
                                todayFormat.format(date)
                            }
                            // This week - show day and time
                            now.get(Calendar.WEEK_OF_YEAR) ==
                                messageDate.get(Calendar.WEEK_OF_YEAR) &&
                                now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) -> {
                                thisWeekFormat.format(date)
                            }
                            // Older - show month and day
                            else -> {
                                olderFormat.format(date)
                            }
                        }
                    }
                }
        }

        fun resetToDefaults() {
            PushwooshInboxStyle.clearColors()
            PushwooshInboxStyle.listEmptyText = null
            PushwooshInboxStyle.listErrorMessage = null
            PushwooshInboxStyle.barTitle = null
            PushwooshInboxStyle.titleTextSize = null
            PushwooshInboxStyle.descriptionTextSize = null
            PushwooshInboxStyle.dateTextSize = null
            PushwooshInboxStyle.listAnimationResource = android.R.anim.slide_in_left
        }
    }
}
