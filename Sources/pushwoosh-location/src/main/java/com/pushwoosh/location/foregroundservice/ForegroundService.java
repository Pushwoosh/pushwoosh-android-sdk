package com.pushwoosh.location.foregroundservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

public class ForegroundService extends Service {
    private final String TAG = ForegroundService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            startForeground();
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to start foreground service", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startForeground() {
        ForegroundServiceHelper foregroundServiceHelper =
                ForegroundServiceHelperRepository.getForegroundServiceHelper();

        if (foregroundServiceHelper != null) {
            foregroundServiceHelper.startForeground(this);
            PWLog.info(TAG, "startForeground success");
        } else {
            PWLog.error(TAG, "startForeground is failed: foregroundServiceHelper is null");
        }
    }
}