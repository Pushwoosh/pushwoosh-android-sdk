package com.pushwoosh.demoapp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.pushwoosh.calls.PushwooshVoIPMessage;
import com.pushwoosh.calls.listener.CallEventListener;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;

public class DemoCallEventListener implements CallEventListener {
    private static final String TAG = "DemoCallEventListener";

    @Override
    public void onAnswer(PushwooshVoIPMessage voIPMessage, int videoState) {
        PWLog.info(TAG, "onAnswer: caller=" + voIPMessage.getCallerName() + ", video=" + voIPMessage.getHasVideo());

        Context context = AndroidPlatformModule.getApplicationContext();
        if (context != null) {
            Toast.makeText(context, "Call answered from " + voIPMessage.getCallerName(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onReject(PushwooshVoIPMessage voIPMessage) {
        PWLog.info(TAG, "onReject: caller=" + voIPMessage.getCallerName());

        Context context = AndroidPlatformModule.getApplicationContext();
        if (context != null) {
            Toast.makeText(context, "Call rejected from " + voIPMessage.getCallerName(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDisconnect(PushwooshVoIPMessage voIPMessage) {
        PWLog.info(TAG, "onDisconnect: caller=" + voIPMessage.getCallerName());

        Context context = AndroidPlatformModule.getApplicationContext();
        if (context != null) {
            Toast.makeText(context, "Call ended with " + voIPMessage.getCallerName(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreateIncomingConnection(Bundle payload) {
        PWLog.info(TAG, "onCreateIncomingConnection");
    }

    @Override
    public void onCallAdded(PushwooshVoIPMessage voIPMessage) {
        PWLog.info(TAG, "onCallAdded: caller=" + voIPMessage.getCallerName());
    }

    @Override
    public void onCallRemoved(PushwooshVoIPMessage voIPMessage) {
        PWLog.info(TAG, "onCallRemoved: caller=" + voIPMessage.getCallerName());
    }

    @Override
    public void onCallCancelled(PushwooshVoIPMessage voIPMessage) {
        PWLog.info(TAG, "onCallCancelled: caller=" + voIPMessage.getCallerName() + ", callId=" + voIPMessage.getCallId());

        Context context = AndroidPlatformModule.getApplicationContext();
        if (context != null) {
            String callerName = voIPMessage.getCallerName();
            if (callerName == null || callerName.isEmpty()) {
                callerName = "Unknown";
            }

            Intent intent = new Intent(context, CallCancelledActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("caller_name", callerName);
            intent.putExtra("call_id", voIPMessage.getCallId());
            intent.putExtra("has_video", voIPMessage.getHasVideo());

            try {
                context.startActivity(intent);
                PWLog.info(TAG, "Launched CallCancelledActivity");
            } catch (Exception e) {
                PWLog.error(TAG, "Failed to launch CallCancelledActivity", e);
                Toast.makeText(context, "Call cancelled from " + callerName, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onCallCancellationFailed(String callId, String reason) {
        PWLog.warn(TAG, "onCallCancellationFailed: callId=" + callId + ", reason=" + reason);

        Context context = AndroidPlatformModule.getApplicationContext();
        if (context != null) {
            // Toast must be shown on Main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "Cancellation failed: " + reason, Toast.LENGTH_SHORT).show();
            });
        }
    }
}
