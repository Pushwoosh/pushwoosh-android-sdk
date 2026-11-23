package com.pushwoosh.internal.network;

import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONException;
import org.json.JSONObject;

public class CreateTestDeviceRequest extends PushRequest<Void> {
    private final String mName;
    private final String mDesc;
    private final boolean mAutoCreated;

    public CreateTestDeviceRequest(String name, String desc, boolean autoCreated) {
        mName = name;
        mDesc = desc;
        mAutoCreated = autoCreated;
    }

    @Override
    public String getMethod() {
        return "createTestDevice";
    }

    @Override
    protected void buildParams(JSONObject params) throws JSONException {
        params.put("name", mName);
        params.put("description", mDesc);
        params.put("auto_created", mAutoCreated);
        params.put(
                "push_token",
                RepositoryModule.getRegistrationPreferences().pushToken().get());
        if (!RepositoryModule.getNotificationPreferences()
                .isCollectingDeviceLocaleAllowed()
                .get()) {
            return;
        }
        params.put(
                "language",
                RepositoryModule.getRegistrationPreferences().language().get());
    }
}
