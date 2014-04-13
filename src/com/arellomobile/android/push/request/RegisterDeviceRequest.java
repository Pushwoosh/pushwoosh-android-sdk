package com.arellomobile.android.push.request;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.content.pm.PackageManager;

import com.arellomobile.android.push.utils.GeneralUtils;

public class RegisterDeviceRequest extends PushRequest
{
	private static final String GOOGLE = "3";
	private static final String AMAZON = "9";

	private String pushToken;

	public RegisterDeviceRequest(String pushToken)
	{
		this.pushToken = pushToken;
	}

	@Override
	public String getMethod()
	{
		return "registerDevice";
	}

	@Override
	protected void buildParams(Context context, Map<String, Object> params)
	{
		params.put("device_name", GeneralUtils.isTablet(context) ? "Tablet" : "Phone");

		//check for Amazon (Kindle) or Google device
		if (GeneralUtils.isAmazonDevice())
		{
			params.put("device_type", AMAZON);
		}
		else
		{
			params.put("device_type", GOOGLE);
		}

		params.put("v", "2.2");
		params.put("language", Locale.getDefault().getLanguage());
		params.put("timezone", Calendar.getInstance().getTimeZone().getRawOffset() / 1000); // converting from milliseconds to seconds

		String packageName = context.getPackageName();
		params.put("android_package", packageName);
		params.put("push_token", pushToken);

		ArrayList<String> rawResourses = GeneralUtils.getRawResourses(context);
		params.put("sounds", rawResourses);

		try
		{
			//noinspection ConstantConditions
			params.put("app_version", context.getPackageManager().getPackageInfo(packageName, 0).versionName);
		}
		catch (PackageManager.NameNotFoundException e)
		{
			// pass
		}
	}
}
