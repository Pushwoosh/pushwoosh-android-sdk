package com.pushwoosh.repository;

import android.net.Uri;

import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.notification.PushMessage;

import java.util.List;

public interface LockScreenMediaStorage {
    void cacheResource(PushMessage pushMessage);
    List<ResourceWrapper> getCachedResourcesList();
    void clearResources();

    void cacheRemoteUrl(Uri url);
    List<Uri> getCachedRemoteUrls();
    void clearRemoteUrls();
}
