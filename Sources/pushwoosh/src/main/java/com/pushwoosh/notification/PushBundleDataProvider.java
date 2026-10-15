/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.notification;

import static com.pushwoosh.repository.NotificationPrefs.DEFAULT_NOTIFICATION_GROUP;

import android.app.Notification;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * class for transforming data from push payload
 */
@SuppressWarnings("WeakerAccess")
public final class PushBundleDataProvider {

    private static final String TAG = "PushBundleDataProvider";

    // Not in the "pw_" namespace: that one belongs to the server, this key is written by the SDK.
    private static final String KEY_TAPPED_BUTTON_ID = "tapped_button_id";

    @SuppressWarnings("deprecation")
    private static final int NOTIFICATION_PRIORITY_DEFAULT =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ? Notification.PRIORITY_DEFAULT : 0;

    private static final int NOTIFICATION_VISIBILITY_PUBLIC =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? Notification.VISIBILITY_PUBLIC : 1;

    public static boolean isSilent(Bundle pushBundle) {
        return getStringBoolean(pushBundle, "silent") || getStringBoolean(pushBundle, "pw_silent");
    }

    public static boolean isUserPush(Bundle pushBundle) {
        return isPwMessageWithKey(pushBundle, 1);
    }

    public static boolean isSystemPush(Bundle pushBundle) {
        int pwMsg = getStringInteger(pushBundle, "pw_system_push", 0);
        return pwMsg == 1;
    }

    private static boolean isPwMessageWithKey(final Bundle pushBundle, final int key) {
        int pwMsg = getStringInteger(pushBundle, "pw_msg", 0);
        return pwMsg == key;
    }

    @Nullable public static String getInternalCommand(Bundle bundle) {
        return bundle.getString("pw_command");
    }

    @Nullable public static String getPushHash(Bundle bundle) {
        return bundle.getString("p");
    }

    @Nullable public static String getPushMetadata(Bundle bundle) {
        return bundle.getString("md");
    }

    public static boolean isLocal(Bundle bundle) {
        return bundle.getBoolean("local", false);
    }

    @NonNull public static Integer getIconBackgroundColor(Bundle bundle) {
        String iconColor = bundle.getString("ibc");
        if (iconColor != null) {
            return Color.parseColor(iconColor);
        }

        return RepositoryModule.getNotificationPreferences()
                .iconBackgroundColor()
                .get();
    }

    @Nullable public static Integer getLedColor(Bundle extras) {
        String ledColor = extras.getString("led");
        if (ledColor != null) {
            return GeneralUtils.parseColor(ledColor);
        }

        return null;
    }

    public static boolean getVibration(Bundle extras) {
        String vibration = extras.getString("vib");
        return !TextUtils.isEmpty(vibration) && vibration.equals("1");
    }

    @Nullable public static String getSound(Bundle extras) {
        return extras.getString("s");
    }

    @Nullable public static String getMessage(Bundle bundle) {
        return bundle.getString("title");
    }

    public static String getHeader(Bundle extras) {
        String result = (String) extras.get("header");
        if (result == null) {
            CharSequence appName = AndroidPlatformModule.getAppInfoProvider().getApplicationLabel();
            if (null == appName) {
                appName = "";
            }
            result = appName.toString();
        }

        return result;
    }

    @SuppressWarnings("deprecation")
    public static int getPriority(Bundle extras) {
        int result = getStringInteger(extras, "pri", NOTIFICATION_PRIORITY_DEFAULT);
        if (Math.abs(result) > 2) {
            PWLog.warn("Unsupported priority: " + result + ", setting to default: 0");
            result = NOTIFICATION_PRIORITY_DEFAULT;
        }

        return result;
    }

    public static int getVisibility(Bundle extras) {
        int result = getStringInteger(extras, "visibility", NOTIFICATION_VISIBILITY_PUBLIC);
        if (Math.abs(result) > 1) {
            PWLog.warn("Unsupported visibility: " + result + ", setting to default: 1");
            result = NOTIFICATION_VISIBILITY_PUBLIC;
        }

        return result;
    }

    public static int getBadges(Bundle extras) {
        return getStringInteger(extras, "pw_badges", -1);
    }

    public static boolean isBadgesAdditive(Bundle extras) {
        String stringValue = extras.getString("pw_badges");
        return !TextUtils.isEmpty(stringValue) && (stringValue.charAt(0) == '-' || stringValue.charAt(0) == '+');
    }

    @NonNull public static Collection<Action> getActions(Bundle extras) {
        Collection<Action> actions = new ArrayList<>();

        // A push that carries the new contract never renders the legacy one, even if parsing failed.
        if (extras.containsKey(ActionButton.KEY_PW_BUTTONS)) {
            return actions;
        }

        try {
            String actionsString = extras.getString("pw_actions");
            if (actionsString != null) {
                JSONArray jsonArray = new JSONArray(actionsString);
                for (int i = 0; i < jsonArray.length(); ++i) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    actions.add(new Action(json));
                }
            }
        } catch (JSONException e) {
            PWLog.exception(e);
        }

