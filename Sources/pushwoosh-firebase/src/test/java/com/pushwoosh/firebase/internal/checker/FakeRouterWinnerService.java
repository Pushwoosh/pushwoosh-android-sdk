package com.pushwoosh.firebase.internal.checker;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.RemoteMessage;
import com.pushwoosh.firebase.PushwooshFcmRouterService;

public class FakeRouterWinnerService extends PushwooshFcmRouterService {
    @Override
    protected void onAppMessageReceived(@NonNull RemoteMessage message) {}
}
