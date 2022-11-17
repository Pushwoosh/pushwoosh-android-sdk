/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.testingapp;

import com.pushwoosh.richmedia.RichMediaManager;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.richmedia.RichMediaPresentingDelegate;
import com.pushwoosh.richmedia.RichMedia;
import com.pushwoosh.internal.utils.PWLog;

import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultRichMediaPresentingDelegate implements RichMediaPresentingDelegate {
    private final String TAG = DefaultRichMediaPresentingDelegate.class.getSimpleName();
    private ArrayDeque<RichMedia> richMediaQueue = new ArrayDeque<>();
    private RichMedia currentRichMedia = null;
    private ReentrantLock reentrantLock;

    public DefaultRichMediaPresentingDelegate() {
        PWLog.noise(TAG, "new DefaultRichMediaPresentingDelegate:" + this);
        reentrantLock = new ReentrantLock();
    }

    @Override
    public boolean shouldPresent(RichMedia richMedia) {
        PWLog.noise(TAG, "shouldPresent:" + richMedia);
        if (currentRichMedia == null) {
            PWLog.noise(TAG, "currentRichMedia is null");
        }
        if (richMedia.isLockScreen()) {
            PWLog.noise(TAG, "isLockScreen is true");
            return true;
        }
        try {
            reentrantLock.lock();
            if (currentRichMedia == null) {
                PWLog.noise(TAG, "show:" + richMedia);
                currentRichMedia = richMedia;
                return true;
            } else {
                PWLog.noise(TAG, "add to queue:" + richMedia);
                richMediaQueue.add(richMedia);
                return false;
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public void onPresent(RichMedia richMedia) {
        PWLog.noise(TAG, "onPresent" + richMedia);
    }

    @Override
    public void onError(RichMedia richMedia, PushwooshException pushwooshException) {
        PWLog.error(TAG, pushwooshException + " richMedia:"+richMedia.toString());
        tryShowNextRichMediaThreadSafety();
    }

    @Override
    public void onClose(RichMedia richMedia) {
        PWLog.noise(TAG, "onClose:" + richMedia);
        tryShowNextRichMediaThreadSafety();
    }

    private void tryShowNextRichMediaThreadSafety() {
        try {
            reentrantLock.lock();
            tryShowNextRichMedia();
        } finally {
            reentrantLock.unlock();
        }
    }

    private void tryShowNextRichMedia() {
        if (!richMediaQueue.isEmpty()) {
			currentRichMedia = richMediaQueue.poll();
			PWLog.noise(TAG, "try manual show:" + currentRichMedia);
			RichMediaManager.present(currentRichMedia);
		} else {
			PWLog.noise(TAG, "richMediaQueue is empty");
			currentRichMedia = null;
		}
    }
}
