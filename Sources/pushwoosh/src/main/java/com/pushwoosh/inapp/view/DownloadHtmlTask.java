package com.pushwoosh.inapp.view;

import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.event.RichMediaErrorEvent;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.mapper.InAppTags;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

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
            inApp.setTags(InAppTags.collect(notificationPrefs));

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
}
