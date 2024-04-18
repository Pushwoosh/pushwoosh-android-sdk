package com.pushwoosh.internal.network;

import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class CreateTestDeviceRequest extends PushRequest<Void> {
	private String mName;
	private String mDesc;

	public CreateTestDeviceRequest(String name, String desc) {
		mName = name;
		mDesc = desc;
	}

	@Override
	public String getMethod() {
		return "createTestDevice";
	}

	@Override
	public boolean shouldUseJitter(){ return false; }

	@Override
	protected void buildParams(JSONObject params) throws JSONException {
		params.put("name", mName);
		params.put("description", mDesc);
		params.put("push_token", RepositoryModule.getRegistrationPreferences().pushToken().get());
		params.put("language", Locale.getDefault().getLanguage());

	}
}
