package com.pushwoosh.test.manifest;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.pushwoosh.calls.PushwooshVoIPMessage;
import com.pushwoosh.calls.listener.CallEventListener;

public class FakeNoCtorCallEventListener implements CallEventListener {
    @SuppressWarnings("unused")
    public FakeNoCtorCallEventListener(int unused) {}

    @Override
    public void onAnswer(PushwooshVoIPMessage voIPMessage, int videoState) {}

    @Override
    public void onReject(PushwooshVoIPMessage voIPMessage) {}

    @Override
    public void onDisconnect(PushwooshVoIPMessage voIPMessage) {}

    @Override
    public void onCreateIncomingConnection(@Nullable Bundle payload) {}

    @Override
    public void onCallAdded(PushwooshVoIPMessage voIPMessage) {}

    @Override
    public void onCallRemoved(PushwooshVoIPMessage voIPMessage) {}

    @Override
    public void onCallCancelled(PushwooshVoIPMessage voIPMessage) {}

    @Override
    public void onCallCancellationFailed(@Nullable String callId, String reason) {}
}
