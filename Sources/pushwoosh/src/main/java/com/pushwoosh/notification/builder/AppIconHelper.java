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

package com.pushwoosh.notification.builder;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.pushwoosh.internal.utils.PWLog;

@RequiresApi(Build.VERSION_CODES.M)
public class AppIconHelper {

	@Nullable
	public static Icon getAppIcon(@Nullable Context context, String packageName) {
		if (context == null) {
			return null;
		}

		ApplicationInfo appInfoProvider = context.getApplicationInfo();
		if (appInfoProvider == null) {
			return null;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			return getAppIcon(context.getPackageManager(), packageName);
		}

		try {
			return Icon.createWithResource(context, appInfoProvider.icon);
		} catch (Exception e) {
			PWLog.error("Failed creation of icon", e);
		}

		return null;
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private static Icon getAppIcon(@NonNull PackageManager packageManager, String packageName) {

		try {
			Drawable drawable = packageManager.getApplicationIcon(packageName);

			if (drawable instanceof BitmapDrawable) {
				return Icon.createWithBitmap(((BitmapDrawable) drawable).getBitmap());
			} else if (drawable instanceof AdaptiveIconDrawable) {
				Drawable foregroundDr = ((AdaptiveIconDrawable) drawable).getForeground();

				Drawable[] drr = new Drawable[]{
						foregroundDr };

				LayerDrawable layerDrawable = new LayerDrawable(drr);

				int width = layerDrawable.getIntrinsicWidth();
				int height = layerDrawable.getIntrinsicHeight();

				Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

				Canvas canvas = new Canvas(bitmap);

				layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
				layerDrawable.draw(canvas);

				return Icon.createWithAdaptiveBitmap(bitmap);
			}
		} catch (PackageManager.NameNotFoundException e) {
			PWLog.error("Failed to create icon", e);
		}

		return null;
	}

	static int getAppIconResId(Context context) {
		return context.getApplicationInfo().icon;
	}
} 