package com.pushwoosh.inapp.view;

import android.os.AsyncTask;
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
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;


public class DownloadHtmlTask extends AsyncTask<Void, Void, Result<HtmlData, ResourceParseException>> {
    private final Resource inApp;
    private final DownloadListener downloadListener;
    private final InAppRepository inAppRepository = InAppModule.getInAppRepository();

    public interface DownloadListener{
       void startLoading();

       void sendResult(Result<HtmlData, ResourceParseException> result);
    }

    public DownloadHtmlTask(Resource inApp, DownloadListener downloadListener) {
        this.inApp = inApp;
        this.downloadListener = downloadListener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        downloadListener.startLoading();
    }

    @Override
    protected Result<HtmlData, ResourceParseException> doInBackground(Void... params) {
        JSONObject tags = RepositoryModule.getNotificationPreferences().tags().get();
        try {
            Map<String, Object> rawTags = JsonUtils.jsonToMap(tags);
            addMissingTags(rawTags);
            inApp.setTags(rawTags);
        } catch (JSONException e) {
            PWLog.error("Failed parse tags", e);
        }

        return inAppRepository.mapToHtmlData(inApp);
    }

    /**
     * getTags returns everything except for "OS Version","Device Model", need to add manually
     */
    private void addMissingTags(@Nullable Map<String, Object> tags) {
        if (tags == null) {
            return;
        }

        if (RepositoryModule.getNotificationPreferences().isCollectingDeviceOsVersionAllowed().get()) {
            tags.put("OS Version", android.os.Build.VERSION.RELEASE);
        }
        if (RepositoryModule.getNotificationPreferences().isCollectingDeviceModelAllowed().get()) {
            tags.put("Device Model", DeviceUtils.getDeviceName());
        }

        InAppTagFormatModifier.convertGeoTags(tags);
    }

    @Override
    protected void onPostExecute(Result<HtmlData, ResourceParseException> result) {
        super.onPostExecute(result);
        if (!result.isSuccess()) {
            EventBus.sendEvent(new RichMediaErrorEvent(inApp, result.getException()));
        }
        downloadListener.sendResult(result);
    }
}
