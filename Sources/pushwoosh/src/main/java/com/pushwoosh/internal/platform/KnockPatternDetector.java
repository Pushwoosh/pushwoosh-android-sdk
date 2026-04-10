package com.pushwoosh.internal.platform;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.network.CreateTestDeviceRequest;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Detects a "knock pattern" — rapid foreground transitions — and registers the device
 * as a test device. When the app is brought to foreground {@link #REQUIRED_KNOCKS} times
 * within {@link #WINDOW_MS} milliseconds, the SDK enables NOISE log level, copies the
 * HWID to the clipboard, and sends a {@link CreateTestDeviceRequest}.
 */
public class KnockPatternDetector {
    private static final String TAG = "KnockPatternDetector";

    static final int REQUIRED_KNOCKS = 6;
    static final long WINDOW_MS = 30_000;
    private static final int MAX_DESCRIPTION_LENGTH = 64;

    interface Clock {
        long now();
    }

    private final long[] timestamps = new long[REQUIRED_KNOCKS];
    private final Clock clock;

    private int index = 0;
    private int count = 0;

    public KnockPatternDetector() {
        this(System::currentTimeMillis);
    }

    KnockPatternDetector(Clock clock) {
        this.clock = clock;
    }

    public void onForeground() {
        long now = clock.now();
        timestamps[index] = now;
        index = (index + 1) % REQUIRED_KNOCKS;
        count = Math.min(count + 1, REQUIRED_KNOCKS);

        if (count < REQUIRED_KNOCKS) {
            return;
        }

        // index now points to the oldest entry in the circular buffer
        long oldest = timestamps[index];
        if (now - oldest <= WINDOW_MS) {
            PWLog.debug(TAG, "Knock pattern detected!");
            reset();
            performKnockAction();
        }
    }

    private void reset() {
        count = 0;
        index = 0;
    }

    int getCount() {
        return count;
    }

    private void performKnockAction() {
        enableNoiseLogging();
        SdkStateProvider.getInstance().executeOrQueue(() -> {
            String hwid = RepositoryModule.getRegistrationPreferences().hwid().get();
            if (hwid == null || hwid.isEmpty()) {
                PWLog.warn(TAG, "HWID is not available");
                return;
            }

            Context context = AndroidPlatformModule.getApplicationContext();
            if (context == null) {
                PWLog.warn(TAG, "Context is null");
                return;
            }

            BackgroundExecutor.main(() -> {
                copyToClipboard(context, hwid);
                createTestDevice();
            });
        });
    }

    private void enableNoiseLogging() {
        String level = PWLog.Level.NOISE.name();
        RegistrationPrefs prefs = RepositoryModule.getRegistrationPreferences();
        if (prefs != null && prefs.logLevel() != null) {
            prefs.logLevel().set(level);
        }
        PWLog.updateLogLevel(level);
        PWLog.info(TAG, "Log level set to NOISE via knock pattern");
    }

    private void copyToClipboard(Context context, String hwid) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Pushwoosh HWID", hwid);
                clipboard.setPrimaryClip(clip);
            }
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to copy HWID to clipboard", e);
        }
    }

    private void createTestDevice() {
        NotificationPrefs prefs = RepositoryModule.getNotificationPreferences();
        String name = (prefs != null && prefs.isCollectingDeviceModelAllowed().get())
                ? DeviceUtils.getDeviceName()
                : "Android Device";
        CreateTestDeviceRequest request = new CreateTestDeviceRequest(name, buildDescription());
        RequestManager requestManager = NetworkModule.getRequestManager();
        if (requestManager == null) {
            PWLog.warn(TAG, "RequestManager is null");
            return;
        }
        requestManager.sendRequest(request, result -> {
            if (!result.isSuccess()) {
                PWLog.warn(TAG, "createTestDevice failed: " + result.getException());
            }
        });
    }

    private String buildDescription() {
        StringBuilder sb = new StringBuilder();

        AppInfoProvider appInfo = AndroidPlatformModule.getAppInfoProvider();
        if (appInfo != null && appInfo.getPackageName() != null) {
            sb.append(appInfo.getPackageName());
        }

        appendSeparator(sb);
        sb.append(new SimpleDateFormat("dd.MM.yy HH:mm", Locale.US).format(new Date()));

        if (sb.length() > MAX_DESCRIPTION_LENGTH) {
            return sb.substring(0, MAX_DESCRIPTION_LENGTH);
        }
        return sb.toString();
    }

    private void appendSeparator(StringBuilder sb) {
        if (sb.length() > 0) {
            sb.append(" | ");
        }
    }
}
