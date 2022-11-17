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

//
// GeneralUtils.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed

package com.pushwoosh.internal.platform.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.TypedValue;

import com.pushwoosh.BuildConfig;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Locale;

public class GeneralUtils {
	public static final String SDK_VERSION = BuildConfig.VERSION_NAME;

	private static final String[] SUPPORTED_AUDIO_FORMATS = { ".mp3", ".3gp", ".mp4", ".m4a", ".aac", ".flac", ".ogg", ".wav" };

	public static void checkNotNullOrEmpty(String reference, String name) {
		checkNotNull(reference, name);
		if (reference.length() == 0) {
			throw new IllegalArgumentException(String.format("Please set the %1$s constant and recompile the app.", name));
		}
	}

	public static String md5(String s) {
		if (s == null) {
			return "";
		}

		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte[] messageDigest = digest.digest();

			// Create Hex String
			StringBuilder hexString = new StringBuilder();
			for (final byte aMessageDigest : messageDigest) {
				hexString.append(String.format("%02x", aMessageDigest));
			}
			return hexString.toString();

		} catch (Exception e) {
			// ignore
		}
		return "";
	}

	public static void checkNotNull(Object reference, String name) {
		if (reference == null) {
			throw new IllegalArgumentException(String.format("Please set the %1$s constant and recompile the app.", name));
		}
	}

	public static boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = AndroidPlatformModule.getManagerProvider().getConnectivityManager();
		NetworkInfo activeNetworkInfo = connectivityManager == null ? null : connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isAvailable() && activeNetworkInfo.isConnected();
	}

	public static boolean isStoreApp() {
		String name = AndroidPlatformModule.getAppInfoProvider().getInstallerPackageName();
		return !TextUtils.isEmpty(name);
	}

	public static boolean checkStickyBroadcastPermissions(Context context) {
		//noinspection ConstantConditions
		try {
			return context.getPackageManager().checkPermission("android.permission.BROADCAST_STICKY", context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
		} catch (Exception e) {
			PWLog.error("error in checking broadcast_sticky permission", e);
		}
		return false;
	}

	public static ArrayList<String> getRawResourses() {
		ArrayList<String> files = new ArrayList<>();
		try {
			Class<?> clazz = Class.forName(AndroidPlatformModule.getAppInfoProvider().getPackageName() + ".R$raw");

			Field[] fields = clazz.getFields();
			TypedValue value;

			for (final Field field : fields) {
				String name = field.getName();
				int res = AndroidPlatformModule.getResourceProvider().getIdentifier(name, "raw");
				value = new TypedValue();
				AndroidPlatformModule.getResourceProvider().getValue(res, value, true);

				if (isSound(value.string.toString())) {
					files.add(name);
				}
			}
		} catch (Exception ignore) {
			//stil nothing
		}

		//iterate the files from file:///android_asset/www/res (for Phonegap)
		try {
			final AssetManager assetManager = AndroidPlatformModule.getManagerProvider().getAssets();
			if (assetManager == null) {
				return files;
			}

			String[] list = assetManager.list("www/res");
			for (String name : list) {
				String[] assets = assetManager.list("www/res/" + name);
				if (assets.length != 0) {
					continue;    //directory
				}

				if (isSound(name)) {
					files.add(name);
				}
			}
		} catch (IOException e) {
			PWLog.exception(e);
		} catch (Exception e) {
			//stil nothing
		}

		return files;
	}

	public static int parseColor(String color) {
		int parsedColor = 0xFFFFFFFF;
		try {
			if (color.startsWith("#") && (color.length() == 7 || color.length() == 9)) {    //#rrggbb   #aarrggbb
				parsedColor = Color.parseColor(color);
			} else if (color.startsWith("#") && color.length() == 4) {     //#rgb
				char[] chars = color.toCharArray();
				parsedColor = Color.parseColor("#" + chars[1] + chars[1] + chars[2] + chars[2] + chars[3] + chars[3]);
			} else if (color.startsWith("#") && color.length() == 5) {      //#argb
				char[] chars = color.toCharArray();
				parsedColor = Color.parseColor("#" + chars[1] + chars[1] + chars[2] + chars[2] + chars[3] + chars[3] + chars[4] + chars[4]);
			} else {                            //255,255,255
				String[] colorArr = color.split(",");
				parsedColor = Color.argb(Integer.parseInt(colorArr[3]),
										 Integer.parseInt(colorArr[0]),
										 Integer.parseInt(colorArr[1]),
										 Integer.parseInt(colorArr[2]));
			}
		} catch (Exception e) {
			PWLog.exception(e);
		}

		return parsedColor;
	}

	private static boolean isSound(String fileName) {
		for (String format : SUPPORTED_AUDIO_FORMATS) {
			if (fileName.toLowerCase(Locale.US).endsWith(format)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isMainActivity(Activity activity) {
		PackageManager packageManager = activity.getPackageManager();
		Intent launchIntent = packageManager.getLaunchIntentForPackage(activity.getPackageName());

        if (launchIntent == null)
            return false;
        
        ComponentName componentName = launchIntent.getComponent();
        
        if (componentName == null)
            return false;
        
		String launchActivity;

		try {
			ActivityInfo activityInfo = packageManager.getActivityInfo(componentName, 0);
			if (activityInfo.targetActivity != null) {
				launchActivity = activityInfo.targetActivity;
		 	} else {
				launchActivity = componentName.getClassName();
			}
		} catch (PackageManager.NameNotFoundException e) {
			launchActivity = componentName.getClassName();
		}
		
		return TextUtils.equals(launchActivity, activity.getClass().getName());
	}

	public static int getAppVersion() {
		return AndroidPlatformModule.getAppInfoProvider().getVersionCode();
	}

	public static String getSenderId() {
		String manifestSenderId = PushwooshPlatform.getInstance().getConfig().getProjectId();
		return manifestSenderId == null || manifestSenderId.isEmpty() ? RepositoryModule.getRegistrationPreferences().projectId().get() : manifestSenderId;
	}
}
