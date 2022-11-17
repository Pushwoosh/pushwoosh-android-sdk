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

package com.pushwoosh.testingapp;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import com.pushwoosh.internal.utils.PWLog;

public class LogsBuffer implements PWLog.LogsUpdateListener {
	public static final LogsBuffer instance = new LogsBuffer();

	private StringBuilder stringBuilder = new StringBuilder();

	private Handler handler = new Handler(Looper.getMainLooper());
	private WeakReference<TextView> textView;

	private LogsBuffer() {/*do nothing*/}

	@Override
	public void logUpdated(final PWLog.Level level, final String message) {
		try {
			stringBuilder
					.append(SimpleDateFormat.getTimeInstance().format(new Date()))
					.append("\n")
					.append(level.name())
					.append(": ")
					.append(message)
					.append("\n\n");

			if (textView != null && textView.get() != null) {
				handler.post(() -> {
					if (textView.get() != null) {
						textView.get().setText(stringBuilder.toString());
					}
				});
			}
		} catch (Throwable t) {
			// ignore
		}
	}

	public void setTextView(final TextView textView) {
		this.textView = new WeakReference<>(textView);
		if (textView != null) {
			textView.setText(stringBuilder.toString());
		}
	}

	public void clear() {
		stringBuilder = new StringBuilder();
	}
}
