//
// RequestManager.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed
package com.pushwoosh.internal.network;

import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.config.ConfigPrefs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Implementation of {@link com.pushwoosh.internal.network.RequestManager}
 */
class PushwooshRequestManager implements RequestManager {
	private static final String TAG = "RequestManager";
	public static final String INTERACTIONS_WERE_STOPPED_EXCEPTION_STRING = "Device data was removed from Pushwoosh and all interactions were stopped";

	private final RegistrationPrefs registrationPrefs;
	private final ServerCommunicationManager serverCommunicationManager;
	private String baseRequestUrl;
	private boolean usingReverseProxy = false;
	private ConfigPrefs configPrefs;

	PushwooshRequestManager(RegistrationPrefs registrationPrefs, @Nullable ConfigPrefs configPrefs,
							ServerCommunicationManager serverCommunicationManager) {
		this.registrationPrefs = registrationPrefs;
		this.configPrefs = configPrefs;
		this.serverCommunicationManager = serverCommunicationManager;

		baseRequestUrl = registrationPrefs.baseUrl().get();
	}

	private boolean isRemoveAllDataDevice(){
		boolean removeAllDeviceData = registrationPrefs.removeAllDeviceData().get();

		if(removeAllDeviceData){
			PWLog.noise(TAG,"remove all data device is true, it is block request to server");
		}
		return removeAllDeviceData;
	}

	@Override
	public <Response> void sendRequest(final PushRequest<Response> request) {
		if(isRemoveAllDataDevice()){
			return;
		}
		sendRequest(request, null);
	}

	public <Response> void sendRequest(PushRequest<Response> request, @Nullable Callback<Response, NetworkException> callback) {
		sendRequest(request, baseRequestUrl, callback);
	}

	@Override
	public <Response> void sendRequest(final PushRequest<Response> request, final String baseUrl, final Callback<Response, NetworkException> callback) {
		if(isRemoveAllDataDevice()) {
			if (callback != null) {
				callback.process(Result.fromException(new NetworkException(INTERACTIONS_WERE_STOPPED_EXCEPTION_STRING)));
			}
			return;
		}
		new SendRequestTask<>(this, request, baseUrl, callback)
				.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
	}

	@NonNull
	public <Response> Result<Response, NetworkException> sendRequestSync(PushRequest<Response> request) {
		if(isRemoveAllDataDevice()){
			return Result.fromData(null);
		}
		return sendRequestSync(request, baseRequestUrl);
	}

	@Override
	public void updateBaseUrl(final String baseUrl) {
		saveBaseUrl(baseUrl);
	}

	@Override
	public void setReverseProxyUrl(String url) {
		usingReverseProxy = true;
		saveBaseUrl(url);
	}

	@Override
	public void disableReverseProxy() {
		usingReverseProxy = false;
	}

	private <Response> Result<Response, NetworkException> sendRequestSync(PushRequest<Response> request, String baseUrl) {
		if (baseUrl == null) {
			baseUrl = baseRequestUrl;
		}
		if (serverCommunicationManager != null && !serverCommunicationManager.isServerCommunicationAllowed() && !isAnalytics(request)) {
			NetworkException e = new NetworkException("As the server communication was stopped" +
					" the request was cached instead of being sent. Start the server communication" +
					" using startServerCommunication method of Pushwoosh class to send '" + request.getMethod() + "' request.");
			return Result.fromException(e);
		}
		if (!isAnalytics(request)) {
			PWLog.debug(TAG, "Try To send: " + request.getMethod() + "; baseUrl: " + baseUrl);
		}

		Exception exception;
		int statusCode = 0, pushwooshStatusCode = 0;
		try {
			JSONObject data = request.getParams();
			NetworkResult result = makeRequest(baseUrl, data, request.getMethod(), isAnalytics(request));

			statusCode = result.getStatus();
			pushwooshStatusCode = result.getPushwooshStatus();

			//postEvent might return 404 if wrong event name is provided, so in that case we want to
			// avoid handling it further
			if (NetworkResult.STATUS_NOT_FOUND == statusCode) {
				return Result.from(null, null);
			}

			if (NetworkResult.STATUS_OK == statusCode && NetworkResult.STATUS_OK == pushwooshStatusCode) {
				if (!isAnalytics(request)) {
					PWLog.debug(TAG, request.getMethod() + " response success");
				}

				JSONObject response = result.getResponse();
				// honor base url change
				if (response.has("base_url") && baseUrl.equals(baseRequestUrl) && !usingReverseProxy) {
					String newBaseUrl = response.optString("base_url");
					saveBaseUrl(newBaseUrl);
				}

				JSONObject responseData = response.optJSONObject("response");
				if (responseData == null) {
					responseData = new JSONObject();
				}

				return Result.fromData(request.parseResponse(responseData));
			} else {
				exception = new NetworkException(result.getResponse().toString());
			}
		} catch (Exception ex) {
			exception = ex;
		}

		if (!isAnalytics(request)) {
			PWLog.error(TAG, exception.getClass().getCanonicalName());
			if (exception instanceof ConnectionException) {
				PWLog.error(TAG, "ERROR: " + "connection error.");
			} else {
				PWLog.error(TAG, "ERROR: " + exception.getMessage(), exception);
			}
		}

		return Result.fromException(new ConnectionException(exception.getMessage(), statusCode, pushwooshStatusCode));
	}

