package com.pushwoosh.internal.network;

import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.ServerCommunicationStartedEvent;
import com.pushwoosh.repository.RepositoryModule;

public class ServerCommunicationManager {
    public boolean isServerCommunicationAllowed() {
        return RepositoryModule.getNotificationPreferences().isServerCommunicationAllowed().get();
    }

    public void startServerCommunication() {
        if (isServerCommunicationAllowed()) {
            // already started
            return;
        }
        RepositoryModule.getNotificationPreferences().isServerCommunicationAllowed().set(true);
        EventBus.sendEvent(new ServerCommunicationStartedEvent());
    }

    public void stopServerCommunication() {
        RepositoryModule.getNotificationPreferences().isServerCommunicationAllowed().set(false);
    }
}
