//
// GeneralUtils.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed

package com.arellomobile.android.push.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.TypedValue;

/**
 * Date: 16.08.12 Time: 21:01
 * 
 * @author mig35
 */
public class GeneralUtils
{
	private static final String SHARED_KEY = "deviceid";
	private static final String SHARED_PREF_NAME = "com.arellomobile.android.push.deviceid";

	public static final String[] SUPPORTED_AUDIO_FORMATS = { ".mp3", ".3gp", ".mp4", ".m4a", ".aac", ".flac", ".ogg", ".wav" };

	private static List<String> sWrongAndroidDevices;

	static
	{
		sWrongAndroidDevices = new ArrayList<String>();

		sWrongAndroidDevices.add("9774d56d682e549c");
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("WorldWriteableFiles")
	public static String getDeviceUUID(Context context)
	{
		final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		// see http://code.google.com/p/android/issues/detail?id=10603
		if (null != androidId && !sWrongAndroidDevices.contains(androidId))
		{
			return androidId;
		}
		try
		{
			final String deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
			if (null != deviceId)
			{
				return deviceId;
			}
		}
		catch (RuntimeException e)
		{
			// if no
		}

		SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_WORLD_WRITEABLE);
		// try to get from pref
		String deviceId = sharedPreferences.getString(SHARED_KEY, null);
		if (null != deviceId)
		{
			return deviceId;
		}
		// generate new
		deviceId = UUID.randomUUID().toString();
		SharedPreferences.Editor editor = sharedPreferences.edit();
		// and save it
		editor.putString(SHARED_KEY, deviceId);
		editor.commit();
		return deviceId;
	}

	@SuppressLint("InlinedApi")
	public static boolean isTablet(Context context)
	{
		int xlargeBit = Configuration.SCREENLAYOUT_SIZE_XLARGE;
		Configuration config = context.getResources().getConfiguration();
		return (config.screenLayout & xlargeBit) == xlargeBit;
	}

	public static void checkNotNullOrEmpty(String reference, String name)
	{
		checkNotNull(reference, name);
		if (reference.length() == 0)
		{
			throw new IllegalArgumentException(String.format("Please set the %1$s constant and recompile the app.", name));
		}
	}

	public static void checkNotNull(Object reference, String name)
	{
		if (reference == null)
		{
			throw new IllegalArgumentException(String.format("Please set the %1$s constant and recompile the app.", name));
		}
	}

	public static boolean isAppOnForeground(Context context)
	{
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		if (appProcesses == null)
		{
			return false;
		}

		final String packageName = context.getPackageName();
		for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses)
		{
			if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName))
			{
				return true;
			}
		}

		return false;
	}

	public static boolean checkStickyBroadcastPermissions(Context context)
	{
		//noinspection ConstantConditions
		return context.getPackageManager().checkPermission("android.permission.BROADCAST_STICKY", context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
	}

	public static boolean isAmazonDevice()
	{
		try
		{
			Class.forName("com.amazon.device.messaging.ADM");
			return true;
		}
		catch (ClassNotFoundException e)
		{
			// Ignore
		}
		return false;
	}

	public static ArrayList<String> getRawResourses(Context context)
	{
		ArrayList<String> files = new ArrayList<String>();
		try
		{
			Class<?> clazz = Class.forName(context.getPackageName() + ".R$raw");

			Field[] fields = clazz.getFields();
			TypedValue value = null;

			for (int i = 0; i < fields.length; i++)
			{
				String name = fields[i].getName();
				int res = context.getResources().getIdentifier(name, "raw", context.getPackageName());
				value = new TypedValue();
				context.getResources().getValue(res, value, true);

				if (isSound(value.string.toString()))
				{
					files.add(name);
				}
			}
		}
		catch (ClassNotFoundException e)
		{
			//nothing
		}

		return files;
	}

	protected static boolean isSound(String fileName)
	{
		for (int i = 0; i < SUPPORTED_AUDIO_FORMATS.length; i++)
		{
			String format = SUPPORTED_AUDIO_FORMATS[i];

			if (fileName.toLowerCase(Locale.US).endsWith(format))
			{
				return true;
			}
		}

		return false;
	}
}
