package com.pushwoosh;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.preference.PreferenceArrayListValue;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.internal.preference.PreferenceValueFactory;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.PrefsUtils;
import com.pushwoosh.repository.DbLocalNotification;
import com.pushwoosh.repository.DbLocalNotificationHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MigrateLocalNotificationStorageTask extends AsyncTask<Void, Void, Void> {
    public static final String TAG = "MigrateLocalNotificationStorageTask";

    private static final String REQUEST_ID = "pushwoosh_local_push_request_id";
    private static final String SHOWED_NOTIFICATIONS = "pushwoosh_showed_local_notificaions";
    private static final String IDS_KEY = "pushwoosh_local_push_ids";
    private static final String BUNDLE_KEY = "pushwoosh_local_push_bundle_";
    private static final String TRIGGER_AT_MILLIS = "pushwoosh_local_push_trigger_at_millis_";

    private final SharedPreferences preferences;
    private final DbLocalNotificationHelper dbLocalNotificationHelper;
    private final PreferenceValueFactory preferenceValueFactory;
    private final PreferenceArrayListValue<String> showedNotifications;
    private final PreferenceIntValue requestId;

    public MigrateLocalNotificationStorageTask(@NonNull Context context) {
        this.preferences = AndroidPlatformModule.getPrefsProvider().provideDefault();
        this.dbLocalNotificationHelper = new DbLocalNotificationHelper(context);
        this.preferenceValueFactory = new PreferenceValueFactory();
        showedNotifications = preferenceValueFactory.buildPreferenceArrayListValue(preferences, SHOWED_NOTIFICATIONS, 10);
        requestId = preferenceValueFactory.buildPreferenceIntValue(preferences, REQUEST_ID, 0);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        migrateNextRequestId();
        executeMigrationInternal();
        return null;
    }

    private void migrateNextRequestId() {
        if (requestId.get() == 0) {
            return;
        }
        int nextId = requestId.get();
        dbLocalNotificationHelper.setNextRequestId(nextId);
        requestId.set(0);
    }

    private void executeMigrationInternal() {
        if (isOldStorageEmpty()) {
            return;
        }
        migrateLocalNotification();
        migrateLocalNotificationShown();
    }

    private boolean isOldStorageEmpty() {
        return getRequestIds().isEmpty() && showedNotifications.get().isEmpty();
    }

    private Set<String> getRequestIds() {
        if (preferences == null) {
            return Collections.emptySet();
        }
        try {
            return new HashSet<>(preferences.getStringSet(IDS_KEY, new HashSet<>()));
        } catch (ClassCastException e) {
            return new HashSet<>();
        }
    }

    private void migrateLocalNotification() {
        List<DbLocalNotification> dbLocalNotificationList = getLocalNotification();
        for (DbLocalNotification dbLocalNotification : dbLocalNotificationList) {
            dbLocalNotificationHelper.putDbLocalNotification(dbLocalNotification);
        }
    }

    private List<DbLocalNotification> getLocalNotification() {
        Set<String> ids = getRequestIds();
        List<DbLocalNotification> dbLocalNotifications = new ArrayList<>();

        for (String id : ids) {
            long triggerAtMillis = preferences == null ? 0 : preferences.getLong(TRIGGER_AT_MILLIS + id, 0);
            Bundle extra = PrefsUtils.getBundle(preferences, BUNDLE_KEY + id);

            int requestId = Integer.parseInt(id);
            DbLocalNotification dbLocalNotification = new DbLocalNotification(requestId, triggerAtMillis, extra);
            dbLocalNotifications.add(dbLocalNotification);
        }
        preferences
                .edit()
                .putStringSet(IDS_KEY, new HashSet<>())
                .apply();
        return dbLocalNotifications;
    }

    private void migrateLocalNotificationShown() {
        List<DbLocalNotification> dbLocalNotificationList = getLocalNotificationShown();
        for (DbLocalNotification dbLocalNotification : dbLocalNotificationList) {
            dbLocalNotificationHelper.addDbLocalNotificationShown(dbLocalNotification);
        }
    }

    private List<DbLocalNotification> getLocalNotificationShown() {
        List<DbLocalNotification> dbLocalNotificationList = new ArrayList<>();
        for (String jsonStr : showedNotifications.get()) {
            try {
                JSONObject json = new JSONObject(jsonStr);
                int requestId = json.getInt("requestId");
                int notificationId = json.getInt("notificationId");
                String notificationTag = json.getString("notificationTag");
                DbLocalNotification dbLocalNotification = new DbLocalNotification(requestId, notificationId, notificationTag);
                dbLocalNotificationList.add(dbLocalNotification);
            } catch (JSONException e) {
                PWLog.exception(e);
            }
        }
        showedNotifications.clear();
        return dbLocalNotificationList;
    }
}
