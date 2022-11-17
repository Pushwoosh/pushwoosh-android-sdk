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

package com.pushwoosh.inbox.ui.presentation.presenter

import android.os.Bundle
import com.pushwoosh.inbox.ui.presentation.lifecycle.LifecycleListener

open class BasePresenter : LifecycleListener {
    protected var viewEnable = false
    protected var restore = false

    override fun onCreate(bundle: Bundle?) {
        restore = bundle != null
        if (bundle != null) {
            restoreState(bundle)
        }
    }

    protected open fun restoreState(bundle: Bundle) {

    }

    override fun onViewCreated() {
        viewEnable = true
    }

    override fun onSaveInstanceState(out: Bundle) {

    }

    override fun onViewDestroy() {
        viewEnable = false
    }

    override fun onDestroy(isFinished: Boolean) {

    }

    override fun onStart() {

    }

    override fun onStop() {

    }
}