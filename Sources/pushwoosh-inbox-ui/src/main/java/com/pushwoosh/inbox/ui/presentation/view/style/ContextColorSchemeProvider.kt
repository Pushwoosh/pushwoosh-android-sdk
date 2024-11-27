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

package com.pushwoosh.inbox.ui.presentation.view.style

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.R

class ContextColorSchemeProvider(private val context: Context) : ColorSchemeProvider {
    private val _defaultIcon: Drawable?
    override val defaultIcon: Drawable?
        get() = _defaultIcon

    private val _cellBackground: Drawable
    override val cellBackground: Drawable
        get() = provideCellBackground()

    private val _titleColor: ColorStateList
    override val titleColor: ColorStateList
        get() = _titleColor

    private val _descriptionColor: ColorStateList
    override val descriptionColor: ColorStateList
        get() = _descriptionColor

    private val _dateColor: ColorStateList
    override val dateColor: ColorStateList
        get() = _dateColor

    private val _divider: Drawable?
    override val divider: Drawable?
        get() = _divider


    private val _accentColor: Int
    override val accentColor: Int
        get() = _accentColor

    private val _imageColor: ColorStateList
    override val imageColor: ColorStateList
        get() = _imageColor

    private val _backgroundColor: Int
    override val backgroundColor: Int
        get() = _backgroundColor

    private val colorResources: Map<Int, Int>
    private val drawableResources: Map<Int, Drawable?>

    private val states: Array<IntArray> = arrayOf(
        intArrayOf(android.R.attr.state_selected),
        intArrayOf()
    )

    @ColorRes
    private var highlightColor: Int

    @ColorRes
    private var background: Int

    init {
        colorResources = provideColorMap()
        drawableResources = provideDrawableMap()

        highlightColor = PushwooshInboxStyle.highlightColor ?: provideColorByAttr(R.attr.inboxHighlightColor)
        background = PushwooshInboxStyle.backgroundColor ?: provideColorByAttr(R.attr.inboxBackgroundColor)


        _accentColor = PushwooshInboxStyle.accentColor ?: provideColorByAttr(R.attr.inboxAccentColor)

        val inboxDividerColor = PushwooshInboxStyle.dividerColor
        _divider = if (inboxDividerColor != null) {
            val shapeDrawable = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(inboxDividerColor, inboxDividerColor))
            shapeDrawable.setSize(context.resources.displayMetrics.widthPixels, context.resources.getDimensionPixelSize(R.dimen.pw_divider_size))
            shapeDrawable
        } else {
            drawableResources[android.R.attr.listDivider]
        }

        _imageColor = generateStateList(arrayOf(
            Pair(PushwooshInboxStyle.imageTypeColor, provideColorByAttr(R.attr.inboxImageTypeColor)),
            Pair(PushwooshInboxStyle.readImageTypeColor, provideColorByAttr(R.attr.inboxReadImageTypeColor))
        ))

        _titleColor = generateStateList(arrayOf(
            Pair(PushwooshInboxStyle.titleColor, provideColorByAttr(R.attr.inboxTitleColor)),
            Pair(PushwooshInboxStyle.readTitleColor, provideColorByAttr(R.attr.inboxReadTitleColor))
        ))

        _descriptionColor = generateStateList(arrayOf(
            Pair(PushwooshInboxStyle.descriptionColor, provideColorByAttr(R.attr.inboxDescriptionColor)),
            Pair(PushwooshInboxStyle.readDescriptionColor, provideColorByAttr(R.attr.inboxReadDescriptionColor))
        ))

        _dateColor = generateStateList(arrayOf(
            Pair(PushwooshInboxStyle.dateColor, provideColorByAttr(R.attr.inboxDateColor)),
            Pair(PushwooshInboxStyle.readDateColor, provideColorByAttr(R.attr.inboxReadDateColor))
        ))

        _cellBackground = provideCellBackground()

        _backgroundColor = background

