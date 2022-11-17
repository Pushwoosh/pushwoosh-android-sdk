package com.pushwoosh.repository;

import android.os.HandlerThread;
import android.text.TextUtils;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.InitHwidEvent;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.utils.AppVersionProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.notification.RegistrationCallbackHolder;
import com.pushwoosh.tags.TagsBundle;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;

public class HWIDMigration {
    private final static String TAG = HWIDMigration.class.getSimpleName();

    private RequestManager requestManager;
    private SendTagsProcessor sendTagsProcessor;
    private PreferenceBooleanValue tagsMigrationPrefs;
    private AppVersionProvider appVersionProvider;
    private PushwooshNotificationManager notificationManager;
    private RegistrationPrefs registrationPrefs;
    private DeviceRegistrar deviceRegistrar;

    private boolean isTagsFromOldHWIDLoaded;
    private boolean isHWIDInitialized;

    private TagsBundle tagsFromOldHWID;
    private String oldHWID;
    private String newHWID;

    public HWIDMigration(RequestManager requestManager,
                         SendTagsProcessor sendTagsProcessor,
                         PreferenceBooleanValue tagsMigrationPrefs,
                         AppVersionProvider appVersionProvider,
                         PushwooshNotificationManager notificationManager,
                         RegistrationPrefs registrationPrefs,
                         DeviceRegistrar deviceRegistrar) {
        this.requestManager = requestManager;
        this.sendTagsProcessor = sendTagsProcessor;
        this.tagsMigrationPrefs = tagsMigrationPrefs;
        this.appVersionProvider = appVersionProvider;
        this.notificationManager = notificationManager;
        this.registrationPrefs = registrationPrefs;
        this.deviceRegistrar = deviceRegistrar;
    }

    public void prepare() {
        PWLog.noise(TAG, "prepare migration");
        String oldHWID = DeviceUtils.getDeviceUUIDOld();
        if (appVersionProvider.isFirstLaunch() || TextUtils.isEmpty(oldHWID)) {
            tagsMigrationPrefs.set(true);
        }
        if (!tagsMigrationPrefs.get()) {
            loadTagsFromOldHWID();
        }
    }

    public void executeMigration(String newHwid, String oldHwid) {
        if ((tagsMigrationPrefs.get() && newHwid.equals(oldHwid)) || oldHwid.isEmpty()) {
            PWLog.noise(TAG, "migration tags already done");
            return;
        }

        this.oldHWID = oldHwid;
        this.newHWID = newHwid;

        if (!tagsMigrationPrefs.get()) {
            synchronized (this) {
                isHWIDInitialized = true;
                if (isTagsFromOldHWIDLoaded) {
                    continueMigration();
                }
            }
        } else {
            synchronized (this) {
                isHWIDInitialized = true;
            }
            loadTagsFromOldHWID();
        }
    }

    protected void continueMigration() {
        if (isRegisterForPushNotification()) {
            if (!newHWID.equals(oldHWID)) { //first launch after update
                registrationPrefs.lastPushRegistration().set(0);
            }
            if (registrationPrefs.lastPushRegistration().get() == 0) {
                RegistrationCallbackHolder.setCallback(result -> saveTags(), false);
                deviceRegistrar.updateRegistration();
            } else {
                saveTags();
            }
        } else {
            saveTags();
        }
    }

    private void loadTagsFromOldHWID() {
        String hwid = tagsMigrationPrefs.get() ? oldHWID : DeviceUtils.getDeviceUUIDOld();
        GetTagsRequestWithOldHWID getTagsRequestWithOldHWID = new GetTagsRequestWithOldHWID(hwid);
        requestManager.sendRequest(getTagsRequestWithOldHWID, result -> {
            if (result.isSuccess()) {
                if (result.getData().getMap().size() > 0) {
                    tagsFromOldHWID = result.getData();
                } else {
                    tagsMigrationPrefs.set(true);
                    PWLog.debug(TAG, "getTags empty");
                }
                synchronized (this) {
                    isTagsFromOldHWIDLoaded = true;
                    if (isHWIDInitialized) {
                        continueMigration();
                    }
                }
            } else {
                //handle if device was not found on pushwoosh
                String error = result.getException().getMessage();
                try {
                    JSONObject jsonObject = new JSONObject(error);
                    int statusCode = jsonObject.getInt("status_code");
                    String statusMessage = jsonObject.getString("status_message");
                    if (statusCode == 210 && statusMessage.equals("Device not found")) {
                        tagsMigrationPrefs.set(true);
                        PWLog.debug(TAG, "getTags returned \"Device not found\"");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean isRegisterForPushNotification() {
        String pushToken = notificationManager.getPushToken();
        return pushToken != null && !pushToken.isEmpty() && registrationPrefs.isRegisteredForPush().get();
    }

    private void saveTags() {
        if (tagsFromOldHWID != null) {
            JSONObject tags = tagsFromOldHWID.toJson();
            PWLog.noise(TAG, "data for migration:" + tags.toString());
            sendTagsProcessor.sendTags(tags, this::processResult);
        }
    }

    private void processResult(Result<Void, PushwooshException> resultSetTag) {
        if (resultSetTag.isSuccess()) {
            tagsMigrationPrefs.set(true);
            PWLog.noise(TAG, "migration success");
        }
    }
}
