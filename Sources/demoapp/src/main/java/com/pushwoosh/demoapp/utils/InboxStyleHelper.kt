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
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
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
            setupImages(context)
            setupFonts(context)
            setupDateFormatter()
        }

        private fun setupColors(context: Context) {
            val isDarkTheme = (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            if (isDarkTheme) {
                setupDarkTheme(context)
            } else {
                setupLightTheme(context)
            }
        }

        private fun setupLightTheme(context: Context) {
            // Light theme colors
            PushwooshInboxStyle.accentColor = Color.parseColor("#FF6B35") // Vibrant orange

            // Light background colors
            PushwooshInboxStyle.backgroundColor = Color.parseColor("#FFFBFE") // White
            PushwooshInboxStyle.highlightColor = Color.parseColor("#F5F5F5") // Light gray highlight

            // Unread message colors
            PushwooshInboxStyle.titleColor = Color.parseColor("#000000") // Black
            PushwooshInboxStyle.descriptionColor = Color.parseColor("#000000") // Black
            PushwooshInboxStyle.dateColor = Color.parseColor("#FFC107") // Bright amber
            PushwooshInboxStyle.imageTypeColor = Color.parseColor("#4CAF50") // Bright green

            // Read message colors
            PushwooshInboxStyle.readTitleColor = Color.parseColor("#AAAAAA") // Medium gray
            PushwooshInboxStyle.readDescriptionColor = Color.parseColor("#888888") // Darker gray
            PushwooshInboxStyle.readDateColor = Color.parseColor("#666666") // Even darker gray
            PushwooshInboxStyle.readImageTypeColor = Color.parseColor("#555555") // Very dark gray

            // Divider and bar colors
            PushwooshInboxStyle.dividerColor = Color.parseColor("#E0E0E0") // Light divider
            PushwooshInboxStyle.barBackgroundColor = Color.parseColor("#FFFFFF") // White toolbar
            PushwooshInboxStyle.barAccentColor = Color.parseColor("#FF6B35") // Orange accent
            PushwooshInboxStyle.barTextColor = Color.parseColor("#000000") // Black text
        }

        private fun setupTexts() {
            // Custom empty and error messages
            PushwooshInboxStyle.listEmptyText = "No messages yet! Check back later for updates."
            PushwooshInboxStyle.listErrorMessage = "Oops! Something went wrong while loading your messages. Please try again."
            
            // Toolbar title
            PushwooshInboxStyle.barTitle = "My Messages"
            
            // Text sizes (in SP)
            PushwooshInboxStyle.titleTextSize = 16f
            PushwooshInboxStyle.descriptionTextSize = 14f
            PushwooshInboxStyle.dateTextSize = 12f
        }

        private fun setupAnimations() {
            // Set custom list item animation
            PushwooshInboxStyle.listAnimationResource = android.R.anim.fade_in
            // Or disable animation completely:
            // PushwooshInboxStyle.listAnimationResource = PushwooshInboxStyle.EMPTY_ANIMATION
        }

        private fun setupImages(context: Context) {
            // Set default icon for messages without images
            PushwooshInboxStyle.defaultImageIcon = R.drawable.ic_launcher_background
            
            // Custom empty state image
            // PushwooshInboxStyle.listEmptyImage = R.drawable.custom_empty_icon
            
            // Custom error state image  
            // PushwooshInboxStyle.listErrorImage = R.drawable.custom_error_icon
        }

        private fun setupFonts(context: Context) {
            // Set custom fonts if available
            try {
                // Example with custom font from assets
                // val customFont = Typeface.createFromAsset(context.assets, "fonts/custom_font.ttf")
                // PushwooshInboxStyle.setTitleFont(customFont)
                
                // Use system fonts
                PushwooshInboxStyle.setTitleFont(Typeface.DEFAULT_BOLD)
                PushwooshInboxStyle.setDescriptionFont(Typeface.DEFAULT)
                PushwooshInboxStyle.setDateFont(Typeface.DEFAULT)
            } catch (e: Exception) {
                // Fallback to default fonts if custom fonts fail to load
            }
        }

        private fun setupDateFormatter() {
            // Custom date formatter
            PushwooshInboxStyle.dateFormatter = object : InboxDateFormatter {
                private val todayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                private val thisWeekFormat = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
                private val olderFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                
                override fun transform(date: Date): String {
                    val now = Calendar.getInstance()
                    val messageDate = Calendar.getInstance().apply { time = date }
                    
                    return when {
                        // Today - show only time
                        now.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR) && 
                        now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) -> {
                            todayFormat.format(date)
                        }
                        // This week - show day and time
                        now.get(Calendar.WEEK_OF_YEAR) == messageDate.get(Calendar.WEEK_OF_YEAR) &&
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

        fun setupDarkTheme(context: Context) {
            // Dark theme color scheme
            PushwooshInboxStyle.backgroundColor = Color.parseColor("#1C1B1F")
            PushwooshInboxStyle.highlightColor = Color.parseColor("#2C2C2C")
            
            // Unread message colors for dark theme
            PushwooshInboxStyle.titleColor = Color.parseColor("#FFFFFF")
            PushwooshInboxStyle.descriptionColor = Color.parseColor("#CCCCCC")
            PushwooshInboxStyle.dateColor = Color.parseColor("#999999")
            
            // Read message colors for dark theme
            PushwooshInboxStyle.readTitleColor = Color.parseColor("#888888")
            PushwooshInboxStyle.readDescriptionColor = Color.parseColor("#666666")
            PushwooshInboxStyle.readDateColor = Color.parseColor("#555555")
            
            PushwooshInboxStyle.dividerColor = Color.parseColor("#2C2C2C")
            PushwooshInboxStyle.barTextColor = Color.WHITE
        }

        fun resetToDefaults() {
            // Clear all customizations to return to default styling
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