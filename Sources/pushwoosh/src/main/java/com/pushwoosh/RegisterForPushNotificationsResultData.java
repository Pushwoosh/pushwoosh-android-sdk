package com.pushwoosh;

public class RegisterForPushNotificationsResultData {
    private final String token;
    private final boolean enabled;

    public RegisterForPushNotificationsResultData(String token, boolean enabled) {
        this.token = token;
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
