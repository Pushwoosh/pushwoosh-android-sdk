package com.pushwoosh.inapp.view;

import androidx.annotation.Nullable;

import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.event.RichMediaErrorEvent;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.mapper.InAppTagFormatModifier;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class DownloadHtmlTask {
    private final Resource inApp;
    private final DownloadListener downloadListener;
    private final InAppRepository inAppRepository;
    private final NotificationPrefs notificationPrefs;
    private volatile boolean cancelled = false;

    public interface DownloadListener {
        void startLoading();

        void sendResult(Result<HtmlData, ResourceParseException> result);
    }

    public DownloadHtmlTask(Resource inApp, DownloadListener downloadListener) {
        this.inApp = inApp;
        this.downloadListener = downloadListener;
        this.inAppRepository = InAppModule.getInAppRepository();
        this.notificationPrefs = RepositoryModule.getNotificationPreferences();
    }

    public void cancel(boolean mayInterruptIfRunning) {
        cancelled = true;
    }

    public void execute() {
        PWLog.noise("DownloadHtmlTask", "execute()");
        downloadListener.startLoading();

        BackgroundExecutor.executeOnPool(() -> {
            JSONObject tags = notificationPrefs.tags().get();
            try {
                Map<String, Object> rawTags = JsonUtils.jsonToMap(tags);
                addMissingTags(rawTags);
                inApp.setTags(rawTags);
            } catch (JSONException e) {
                PWLog.error("DownloadHtmlTask", "Failed parse tags", e);
            }

            Result<HtmlData, ResourceParseException> result;
            if (inAppRepository != null) {
                result = inAppRepository.mapToHtmlData(inApp);
            } else {
                result = Result.fromException(new ResourceParseException("InAppRepository is not initialized"));
            }

            if (!cancelled) {
                BackgroundExecutor.main(() -> {
                    if (cancelled) {
                        return;
                    }
                    if (!result.isSuccess()) {
                        EventBus.sendEvent(new RichMediaErrorEvent(inApp, result.getException()));
                    }
                    downloadListener.sendResult(result);
                });
            }
        });
    }

    /**
     * getTags returns everything except for "OS Version","Device Model", need to add manually
     */
    private void addMissingTags(@Nullable Map<String, Object> tags) {
        if (tags == null) {
            return;
        }

        if (notificationPrefs.isCollectingDeviceOsVersionAllowed().get()) {
            tags.put("OS Version", android.os.Build.VERSION.RELEASE);
        }
        if (notificationPrefs.isCollectingDeviceModelAllowed().get()) {
            tags.put("Device Model", DeviceUtils.getDeviceName());
        }

        InAppTagFormatModifier.convertGeoTags(tags);
    }
}