        val styleDefaultIcon = drawableResources[R.attr.inboxDefaultIcon]
        val loadIcon = context.applicationInfo.loadIcon(context.packageManager)
        _defaultIcon = when {
            PushwooshInboxStyle.defaultImageIconDrawable != null -> PushwooshInboxStyle.defaultImageIconDrawable
            PushwooshInboxStyle.defaultImageIcon != -1 -> ContextCompat.getDrawable(context, PushwooshInboxStyle.defaultImageIcon) ?: loadIcon
            styleDefaultIcon != null -> styleDefaultIcon
            else -> loadIcon
        }
    }

    private fun provideColorByAttr(@AttrRes attr: Int): Int {
        val mapResult = colorResources[attr]
        return if (mapResult == null || mapResult == 0) {
            val defaultColor = provideDefaultColor(attr)
            if (defaultColor == 0) {
                throw throw getResourceException(attr)
            } else {
                defaultColor
            }
        } else {
            mapResult
        }
    }

    private fun provideColorMap(): Map<Int, Int> {
        val colorArray = intArrayOf(R.attr.inboxAccentColor,
            R.attr.inboxBackgroundColor,
            R.attr.inboxHighlightColor,
            R.attr.inboxImageTypeColor,
            R.attr.inboxReadImageTypeColor,
            R.attr.inboxTitleColor,
            R.attr.inboxReadTitleColor,
            R.attr.inboxDescriptionColor,
            R.attr.inboxReadDescriptionColor,
            R.attr.inboxDateColor,
            R.attr.inboxReadDateColor,
            androidx.appcompat.R.attr.colorAccent,
            androidx.appcompat.R.attr.colorControlHighlight,
            android.R.attr.windowBackground,
            android.R.attr.textColorPrimary,
            android.R.attr.textColorSecondary)

        val result = mutableMapOf<Int, Int>()
        provideFromResource(colorArray, { index, attr, typedArray ->
            result[attr] = typedArray.getColor(index, 0)
        })

        return result
    }

    private fun provideDrawableMap(): Map<Int, Drawable> {
        val drawableArray = intArrayOf(
            android.R.attr.listDivider,
            R.attr.inboxDefaultIcon)

        val result = mutableMapOf<Int, Drawable>()
        provideFromResource(drawableArray, { index, attr, typedArray ->
            val drawable = typedArray.getDrawable(index)
            if (drawable != null) {
                result[attr] = drawable
            }
        })

        return result
    }

    private fun provideFromResource(attrs: IntArray, callback: (Int, Int, TypedArray) -> Unit) {
        val theme = context.theme
        val typedArray = theme.obtainStyledAttributes(attrs)
        try {
            attrs.forEachIndexed { index, attr ->
                callback(index, attr, typedArray)
            }
        } finally {
            typedArray.recycle()
        }
    }

    private fun provideCellBackground(): Drawable {
        return getAdaptiveRippleDrawable(background, highlightColor)
    }

    private fun generateStateList(colorsState: Array<Pair<Int?, Int>>): ColorStateList {
        val colors: IntArray = colorsState
            .map { it.first ?: it.second }
            .toIntArray()

        return ColorStateList(states, colors)
    }

    private fun provideDefaultColor(@AttrRes attr: Int): Int {
        val defaultColor = when (attr) {
            R.attr.inboxAccentColor -> provideColorByAttr(androidx.appcompat.R.attr.colorAccent)
            R.attr.inboxBackgroundColor -> provideColorByAttr(android.R.attr.windowBackground)
            R.attr.inboxHighlightColor -> provideColorByAttr(androidx.appcompat.R.attr.colorControlHighlight)
            R.attr.inboxImageTypeColor -> provideColorByAttr(R.attr.inboxAccentColor)
            R.attr.inboxReadImageTypeColor -> provideColorByAttr(R.attr.inboxReadDateColor)
            R.attr.inboxTitleColor -> provideColorByAttr(android.R.attr.textColorPrimary)
            R.attr.inboxReadTitleColor,
            R.attr.inboxReadDescriptionColor,
            R.attr.inboxDescriptionColor,
            R.attr.inboxDateColor,
            R.attr.inboxReadDateColor -> provideColorByAttr(android.R.attr.textColorSecondary)
            else -> null
        }

        return defaultColor ?: throw getResourceException(attr)
    }

    private fun getResourceException(attr: Int): Exception =
        IllegalArgumentException("Unknown attribute please set up ${context.resources.getResourceName(attr)} into your theme")
}