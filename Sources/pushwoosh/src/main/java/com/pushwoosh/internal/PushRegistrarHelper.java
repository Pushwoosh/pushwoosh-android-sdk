package com.pushwoosh.internal;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushwooshNotificationManager;

import java.lang.reflect.Method;

/**
 * PushRegistrarHelper class is used to keep backwards compatibility for Native applications and
 * Plugin-based applications because of HuaweiPushRegistrar being initialized automatically if
 * 'pushwoosh-huawei' module is added. In Native applications you can manually add or remove
 * 'pushwoosh-huawei' module, but not in Plugin-based.
 * Therefore there is a need for the method 'enableHuaweiPushNotifications()' to have a possibility to
 * activate HuaweiPushRegistrar manually before calling 'registerForPushNotifications()' as well
 * as to keep FcmRegistrar/AdmRegistrar as a default even with added 'pushwoosh-huawei' module.
 */

public class PushRegistrarHelper {
    private final static String TAG = "PushRegistrarHelper";
    private final static String AMAZON_INITIALIZER_CLASS_NAME = "com.pushwoosh.amazon.AmazonInitializer";
    private final static String AMAZON_PUSH_REGISTRAR_CLASS_NAME = "com.pushwoosh.amazon.internal.registrar.AdmRegistrar";
    private final static String FIREBASE_INITIALIZER_CLASS_NAME = "com.pushwoosh.firebase.FirebaseInitializer";
    private final static String FIREBASE_PUSH_REGISTRAR_CLASS_NAME = "com.pushwoosh.firebase.internal.registrar.FcmRegistrar";
    private final static String HUAWEI_INITIALIZER_CLASS_NAME = "com.pushwoosh.huawei.HuaweiInitializer";
    private final static String HUAWEI_PUSH_REGISTRAR_CLASS_NAME = "com.pushwoosh.huawei.internal.registrar.HuaweiPushRegistrar";
    private final PluginProvider pluginProvider;
    private final PushwooshNotificationManager pushwooshNotificationManager;

    public PushRegistrarHelper(PluginProvider pluginProvider,
                               PushwooshNotificationManager pushwooshNotificationManager) {
        this.pluginProvider = pluginProvider;
        this.pushwooshNotificationManager = pushwooshNotificationManager;
    }

    public boolean initDefaultPushRegistrarInPlugin() {
        if (pluginProvider == null) {
            return false;
        }

        // check we are in the plugin
        if (isNativePlugin(pluginProvider.getPluginType())) {
            return false;
        }

        PWLog.debug(TAG, "Initializing default PushRegistrar in a plugin");
        // check context not null
        Context context = AndroidPlatformModule.getApplicationContext();
        if (context == null) {
            return false;
        }

        // The last initialized PushRegistrar should be Huawei because of initOrder=51 for the
        // Content Provider. But we don't want it to be initialized by default in plugins.

        // The second is Amazon with initOrder=52:
        // init Amazon if added and return;
        if (isAmazonModuleAdded() && initAmazon(context)) {
            return true;
        }

        // The third is Firebase with with initOrder=54
        // init Firebase if added and return;
        if (isFirebaseModuleAdded() && initFirebase(context)) {
            return true;
        }

        // init another push providers here
        return false;
    }

    public void enableHuaweiPushNotifications() {
        if (pluginProvider == null) {
            return;
        }

        // check it is a plugin
        if (isNativePlugin(pluginProvider.getPluginType())) {
            return;
        }

        // check context not null
        Context context = AndroidPlatformModule.getApplicationContext();
        if (context == null) {
            return;
        }

        // check pushwoosh-huawei module has been added
        if (!isHuaweiModuleAdded()) {
            return;
        }

        // and then initialize it
        initHuawei(context);
    }

    private boolean isNativePlugin(String pluginType) {
        return TextUtils.equals(pluginType, NativePluginProvider.NATIVE_PLUGIN_TYPE);
    }

    private boolean isAmazonModuleAdded() {
        return isModuleAdded(AMAZON_INITIALIZER_CLASS_NAME);
    }

    private boolean isFirebaseModuleAdded() {
        return isModuleAdded(FIREBASE_INITIALIZER_CLASS_NAME);
    }

    private boolean isHuaweiModuleAdded() {
        return isModuleAdded(HUAWEI_INITIALIZER_CLASS_NAME);
    }

    private boolean isModuleAdded(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    private boolean initAmazon(Context context) {
        return initRegistrarClass(AMAZON_INITIALIZER_CLASS_NAME, AMAZON_PUSH_REGISTRAR_CLASS_NAME, context);
    }

    private boolean initFirebase(Context context) {
        return initRegistrarClass(FIREBASE_INITIALIZER_CLASS_NAME, FIREBASE_PUSH_REGISTRAR_CLASS_NAME, context);
    }

    private boolean initHuawei(Context context) {
        return initRegistrarClass(HUAWEI_INITIALIZER_CLASS_NAME, HUAWEI_PUSH_REGISTRAR_CLASS_NAME, context);
    }

    @SuppressWarnings("unchecked")
    private boolean initRegistrarClass(String initializerClassName, String pushRegistrarClassName, Context context) {
        try {
            Class initializerClass = Class.forName(initializerClassName);
            Method initMethod = initializerClass.getMethod("init", Context.class);
            initMethod.invoke(null, context);

            Class pushRegistrarClass = Class.forName(pushRegistrarClassName);
            PushRegistrar pushRegistrar = DeviceSpecificProvider.getInstance().pushRegistrar();
            // check we got the correct PushRegistrar class
            if (pushRegistrarClass.isInstance(pushRegistrar)) {
                setPushRegistrar(pushRegistrar);
                return true;
            }
        } catch (Throwable e) {
            PWLog.error(TAG, "Unexpected error occurred calling 'initRegistrarClass' method");
            PWLog.error(TAG, e.getMessage());
        }
        return false;
    }

    private void setPushRegistrar(PushRegistrar pushRegistrar) {
        if (pushwooshNotificationManager != null) {
            pushwooshNotificationManager.setPushRegistrar(pushRegistrar);
        }
    }
}
