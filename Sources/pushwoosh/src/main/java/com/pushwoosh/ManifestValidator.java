/*
 *
 * Copyright (c) 2026. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.PluginProvider;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.NotificationFactory;
import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.SummaryNotificationFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

class ManifestValidator {
    private static final String TAG = "ManifestValidator";

    static final String NO_ATTACHED_PUSH_NOTIFICATIONS_PROVIDERS_FOUND_MESSAGE =
            "No attached push notifications providers have been found.\n"
                    + "This error can be seen when you use 'pushwoosh-huawei' module\n"
                    + "not on Huawei device or you have not added any module attaching\n"
                    + "push notifications provider.\n"
                    + "Pushwoosh supports Firebase, Amazon, Huawei push notification providers.\n"
                    + "See the integration guide https://docs.pushwoosh.com/platform-docs/pushwoosh-sdk/android-push-notifications";

    private static final AtomicBoolean validated = new AtomicBoolean(false);

    static void scheduleValidation() {
        scheduleValidation(BackgroundExecutor::executeOnPool);
    }

    @VisibleForTesting
    static boolean scheduleValidation(Executor executor) {
        if (!validated.compareAndSet(false, true)) {
            return false;
        }
        executor.execute(ManifestValidator::runValidationSafely);
        return true;
    }

    private static void runValidationSafely() {
        try {
            validate();
        } catch (Throwable t) {
            PWLog.exception(t);
        }
    }

    @VisibleForTesting
    static void validate() {
        AppInfoProvider provider = AndroidPlatformModule.getAppInfoProvider();
        if (provider == null) {
            validateProviderMatch();
            return;
        }
        ApplicationInfo info = provider.getApplicationInfo();
        if (info == null || info.metaData == null) {
            validateProviderMatch();
            return;
        }
        Bundle metaData = info.metaData;
        validateNotificationServiceExtension(metaData);
        validateNotificationFactory(metaData);
        validateSummaryNotificationFactory(metaData);
        validateOptionalModuleClass(
                metaData,
                "com.pushwoosh.CALL_EVENT_LISTENER",
                "com.pushwoosh.calls.listener.CallEventListener",
                "pushwoosh-calls",
                "CallEventListener");
        validateOptionalModuleClass(
                metaData,
                "com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER",
                "com.pushwoosh.liveupdates.LiveUpdateProgressStyleProvider",
                "pushwoosh-liveupdates",
                "LiveUpdateProgressStyleProvider");
        validatePluginProvider(metaData);
        validateCustomPlugins(metaData);
        validateProviderMatch();
    }

    private static void validateProviderMatch() {
        if (DeviceSpecificProvider.getInstance() == null) {
            PWLog.warn(TAG, NO_ATTACHED_PUSH_NOTIFICATIONS_PROVIDERS_FOUND_MESSAGE);
        }
    }

    private static void validateCustomPlugins(Bundle metaData) {
        for (String key : metaData.keySet()) {
            if (!key.startsWith("com.pushwoosh.plugin.")) {
                continue;
            }
            Class<?> cls = lookupClass(metaData, key);
            if (cls == null) {
                continue;
            }
            checkBaseAndCtor(key, cls, Plugin.class, "Plugin", true);
        }
    }

    private static void validatePluginProvider(Bundle metaData) {
        String key = "com.pushwoosh.internal.plugin_provider";
        Class<?> cls = lookupClass(metaData, key);
        if (cls == null) {
            return;
        }
        checkBaseAndCtor(key, cls, PluginProvider.class, "PluginProvider", true);
    }

    /**
     * Validates a class declared for an optional add-on module (e.g. pushwoosh-calls,
     * pushwoosh-liveupdates). The module's base type may be absent from the classpath — that means
     * the integrator declared the meta-data without adding the matching dependency, reported with a
     * specific hint. When the base is present, defers to {@link #checkBaseAndCtor}; these bases are
     * always interfaces.
     */
    @VisibleForTesting
    static void validateOptionalModuleClass(
            Bundle metaData, String key, String baseClassName, String moduleName, String baseSimpleName) {
        Class<?> cls = lookupClass(metaData, key);
        if (cls == null) {
            return;
        }
        Class<?> expectedBase;
        try {
            expectedBase = Class.forName(baseClassName);
        } catch (ClassNotFoundException e) {
            PWLog.warn(
                    TAG,
                    "'" + key + "' is declared but the " + moduleName + " module is not on the classpath. "
                            + "Either add the " + moduleName + " dependency or remove the <meta-data> entry.");
            return;
        }
        checkBaseAndCtor(key, cls, expectedBase, baseSimpleName, true);
    }

    private static void validateSummaryNotificationFactory(Bundle metaData) {
        String key = "com.pushwoosh.summary_notification_factory";
        Class<?> cls = lookupClass(metaData, key);
        if (cls == null) {
            return;
        }
        checkBaseAndCtor(key, cls, SummaryNotificationFactory.class, "SummaryNotificationFactory", false);
    }

    private static void validateNotificationFactory(Bundle metaData) {
        String key = "com.pushwoosh.notification_factory";
        Class<?> cls = lookupClass(metaData, key);
        if (cls == null) {
            return;
        }
        checkBaseAndCtor(key, cls, NotificationFactory.class, "NotificationFactory", false);
    }

    private static void validateNotificationServiceExtension(Bundle metaData) {
        String key = "com.pushwoosh.notification_service_extension";
        Class<?> cls = lookupClass(metaData, key);
        if (cls == null) {
            return;
        }
        checkBaseAndCtor(key, cls, NotificationServiceExtension.class, "NotificationServiceExtension", false);
    }

    /**
     * Looks up the class declared under {@code key} in the manifest's metaData, applying the same
     * leading-dot resolution as {@link AndroidManifestConfig}. Returns null when the key is absent
     * or the class is not on the classpath (the latter is also reported as a warn).
     */
    @Nullable private static Class<?> lookupClass(Bundle metaData, String key) {
        String raw = metaData.getString(key);
        String className = AndroidManifestConfig.resolveClassName(raw);
        if (className == null) {
            return null;
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            PWLog.warn(
                    TAG,
                    key + " points to '" + className + "', but this class was not found on the classpath. "
                            + "Verify the class name in AndroidManifest.xml and that the class is included in the build.");
            return null;
        }
    }

    /** Helper used by each rule: given a class already known to exist, verify base and ctor. */
    private static void checkBaseAndCtor(
            String key, Class<?> cls, Class<?> expectedBase, String baseSimpleName, boolean isInterface) {
        if (!expectedBase.isAssignableFrom(cls)) {
            String verb = isInterface ? "implement" : "extend";
            String syntax = isInterface ? "implements " : "extends ";
            PWLog.warn(
                    TAG,
                    "Class '" + cls.getName() + "' declared by '" + key + "' does not " + verb + " "
                            + expectedBase.getName() + ". Make it: public class " + cls.getSimpleName() + " "
                            + syntax + baseSimpleName + " { ... }");
            return;
        }
        try {
            cls.getConstructor();
        } catch (NoSuchMethodException e) {
            PWLog.warn(
                    TAG,
                    "Class '" + cls.getName() + "' declared by '" + key
                            + "' has no public no-argument constructor. "
                            + "Pushwoosh SDK instantiates it via reflection. Add: public "
                            + cls.getSimpleName() + "() { ... }");
        }
    }

    @VisibleForTesting
    static void resetForTesting() {
        validated.set(false);
    }
}