        return actions;
    }

    @NonNull public static List<ActionButton> getButtons(Bundle extras) {
        String rawButtons = extras.getString(ActionButton.KEY_PW_BUTTONS);
        if (rawButtons == null) {
            return Collections.emptyList();
        }

        List<ActionButton> buttons = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        try {
            JSONArray jsonArray = new JSONArray(rawButtons);
            for (int i = 0; i < jsonArray.length() && buttons.size() < ActionButton.MAX_BUTTONS; ++i) {
                JSONObject json = jsonArray.optJSONObject(i);
                if (json == null) {
                    PWLog.warn(TAG, "action button #" + i + " is not an object, skipping it");
                    continue;
                }
                ActionButton button = ActionButton.parse(json);
                if (button == null) {
                    PWLog.warn(TAG, "action button #" + i + " is incomplete, skipping it");
                    continue;
                }
                if (!seenIds.add(button.getId())) {
                    PWLog.warn(TAG, "action button id " + button.getId() + " is duplicated, skipping it");
                    continue;
                }
                buttons.add(button);
            }
        } catch (JSONException e) {
            PWLog.warn(TAG, "pw_buttons is not a valid JSON array, showing no action buttons");
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(buttons);
    }

    @Nullable public static String getTappedButtonId(Bundle bundle) {
        return bundle.getString(KEY_TAPPED_BUTTON_ID);
    }

    public static void setTappedButtonId(@NonNull Bundle bundle, @NonNull String buttonId) {
        bundle.putString(KEY_TAPPED_BUTTON_ID, buttonId);
    }

    @Nullable public static String getBigPicture(Bundle extras) {
        return extras.getString("b");
    }

    @Nullable public static String getLargeIcon(Bundle extras) {
        return extras.getString("ci");
    }

    public static int getSmallIcon(Bundle extras) {
        String smallIconPath = extras.getString("i");
        return NotificationUtils.tryToGetIconFormStringOrGetFromApplication(smallIconPath);
    }

    public static int getLedOnMs(Bundle extras) {
        return getStringInteger(extras, "led_on_ms", 100);
    }

    public static int getLedOffMs(Bundle extras) {
        return getStringInteger(extras, "led_off_ms", 1000);
    }

    @Nullable public static String getMessageTag(Bundle extras) {
        return extras.getString("pw_msg_tag");
    }

    public static boolean isLockScreen(Bundle bundle) {
        return getStringBoolean(bundle, "pw_lockscreen");
    }

    @Nullable public static String getCustomData(final Bundle extras) {
        return extras.getString("u");
    }

    @Nullable public static String getApplicationBundles(final Bundle pushBundle) {
        return pushBundle.getString("packs");
    }

    @Nullable public static String getValue(final Bundle pushBundle) {
        return pushBundle.getString("value");
    }

    @Nullable public static String getRichMedia(final Bundle pushBundle) {
        return pushBundle.getString("rm");
    }

    @Nullable public static String getLink(final Bundle pushBundle) {
        return pushBundle.getString("l");
    }

    @Nullable public static String getNotificationChannel(final Bundle pushBundle) {
        return pushBundle.getString("pw_channel");
    }

    public static void setForeground(@NonNull Bundle pushBundle, final boolean foreground) {
        pushBundle.putBoolean("foreground", foreground);
        pushBundle.putBoolean("onStart", !foreground);
    }

    @SuppressWarnings("SameParameterValue")
    public static void setLocal(@NonNull Bundle pushBundle, final boolean isLocal) {
        pushBundle.putString("pw_msg", "1");
        pushBundle.putBoolean("local", isLocal);
    }

    public static void setTag(final Bundle extras, final String tag) {
        extras.putString("pw_msg_tag", tag);
    }

    public static void setMessage(final Bundle extras, final String message) {
        extras.putString("title", message);
    }

    public static void setLink(final Bundle extras, final String url) {
        extras.putString("l", url);
    }

    public static void setBanner(final Bundle extras, final String url) {
        extras.putString("b", url);
    }

    public static void setSmallIcon(final Bundle extras, final String url) {
        extras.putString("i", url);
    }

    public static void setLargeIcon(final Bundle extras, final String url) {
        extras.putString("ci", url);
    }

    @NonNull public static JSONObject asJson(final Bundle pushBundle) {
        return JsonUtils.bundleToJsonWithUserData(pushBundle);
    }

    private static int getStringInteger(Bundle extras, String key, int defaultValue) {
        String stringValue = extras.getString(key);
        if (!TextUtils.isEmpty(stringValue)) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
                PWLog.error("ERROR! Incorrect format for key [ " + key + " ]: " + stringValue);
                return defaultValue;
            }
        }

        if (stringValue != null && stringValue.isEmpty()) {
            return defaultValue;
        }

        return extras.getInt(key, defaultValue);
    }

    private static boolean getStringBoolean(Bundle extras, String key) {
        String stringBoolean = extras.getString(key);
        if (TextUtils.equals(stringBoolean, "true")) {
            return true;
        }

        try {
            int value = Integer.parseInt(stringBoolean);
            if (value > 0) {
                return true;
            }
        } catch (Exception e) {
            // fall through
        }

        return false;
    }

    private PushBundleDataProvider() {
        /*do nothing*/
    }

    public static long getSentTime(Bundle pushBundle) {
        return pushBundle.getLong("google.sent_time", System.currentTimeMillis());
    }

    public static String getGroupId(Bundle pushBundle) {
        return pushBundle.getString("group_id", DEFAULT_NOTIFICATION_GROUP);
    }
}
