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

package com.pushwoosh.inbox.ui.presentation.view.activity

import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.presentation.view.fragment.InboxFragment
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.SpannableString
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import android.graphics.PorterDuffColorFilter


open class InboxActivity : AppCompatActivity() {

    companion object {
        const val TAG = "pushwoosh.inbox.ui.InboxActivity"
        const val FRAGMENT_TAG = TAG + ".InboxFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pw_activity_inbox)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setColorActionBar()
        setTitle()
        setTextColorBar()
        setColorHomeButton()

        attachInboxFragment()
    }

    private fun setColorHomeButton() {
        val barAccentColor: Int = PushwooshInboxStyle.barAccentColor ?: return
        val drawable = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_material)
        val porterDuffColorFilter = PorterDuffColorFilter(barAccentColor, PorterDuff.Mode.SRC_IN)
        drawable?.colorFilter = porterDuffColorFilter
        supportActionBar?.setHomeAsUpIndicator(drawable)
    }

    private fun setTitle() {
        val title: String = PushwooshInboxStyle.barTitle ?: return
        supportActionBar?.title = title
    }

    private fun setTextColorBar() {
        val textColor: Int = PushwooshInboxStyle.barTextColor ?: return

        val text = SpannableString(supportActionBar?.title)
        text.setSpan(ForegroundColorSpan(textColor), 0, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        supportActionBar?.title = text
    }

    private fun setColorActionBar() {
        val barColor: Int = PushwooshInboxStyle.barBackgroundColor ?: return
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(barColor))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected open fun attachInboxFragment() {
        var needToAdd = true
        val fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)?.also { needToAdd = false }
                ?: InboxFragment()

        val beginTransaction = supportFragmentManager.beginTransaction()
        if (needToAdd) {
            beginTransaction.add(R.id.inboxContentContainer, fragment, FRAGMENT_TAG)
        } else {
            beginTransaction.attach(fragment)
        }
        beginTransaction.commitNowAllowingStateLoss()
    }
}
