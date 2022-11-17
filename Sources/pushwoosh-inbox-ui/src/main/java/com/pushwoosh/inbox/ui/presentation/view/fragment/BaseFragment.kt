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

package com.pushwoosh.inbox.ui.presentation.view.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.pushwoosh.inbox.ui.presentation.lifecycle.Lifecycle
import com.pushwoosh.inbox.ui.presentation.lifecycle.LifecycleListener

open class BaseFragment : androidx.fragment.app.Fragment(), Lifecycle {
    private val lifecycleListeners = mutableListOf<LifecycleListener>()

    override fun addLifecycleListener(lifecycleListener: LifecycleListener) {
        lifecycleListeners.add(lifecycleListener)
    }

    override fun removeLifecycleListener(lifecycleListener: LifecycleListener) {
        lifecycleListeners.remove(lifecycleListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleListeners.forEach { it.onCreate(savedInstanceState) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleListeners.forEach { it.onViewCreated() }
    }

    override fun onStart() {
        super.onStart()
        lifecycleListeners.forEach { it.onStart() }
    }

    override fun onStop() {
        super.onStop()
        lifecycleListeners.forEach { it.onStop() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lifecycleListeners.forEach { it.onSaveInstanceState(outState) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lifecycleListeners.forEach { it.onViewDestroy() }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleListeners.forEach { it.onDestroy(activity?.isFinishing ?: true) }
    }
}