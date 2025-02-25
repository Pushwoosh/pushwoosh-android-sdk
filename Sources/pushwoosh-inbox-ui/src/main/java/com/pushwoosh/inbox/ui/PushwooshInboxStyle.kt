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

package com.pushwoosh.inbox.ui

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.Typeface
import androidx.annotation.AnimRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import android.view.animation.Animation
import com.pushwoosh.inbox.ui.model.customizing.formatter.DefaultDateFormatter
import com.pushwoosh.inbox.ui.model.customizing.formatter.InboxDateFormatter

object PushwooshInboxStyle {
    /**
     * Use this property for clearing list animation {@link #listAnimationResource}
     */
    const val EMPTY_ANIMATION = -1

    /**
     * Inbox message date format.
     * By default used {@link com.pushwoosh.inbox.ui.model.customizing.formatter.InboxDateFormatter#DEFAULT_DATE_FORMAT} format
     */
    var dateFormatter: InboxDateFormatter = DefaultDateFormatter()

    /**
     * Item appearing animation. Set {@link #EMPTY_ANIMATION} for clear animation
     */
    @AnimRes
    var listAnimationResource: Int = android.R.anim.slide_in_left
    var listAnimation: Animation? = null

    /**
     * The icon shown near the message if there's no icon in push payload. If not specified, the app icon is used
     */
    @DrawableRes
    var defaultImageIcon: Int = -1
    var defaultImageIconDrawable: Drawable? = null

    /**
     * The image which is displayed if an error occurs and the list of inbox messages is empty
     */
    var listErrorImage : Int = R.drawable.inbox_ic_error
    var listErrorImageDrawable : Drawable? = null

    /**
     * The error text which is displayed when an error occurs
     */
    var listErrorMessage: CharSequence? = "It seems something went wrong. Please try again later!"

    /**
     * The image which is displayed if the list of inbox messages is empty
     */
    var listEmptyImage : Int = R.drawable.inbox_ic_empty


    var listEmptyImageDrawable : Drawable? = null
    /**
     * The text which is displayed if the list of inbox messages is empty
     */
    var listEmptyText: CharSequence? = "Currently there are no messages in the Inbox."

    /**
     * Accent color of inbox cell. By default used AppCompat R.attr.colorAccent
     */
    var accentColor: Int? = null

    /**
     * The color of cell background. By default used Android android.R.attr.windowBackground
     */
    var backgroundColor: Int? = null
    /**
     * The color of cell highlight. By default used AppCompat R.attr.colorControlHighlight
     */
    var highlightColor: Int? = null

    /**
     * The color of the unread message action icon (Deep Link, URL, etc.). By default used {@link #accentColor}
     */
    var imageTypeColor: Int? = null
    /**
     * The color of the read message action icon. By default used {@link #readDateColor}
     */
    var readImageTypeColor: Int? = null

    /**
     * The title color of unread messages. By default used Android android.R.attr.textColorPrimary
     */
    var titleColor: Int? = null
    /**
     * The title color of read messages. By default used Android android.R.attr.textColorSecondary
     */
    var readTitleColor: Int? = null

    /**
     * The text color of unread messages. By default used Android android.R.attr.textColorSecondary
     */
    var descriptionColor: Int? = null
    /**
     * The text color of read messages. By default used Android android.R.attr.textColorSecondary
     */
    var readDescriptionColor: Int? = null

    /**
     * The date color of unread messages. By default used Android android.R.attr.textColorSecondary
     */
    var dateColor: Int? = null

    /**
     * The date color of read messages. By default used Android android.R.attr.textColorSecondary
     */
    var readDateColor: Int? = null

    /**
     * The divider color. By default used Android android.R.attr.listDivider
     */
    var dividerColor: Int? = null

    /**
     * The bar color. By default used Android android.R.attr.textColorSecondary
     */
    var barBackgroundColor: Int? = null

    /**
     * The bar accent color. By default used Android android.R.attr.textColorSecondary
     */
    var barAccentColor: Int? = null

    /**
     * The bar text color. By default used Android android.R.attr.textColorPrimary
     */
    var barTextColor: Int? = null


    /**
     * The bar text title.
     */
    var barTitle : String? = null

    /**
     * The title text size.
     */
    var titleTextSize : Float? = null

    /**
     * The description text size.
     */
    var descriptionTextSize : Float? = null

    /**
     * The date text size.
     */
    var dateTextSize : Float? = null

    /**
     * Set a custom title font.
     */
    private var titleFont: Typeface? = null

    fun setTitleFont(typeface: Typeface) {
        titleFont = typeface
    }

    internal fun getTitleFont(): Typeface? = titleFont

    /**
     * Set a custom date font.
     */
    private var dateFont: Typeface? = null

    fun setDateFont(typeface: Typeface) {
        dateFont = typeface
    }

    internal fun getDateFont(): Typeface? = dateFont

    /**
     * Set a custom description font.
     */
    private var descriptionFont: Typeface? = null

    fun setDescriptionFont(typeface: Typeface) {
        descriptionFont = typeface
    }

    internal fun getDescriptionFont(): Typeface? = descriptionFont

    /**
     * Clear all setting colors
     */
    fun clearColors() {
        accentColor = null
        backgroundColor = null
        highlightColor = null
        imageTypeColor = null
        readImageTypeColor = null
        titleColor = null
        readTitleColor = null
        descriptionColor = null
        readDescriptionColor = null
        dateColor = null
        readDateColor = null
        dividerColor = null
    }
}