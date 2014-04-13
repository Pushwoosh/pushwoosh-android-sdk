//
// RequestManager.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed
package com.arellomobile.android.push.request;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.json.JSONObject;

import com.arellomobile.android.push.utils.PreferenceUtils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class RequestManager
{
	private static final String META_NAME_PUSHWOOSH_URL = "PushwooshUrl";

	private static final String TAG = "Pushwoosh: Request manager";

	public static final int MAX_TRIES = 1;

	// due to lots of Android 2.2 not honoring updated GeoTrust certificate chain
	public static boolean useSSL = false;
	private static final String BASE_URL_SECURE = "https://cp.pushwoosh.com/json/1.3/";
	private static final String BASE_URL = "http://cp.pushwoosh.com/json/1.3/";

	public static void sendRequest(Context context, PushRequest request) throws Exception
	{
		Map<String, Object> data = request.getParams(context);

		Log.w(TAG, "Try To sent: " + request.getMethod());

		NetworkResult res = new NetworkResult(500, 0, null);
		Exception exception = new Exception();

		for (int i = 0; i < MAX_TRIES; ++i)
		{
			try
			{
				res = makeRequest(context, data, request.getMethod());
				if (200 != res.getResultCode())
				{
					continue;
				}

				if (200 != res.getPushwooshCode())
				{
					break;
				}

				Log.w(TAG, request.getMethod() + " response success");

				JSONObject response = res.getResultData();
				if (response != null)
				{
					// honor base url change
					if (response.has("base_url"))
					{
						String newBaseUrl = response.optString("base_url");
						PreferenceUtils.setBaseUrl(context, newBaseUrl);
					}

					request.parseResponse(response);
				}

				return;
			}
			catch (Exception ex)
			{
				exception = ex;
			}
		}

		Log.e(TAG, "ERROR: " + exception.getMessage() + ". Response = " + res.getResultData(), exception);
		throw exception;
	}

	private static NetworkResult makeRequest(Context context, Map<String, Object> data, String methodName) throws Exception
	{
		NetworkResult result = new NetworkResult(500, 0, null);
		OutputStream connectionOutput = null;
		InputStream inputStream = null;
		try
		{
			// get the base url from preferences first
			String baseUrl = PreferenceUtils.getBaseUrl(context);
			if(TextUtils.isEmpty(baseUrl))
			{
				baseUrl = getDefaultUrl(context);
			}
			
			if (!baseUrl.endsWith("/"))
			{
				baseUrl += "/";
			}
			
			// save it
			PreferenceUtils.setBaseUrl(context, baseUrl);
			
			URL url = new URL(baseUrl + methodName);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

			connection.setDoOutput(true);

			JSONObject innerRequestJson = new JSONObject();

			for (String key : data.keySet())
			{
				innerRequestJson.put(key, data.get(key));
			}

			JSONObject requestJson = new JSONObject();
			requestJson.put("request", innerRequestJson);

			connection.setRequestProperty("Content-Length", String.valueOf(requestJson.toString().getBytes().length));

			connectionOutput = connection.getOutputStream();
			connectionOutput.write(requestJson.toString().getBytes());
			connectionOutput.flush();
			connectionOutput.close();

			inputStream = new BufferedInputStream(connection.getInputStream());

			ByteArrayOutputStream dataCache = new ByteArrayOutputStream();

			// Fully read data
			byte[] buff = new byte[1024];
			int len;
			while ((len = inputStream.read(buff)) >= 0)
			{
				dataCache.write(buff, 0, len);
			}

			// Close streams
			dataCache.close();

			String jsonString = new String(dataCache.toByteArray()).trim();
			Log.w(TAG, "PushWooshResult: " + jsonString);

			try
			{
				JSONObject resultJSON = new JSONObject(jsonString);
				result.setData(resultJSON);
				result.setCode(connection.getResponseCode());
				result.setPushwooshCode(resultJSON.getInt("status_code"));
			}
			catch(Exception e)
			{
				//reset base url
				PreferenceUtils.setBaseUrl(context, getDefaultUrl(context));		
				throw e;
			}
		}
		finally
		{
			if (null != inputStream)
			{
				inputStream.close();
			}
			if (null != connectionOutput)
			{
				connectionOutput.close();
			}
		}

		return result;
	}

	private static String getDefaultUrl(Context context)
	{
		String url = null;

		//Get Base URL from Metadata
		PackageManager packageManager = context.getPackageManager();
		try
		{
			ApplicationInfo info = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			Bundle metaData = info.metaData;
			if (metaData != null)
			{
				url = metaData.getString(META_NAME_PUSHWOOSH_URL);
			}
		}
		catch (NameNotFoundException e)
		{
			//nothing
		}
		
		if (TextUtils.isEmpty(url))
		{
			url = useSSL ? BASE_URL_SECURE : BASE_URL;
		}

		return url;
	}

	public static class NetworkResult
	{
		private int mPushwooshCode;
		private int mResultCode;
		private JSONObject mResultData;

		public NetworkResult(int networkCode, int pushwooshCode, JSONObject data)
		{
			mResultCode = networkCode;
			mPushwooshCode = pushwooshCode;
			mResultData = data;
		}

		public void setCode(int code)
		{
			mResultCode = code;
		}

		public void setPushwooshCode(int code)
		{
			mPushwooshCode = code;
		}

		public void setData(JSONObject data)
		{
			mResultData = data;
		}

		public int getResultCode()
		{
			return mResultCode;
		}

		public int getPushwooshCode()
		{
			return mPushwooshCode;
		}

		public JSONObject getResultData()
		{
			return mResultData;
		}
	}
}
