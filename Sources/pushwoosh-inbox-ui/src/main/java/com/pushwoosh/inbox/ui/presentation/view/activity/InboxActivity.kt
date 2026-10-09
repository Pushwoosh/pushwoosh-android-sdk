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

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.presentation.view.fragment.InboxFragment

open class InboxActivity : AppCompatActivity() {

    companion object {
        const val TAG = "pushwoosh.inbox.ui.InboxActivity"
        const val FRAGMENT_TAG = "$TAG.InboxFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setupSystemBars()

        setContentView(R.layout.pw_activity_inbox)

        // Apply background color to activity root view and content container
        val backgroundColor = PushwooshInboxStyle.backgroundColor
        if (backgroundColor != null) {
            findViewById<android.view.View>(android.R.id.content)
                .setBackgroundColor(backgroundColor)
            findViewById<android.view.View>(R.id.inboxContentContainer)
                ?.setBackgroundColor(backgroundColor)
        }

        attachInboxFragment()
    }

    private fun setupSystemBars() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        val belowEdgeToEdge = Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM

        statusBarColor()?.let { color ->
            if (belowEdgeToEdge) {
                @Suppress("DEPRECATION")
                window.statusBarColor = color
            }
            insetsController.isAppearanceLightStatusBars = isColorLight(color)
        }

        PushwooshInboxStyle.backgroundColor?.let { color ->
            if (belowEdgeToEdge) {
                @Suppress("DEPRECATION")
                window.navigationBarColor = color
            }
            insetsController.isAppearanceLightNavigationBars = isColorLight(color)
        }
    }

    /**
     * The color sitting under the status bar: the toolbar covers it when shown, otherwise the
     * screen reaches it. Null leaves the bar as the theme painted it.
     */
    private fun statusBarColor(): Int? {
        val toolbarShown = PushwooshInboxStyle.showToolbar ?: true
        return if (toolbarShown) {
            PushwooshInboxStyle.barBackgroundColor
        } else {
            PushwooshInboxStyle.backgroundColor
        }
    }

    private fun isColorLight(color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        // Calculate luminance using standard formula
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        return luminance > 0.5
    }

    protected open fun attachInboxFragment() {
        var needToAdd = true
        val fragment =
            supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)?.also { needToAdd = false }
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
