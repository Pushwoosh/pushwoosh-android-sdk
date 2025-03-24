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

package com.pushwoosh.inbox.ui.presentation.view.adapter.inbox

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.util.TypedValue
import android.view.View
import androidx.appcompat.R as AppCompatR
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.data.InboxMessageType
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.databinding.PwItemInboxBinding
import com.pushwoosh.inbox.ui.presentation.view.adapter.BaseRecyclerAdapter
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import com.pushwoosh.inbox.ui.utils.GlideUtils
import com.pushwoosh.inbox.ui.utils.parseToString

class InboxViewHolder(adapter: InboxAdapter,
                      val binding: PwItemInboxBinding,
                      private val colorSchemeProvider: ColorSchemeProvider,
                      attachmentClickListener: ((String, View) -> Unit)) : BaseRecyclerAdapter.ViewHolder<InboxMessage>(binding, adapter) {
    var attachmentClickListener : (String, View) -> Unit = attachmentClickListener

    override fun fillView(model: InboxMessage?, position: Int) {
        if (model == null) {
            return
        }

        itemView.background = colorSchemeProvider.cellBackground
        binding.inboxLabelTextView.text = getInboxLabelText(model.title,
            colorSchemeProvider.titleColor,
            model.sendDate.parseToString(),
            colorSchemeProvider.dateColor)
        if (PushwooshInboxStyle.descriptionTextSize != null)
            binding.inboxDescriptionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, PushwooshInboxStyle.descriptionTextSize!!)

        binding.inboxDescriptionTextView.setTextColor(colorSchemeProvider.descriptionColor)
        binding.inboxStatusImageView.setColorFilter(colorSchemeProvider.imageColor)

        binding.inboxDescriptionTextView.text = model.message
        binding.inboxStatusImageView.setImageResource(model.type.getResource())

        binding.inboxLabelTextView.isSelected = !model.isActionPerformed
        binding.inboxStatusImageView.isSelected = binding.inboxLabelTextView.isSelected
        binding.inboxDescriptionTextView.isSelected = binding.inboxLabelTextView.isSelected

        Glide.with(itemView)
                .clear(itemView)

        val requestBuilder =
        if (TextUtils.isEmpty(model.imageUrl)) {
            Glide.with(itemView.context)
                    .load(colorSchemeProvider.defaultIcon)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            binding.inboxImageView.setImageDrawable(colorSchemeProvider.defaultIcon)
                            return true
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            return false
                        }

                    })
        } else {
            Glide.with(itemView)
                    .load(model.imageUrl)
        }

        GlideUtils.applyInto(requestBuilder, binding.inboxImageView)


        val bannerUrl = model.bannerUrl
        if (bannerUrl != null && !TextUtils.isEmpty(bannerUrl)) {
            binding.inboxBannerImage.setOnClickListener{ attachmentClickListener.invoke(bannerUrl, binding.inboxBannerImage) }
            binding.inboxBannerImage.visibility = View.VISIBLE
            Glide.with(itemView.context)
                    .load(bannerUrl)
                    .into(binding.inboxBannerImage)
        } else {
            binding.inboxBannerImage.visibility = View.GONE
        }
    }

    private fun getInboxLabelText(title: String?, titleColor: ColorStateList, date: String?, dateColor: ColorStateList) : CharSequence {
        val ssb = SpannableStringBuilder()
        if (title != null && !TextUtils.isEmpty(title)) {
            val titleSpannable = SpannableString(title)
            val styledAttrs = context.theme.obtainStyledAttributes(intArrayOf(R.attr.inboxTitleAppearance))
            val titleStyle = styledAttrs.getResourceId(0, AppCompatR.style.TextAppearance_AppCompat_Subhead)
            styledAttrs.recycle()
            
            val titleAppearanceSpan = getTextAppearanceSpan(context,
                    titleStyle,
                    PushwooshInboxStyle.titleTextSize,
                    titleColor,
                    PushwooshInboxStyle.getTitleFont())
            titleSpannable.setSpan(titleAppearanceSpan, 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            ssb.append(titleSpannable)
            ssb.append("  ")
        }
        if (date != null && !TextUtils.isEmpty(date)) {
            val dateSpannable = SpannableString(date)
            val styledAttrs = context.theme.obtainStyledAttributes(intArrayOf(R.attr.inboxDateAppearance))
            val dateStyle = styledAttrs.getResourceId(0, AppCompatR.style.TextAppearance_AppCompat_Caption)
            styledAttrs.recycle()
            
            val dateAppearanceSpan = getTextAppearanceSpan(context,
                    dateStyle,
                    PushwooshInboxStyle.dateTextSize,
                    dateColor,
                    PushwooshInboxStyle.getDateFont())
            dateSpannable.setSpan(dateAppearanceSpan, 0, date.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            ssb.append(dateSpannable)
            ssb.append(" ")
        }
        return ssb
    }

    private fun getTextAppearanceSpan(context: Context, appearance : Int, textSizeSp: Float?, colorStateList: ColorStateList, customFont: Typeface? = null) : TextAppearanceSpan {

        val tempSpan = TextAppearanceSpan(context, appearance)
        val textSize = if (textSizeSp != null) spToPx(textSizeSp) else tempSpan.textSize
        
        return TextAppearanceSpan(
            null,
            customFont?.style ?: Typeface.NORMAL,
            textSize,
            colorStateList,
            null
        )
    }

    private fun spToPx(sp: Float) : Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics).toInt()
    }
}

fun InboxMessageType.getResource() = when (this) {
    InboxMessageType.PLAIN -> R.drawable.inbox_ic_app_open
    InboxMessageType.RICH_MEDIA -> R.drawable.inbox_ic_rich_media
    InboxMessageType.URL -> R.drawable.inbox_ic_remote_url
    InboxMessageType.DEEP_LINK -> R.drawable.inbox_ic_deep_link
}