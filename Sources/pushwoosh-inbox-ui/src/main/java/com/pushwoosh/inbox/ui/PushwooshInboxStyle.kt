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
     * When true, messages without an explicit `displayType` pick a rich card by the
     * iOS-parity heuristic: image + title renders a captioned card, image alone renders
     * a banner card. Off by default so existing inboxes keep their legacy row rendering;
     * messages with an explicit `displayType` render as rich cards regardless of this flag.
     */
    var richCardsHeuristicEnabled: Boolean = false

    /**
     * When true, messages are marked read once they are on screen: after the list loads and when a scroll settles.
     */
    var automaticReadOnDisplay: Boolean = true

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
     * Accent of the inbox: the action icon of an unread message, the unread dot and the buttons of
     * rich cards, the avatar background of an unread classic row, the pinned-message icon of a
     * classic row, the swipe-to-delete background, the pull-to-refresh spinner and the first-load
     * indicator. Defaults to the theme attribute inboxAccentColor, which resolves to the theme's
     * colorAccent.
     */
    var accentColor: Int? = null

    /**
     * Background of the inbox screen: the list, the message rows, the empty state and the error
     * state, and below API 35 the navigation bar. Does not paint the toolbar. Defaults to the theme
     * attribute inboxBackgroundColor, which PwInboxTheme sets to white; only a host theme that
     * leaves that attribute undefined falls through to android.R.attr.windowBackground.
     */
    var backgroundColor: Int? = null
    /**
     * The color of cell highlight. By default used AppCompat R.attr.colorControlHighlight
     */
    var highlightColor: Int? = null

    /**
     * The action icon (Deep Link, URL, etc.) of an unread message. Defaults to [accentColor], and
     * with neither key set to the theme attribute inboxImageTypeColor, which resolves to the
     * theme's colorAccent.
     */
    var imageTypeColor: Int? = null
    /**
     * The action icon of a read message. Defaults to [readDateColor], and with neither key set to
     * the theme attribute inboxReadImageTypeColor, which resolves to the theme's
     * android.R.attr.textColorSecondary.
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
     * The date of an unread message. Defaults to the theme attribute inboxDateColor, which resolves
     * to the theme's android.R.attr.textColorSecondary.
     */
    var dateColor: Int? = null

    /**
     * The date of a read message. Defaults to the theme attribute inboxReadDateColor, which
     * resolves to the theme's android.R.attr.textColorSecondary.
     */
    var readDateColor: Int? = null

    /**
     * The divider color. By default used Android android.R.attr.listDivider
     */
    var dividerColor: Int? = null

    /**
     * Background of the toolbar and the app bar, and below API 35 of the status bar. Independent of
     * [backgroundColor]. Defaults to android.R.attr.colorPrimary — the platform attribute the
     * toolbar layout points at, not AppCompat's colorPrimary.
     */
    var barBackgroundColor: Int? = null

    /**
     * The toolbar navigation icon — the back arrow. Nothing else. With the key unset we apply no
     * tint of our own, so the icon keeps the one AppCompat gives it from the activity theme's
     * AppCompat colorControlNormal.
     */
    var barAccentColor: Int? = null

    /**
     * The toolbar title. Nothing else. Defaults to white.
     */
    var barTextColor: Int? = null


    /**
     * The bar text title.
     */
    var barTitle : String? = null

    /**
     * Controls whether the InboxFragment displays its own toolbar.
     * - null (default): Auto-detect based on host Activity's ActionBar
     * - true: Always show toolbar (use for InboxActivity or Activities without toolbar)
     * - false: Always hide toolbar (use when embedding in Activity with existing toolbar)
     */
    var showToolbar: Boolean? = null


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
     * Resets every color key of this object back to its default, the three toolbar keys included,
     * and puts [showToolbar] back to auto-detect. [barTitle] is not a color and is left alone.
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
        barBackgroundColor = null
        barAccentColor = null
        barTextColor = null
        showToolbar = null
    }
}