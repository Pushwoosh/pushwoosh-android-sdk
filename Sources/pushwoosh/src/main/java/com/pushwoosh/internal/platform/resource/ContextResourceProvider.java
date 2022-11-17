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

package com.pushwoosh.internal.platform.resource;

import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.lang.ref.WeakReference;

public class ContextResourceProvider implements ResourceProvider {

	private final WeakReference<Context> context;

	public ContextResourceProvider(@Nullable final Context context) {
		this.context = new WeakReference<>(context);
	}

	@Nullable
	private Context getContext() {
		return context.get();
	}

	@Override
	public String getString(final int id, final Object... params) {
		return getContext() == null ? "" : getContext().getString(id, params);
	}

	@Override
	public int getIdentifier(final String resourceName, final String defType) {
		return getContext() == null ? -1 : getContext().getResources().getIdentifier(resourceName, defType, getContext().getPackageName());
	}

	@Override
	public Configuration getConfiguration() {
		return getContext() == null ? null : getContext().getResources().getConfiguration();
	}

	@Override
	public void getValue(final int res, final TypedValue value, final boolean resolveRefs) {
		if (getContext() == null) {
			return;
		}
		getContext().getResources().getValue(res, value, resolveRefs);
	}

	@Override
	public DisplayMetrics getDisplayMetrics() {
		return getContext() == null ? null : getContext().getResources().getDisplayMetrics();
	}

	@Override
	public float getDimension(final int res) {
		return getContext() == null ? -1 : getContext().getResources().getDimension(res);
	}
}
