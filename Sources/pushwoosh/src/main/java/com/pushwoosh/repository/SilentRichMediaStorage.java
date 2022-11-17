package com.pushwoosh.repository;

import androidx.annotation.Nullable;

import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.notification.PushMessage;

public interface SilentRichMediaStorage {
    void replaceResource(PushMessage pushMessage);
    @Nullable ResourceWrapper getResourceWrapper();
}