	private <Response> boolean isAnalytics(final PushRequest<Response> request) {
		return request instanceof AnalyticsPushRequest;
	}

	private void saveBaseUrl(String url) {
		baseRequestUrl = url;
		registrationPrefs.baseUrl().set(url);
	}

	private NetworkResult makeRequest(final String baseUrl, JSONObject data, String methodName, boolean isAnalytics) throws Exception {
		try {
			URL url = new URL(baseUrl + methodName);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();


			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			connection.setDoOutput(true);

			JSONObject requestJson = new JSONObject();
			requestJson.put("request", data);

			connection.setRequestProperty("Content-Length", String.valueOf(requestJson.toString().getBytes().length));
			connection.setUseCaches(false);
			try (OutputStream connectionOutput = connection.getOutputStream()) {
				connectionOutput.write(requestJson.toString().getBytes());
				connectionOutput.flush();
			}

			NetworkResult networkResult = getNetworkResultFromConnection(connection);

			if (!isAnalytics) {
				PWLog.info(TAG, "\n"
						+ "x\n"
						+ "|     Pushwoosh request:\n"
						+ "| Url: " + url.toString() + "\n"
						+ "| Payload: " + requestJson.toString() + "\n"
						+ "| Response: " + networkResult.getResponse().toString() + "\n"
						+ "x");
			}

			return networkResult;
		} catch (Exception e) {
			//reset base url
			if (baseUrl.equals(baseRequestUrl)) {
				baseRequestUrl = registrationPrefs.getDefaultBaseUrl();
			}

			throw e;
		}
	}

	private NetworkResult getNetworkResultFromConnection(HttpURLConnection connection) throws IOException {
		InputStream inputStream;
		JSONObject responseJson = new JSONObject();
		int status = connection.getResponseCode();
		int pushwooshStatus = 0;
		if (isErrorResponseCode(connection.getResponseCode())) {
			inputStream = new BufferedInputStream(connection.getErrorStream());
			pushwooshStatus = status;
			try {
				responseJson.put("status_code",pushwooshStatus);
				responseJson.put("status_message", connection.getResponseMessage());
			} catch (JSONException e) {
				PWLog.error(TAG, e.getMessage());
			}
		} else {
			inputStream = new BufferedInputStream(connection.getInputStream());
		}

		try {
			if (connection.getContentLength() != 0) {
				try (ByteArrayOutputStream dataCache = new ByteArrayOutputStream()) {

					// Fully read data
					byte[] buff = new byte[1024];
					int len;
					while ((len = inputStream.read(buff)) >= 0) {
						dataCache.write(buff, 0, len);
					}

					responseJson = new JSONObject(dataCache.toString().trim());
					pushwooshStatus = responseJson.getInt("status_code");
				} catch (Exception e) {
					PWLog.error(TAG, e.getMessage());
				}
			}
		} finally {
			inputStream.close();
		}
		return new NetworkResult(status, pushwooshStatus, responseJson);
	}

	private boolean isErrorResponseCode(int code) {
		return code >= 400 && code < 600;
	}

	static class NetworkResult {
		static final int STATUS_OK = 200;
		static final int STATUS_NOT_FOUND = 404;

		private int pushwooshStatus;
		private int status;
		private JSONObject response;

		NetworkResult(int networkCode, int pushwooshCode, JSONObject data) {
			status = networkCode;
			pushwooshStatus = pushwooshCode;
			response = data;
		}

		int getStatus() {
			return status;
		}

		int getPushwooshStatus() {
			return pushwooshStatus;
		}

		JSONObject getResponse() {
			return response;
		}
	}

	private static class SendRequestTask<Response> extends AsyncTask<Void, Void, Result<Response, NetworkException>> {
		private final WeakReference<PushwooshRequestManager> requestManagerWeakRef;
		private final PushRequest<Response> request;
		private final String baseUrl;
		private final Callback<Response, NetworkException> callback;

		SendRequestTask(PushwooshRequestManager pushwooshRequestManager,
							   PushRequest<Response> request,
							   String baseUrl,
							   Callback<Response, NetworkException> callback) {
			this.requestManagerWeakRef = new WeakReference<>(pushwooshRequestManager);
			this.request = request;
			this.baseUrl = baseUrl;
			this.callback = callback;
		}

		@Override
		protected Result<Response, NetworkException> doInBackground(Void... voids) {
			if (requestManagerWeakRef.get() != null) {
				return requestManagerWeakRef.get().sendRequestSync(request, baseUrl);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Result<Response, NetworkException> result) {
			super.onPostExecute(result);
			if (result == null) {
				return;
			}
			if (callback != null) {
				callback.process(result);
			}
		}
	}
}
