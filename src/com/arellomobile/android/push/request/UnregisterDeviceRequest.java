package com.arellomobile.android.push.request;

public class UnregisterDeviceRequest extends PushRequest {

	@Override
	public String getMethod() {
		return "unregisterDevice";
	}
}
