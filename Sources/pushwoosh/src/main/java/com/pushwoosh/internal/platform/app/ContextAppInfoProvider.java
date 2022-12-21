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

package com.pushwoosh.internal.platform.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class ContextAppInfoProvider implements AppInfoProvider {
	private final WeakReference<Context> context;

	public ContextAppInfoProvider(@Nullable final Context context) {
		this.context = new WeakReference<>(context);
	}

	@Nullable
	private Context getContext() {
		return context.get();
	}

	@Override
	@Nullable
	public ApplicationInfo getApplicationInfo() {
		try {
			return getContext() == null ? null : getContext().getPackageManager().getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);
		} catch (Exception e) {
			PWLog.exception(e);
		}

		return null;
	}

	@Override
	public String getPackageName() {
		return getContext() == null ? "" : getContext().getPackageName();
	}

	@Override
	@Nullable
	public String getVersionName() {
		try {
			return getContext() == null ? null : getContext().getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			PWLog.exception(e);
		}

		return null;
	}

	@Override
	public String getInstallerPackageName() {
		return getContext() == null ? null : getContext().getPackageManager().getInstallerPackageName(getPackageName());
	}

	@Override
	public int getVersionCode() {
		try {
			return getContext() == null ? 0 : getContext().getPackageManager()
					.getPackageInfo(getPackageName(), 0)
					.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			PWLog.exception(e);
		}

		return 0;
	}

	@Override
	public File getExternalCacheDir() {
		return getContext() == null ? null : getContext().getExternalCacheDir();
	}

	@Override
	public CharSequence getApplicationLabel() {
		return getContext() == null ? null : getContext().getPackageManager().getApplicationLabel(getContext().getApplicationInfo());
	}
}
