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

package com.pushwoosh.internal.platform.reciever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

public class ContextReceiverProvider implements ReceiverProvider {
	private final WeakReference<Context> context;

	public ContextReceiverProvider(@Nullable final Context context) {
		this.context = new WeakReference<>(context);
	}

	@Nullable
	private Context getContext() {
		return context.get();
	}

	@Override
	public Intent registerReceiver(final BroadcastReceiver broadcastReceiver, final IntentFilter intentFilter) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return getContext() == null ? null : getContext().registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
		} else {
			return getContext() == null ? null : getContext().registerReceiver(broadcastReceiver, intentFilter);
		}
	}

	@Override
	public void sendBroadcast(final Intent intent) {
		if (getContext() == null) {
			return;
		}
		getContext().sendBroadcast(intent);
	}

	@Override
	public void sendBroadcast(final Intent broadcastIntent, final String permission) {
		if (getContext() == null) {
			return;
		}
		getContext().sendBroadcast(broadcastIntent, permission);
	}
}
