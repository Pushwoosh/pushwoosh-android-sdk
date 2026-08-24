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

//
// GeneralUtils.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed

package com.pushwoosh.internal.platform.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.TypedValue;

import androidx.annotation.Nullable;

import com.pushwoosh.BuildConfig;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GeneralUtils {
    private static final String TAG = "GeneralUtils";

    public static final String SDK_VERSION = BuildConfig.VERSION_NAME;

    private static final String[] SUPPORTED_AUDIO_FORMATS = {
        ".mp3", ".3gp", ".mp4", ".m4a", ".aac", ".flac", ".ogg", ".wav"
    };

    public static void checkNotNullOrEmpty(String reference, String name) {
        checkNotNull(reference, name);
        if (reference.length() == 0) {
            throw new IllegalArgumentException(
                    String.format("Please set the %1$s constant and recompile the app.", name));
        }
    }

    public static String md5(String s) {
        if (s == null) {
            return "";
        }

        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (final byte aMessageDigest : messageDigest) {
                hexString.append(String.format("%02x", aMessageDigest));
            }
            return hexString.toString();

        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    public static void checkNotNull(Object reference, String name) {
        if (reference == null) {
            throw new IllegalArgumentException(
                    String.format("Please set the %1$s constant and recompile the app.", name));
        }
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                AndroidPlatformModule.getManagerProvider().getConnectivityManager();
        NetworkInfo activeNetworkInfo = connectivityManager == null ? null : connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isAvailable() && activeNetworkInfo.isConnected();
    }

    public static boolean isStoreApp() {
        String name = AndroidPlatformModule.getAppInfoProvider().getInstallerPackageName();
        return !TextUtils.isEmpty(name);
    }

    public static boolean checkStickyBroadcastPermissions(Context context) {
        //noinspection ConstantConditions
        try {
            return context.getPackageManager()
                            .checkPermission("android.permission.BROADCAST_STICKY", context.getPackageName())
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            PWLog.error("error in checking broadcast_sticky permission", e);
        }
        return false;
    }

    public static ArrayList<String> getRawResourses() {
        ArrayList<String> files = new ArrayList<>();

        try {
            collectRawResourceSounds(files);
        } catch (Exception e) {
            // Enumeration feeds registerDevice: anything thrown here must stay inside, or registration itself fails.
            PWLog.noise(TAG, "Failed to enumerate raw resources for sound files", e);
        }

        // iterate the files from file:///android_asset/www/res (for Phonegap)
        try {
            final AssetManager assetManager =
                    AndroidPlatformModule.getManagerProvider().getAssets();
            if (assetManager == null) {
                return files;
            }

            String[] list = assetManager.list("www/res");
            for (String name : list) {
                String[] assets = assetManager.list("www/res/" + name);
                if (assets.length != 0) {
                    continue; // directory
                }

                if (isSound(name)) {
                    files.add(name);
                }
            }
        } catch (IOException e) {
            PWLog.exception(e);
        } catch (Exception e) {
            PWLog.noise(TAG, "Failed to enumerate www/res sound files", e);
        }

        return files;
    }

    private static void collectRawResourceSounds(List<String> files) {
        Set<String> candidates = rawClassPackages();
        Class<?> rawClass = findRawResourceClass(candidates);
        if (rawClass == null) {
            PWLog.debug(TAG, "No R$raw class found in candidate packages " + candidates + ", reporting no sounds");
            return;
        }

        collectSoundNames(rawClass, files);
    }

    @Nullable private static Class<?> findRawResourceClass(Set<String> candidates) {
        for (String pkg : candidates) {
            try {
                return Class.forName(pkg + ".R$raw");
            } catch (ClassNotFoundException e) {
                // At most one candidate package can hold the app's R class; the rest missing is the normal case.
            }
        }
        return null;
    }

    private static Set<String> rawClassPackages() {
        String applicationId = AndroidPlatformModule.getAppInfoProvider().getPackageName();
        String applicationPackage = applicationClassPackage();
        String launcherPackage = launcherActivityPackage();

        Set<String> packages = new LinkedHashSet<>();
        addPackage(packages, applicationId);
        addPackage(packages, applicationPackage);
        addPackage(packages, launcherPackage);
        // Exact packages go before any parent: a parent of applicationId may belong to a library shipping res/raw.
        addParentPackages(packages, applicationId, applicationPackage, launcherPackage);
        return packages;
    }

    private static void addPackage(Set<String> packages, @Nullable String pkg) {
        // Floor at two segments: "com.R$raw" and friends belong to no app and only cost lookups.
        if (pkg != null && pkg.indexOf('.') > 0) {
            packages.add(pkg);
        }
    }

    private static void addParentPackages(Set<String> packages, @Nullable String... sources) {
        List<String> parents = new ArrayList<>();
        for (String source : sources) {
            for (String parent = packageOf(source); parent != null; parent = packageOf(parent)) {
                parents.add(parent);
            }
        }

        // Deepest first: a two-segment parent of one source must not outrun the real R package of another, which
        // may sit deeper. The sort is stable, so same-depth parents keep the order of the sources they came from.
        Collections.sort(parents, (left, right) -> segmentCount(right) - segmentCount(left));
        for (String parent : parents) {
            addPackage(packages, parent);
        }
    }

    private static int segmentCount(String pkg) {
        int segments = 1;
        for (int i = 0; i < pkg.length(); i++) {
            if (pkg.charAt(i) == '.') {
                segments++;
            }
        }
        return segments;
    }

    @Nullable private static String applicationClassPackage() {
        // Resolving a candidate is binder-IPC and can die on its own; that must not cost the other candidates.
        try {
            ApplicationInfo applicationInfo =
                    AndroidPlatformModule.getAppInfoProvider().getApplicationInfo();
            return applicationInfo == null ? null : packageOf(applicationInfo.className);
        } catch (Exception e) {
            PWLog.noise(TAG, "Failed to resolve the Application class package", e);
            return null;
        }
    }

    @Nullable private static String launcherActivityPackage() {
        try {
            PackageManager packageManager =
                    AndroidPlatformModule.getManagerProvider().getPackageManager();
            if (packageManager == null) {
                return null;
            }

            Intent launchIntent = packageManager.getLaunchIntentForPackage(
                    AndroidPlatformModule.getAppInfoProvider().getPackageName());
            ComponentName component = launchIntent == null ? null : launchIntent.getComponent();
            if (component == null) {
                return null;
            }

            return packageOf(launcherActivityName(packageManager, component));
        } catch (Exception e) {
            PWLog.noise(TAG, "Failed to resolve the launcher activity package", e);
            return null;
        }
    }

    private static String launcherActivityName(PackageManager packageManager, ComponentName component) {
        // An <activity-alias> launcher lives in the branding package, while R sits in the real activity's one.
        try {
            ActivityInfo activityInfo = packageManager.getActivityInfo(component, 0);
            if (activityInfo.targetActivity != null) {
                return activityInfo.targetActivity;
            }
        } catch (PackageManager.NameNotFoundException e) {
            PWLog.noise(TAG, "Failed to read launcher activity info for " + component, e);
        }

        return component.getClassName();
    }

    @Nullable private static String packageOf(@Nullable String className) {
        if (TextUtils.isEmpty(className)) {
            return null;
        }

        int lastDot = className.lastIndexOf('.');
        return lastDot <= 0 ? null : className.substring(0, lastDot);
    }

    private static void collectSoundNames(Class<?> rawClass, List<String> files) {
        for (final Field field : rawClass.getFields()) {
            String name = field.getName();
            try {
                int res = AndroidPlatformModule.getResourceProvider().getIdentifier(name, "raw");
                if (res <= 0) {
                    // A candidate package may hold a foreign R$raw; names missing from our table are skipped.
                    continue;
                }

                TypedValue value = new TypedValue();
                AndroidPlatformModule.getResourceProvider().getValue(res, value, true);
                if (value.string != null && isSound(value.string.toString())) {
                    files.add(name);
                }
            } catch (Exception e) {
                // One unreadable field must not truncate the sounds of the whole app.
                PWLog.noise(TAG, "Failed to resolve raw resource " + name, e);
            }
        }
    }

    public static int parseColor(String color) {
        int parsedColor = 0xFFFFFFFF;
        try {
            if (color.startsWith("#") && (color.length() == 7 || color.length() == 9)) { // #rrggbb   #aarrggbb
                parsedColor = Color.parseColor(color);
            } else if (color.startsWith("#") && color.length() == 4) { // #rgb
                char[] chars = color.toCharArray();
                parsedColor = Color.parseColor("#" + chars[1] + chars[1] + chars[2] + chars[2] + chars[3] + chars[3]);
            } else if (color.startsWith("#") && color.length() == 5) { // #argb
                char[] chars = color.toCharArray();
                parsedColor = Color.parseColor(
                        "#" + chars[1] + chars[1] + chars[2] + chars[2] + chars[3] + chars[3] + chars[4] + chars[4]);
            } else { // 255,255,255
                String[] colorArr = color.split(",");
                parsedColor = Color.argb(
                        Integer.parseInt(colorArr[3]),
                        Integer.parseInt(colorArr[0]),
                        Integer.parseInt(colorArr[1]),
                        Integer.parseInt(colorArr[2]));
            }
        } catch (Exception e) {
            PWLog.exception(e);
        }

        return parsedColor;
    }

    private static boolean isSound(String fileName) {
        for (String format : SUPPORTED_AUDIO_FORMATS) {
            if (fileName.toLowerCase(Locale.US).endsWith(format)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isMainActivity(Activity activity) {
        PackageManager packageManager = activity.getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(activity.getPackageName());

        if (launchIntent == null) return false;

        ComponentName componentName = launchIntent.getComponent();

        if (componentName == null) return false;

        return TextUtils.equals(
                launcherActivityName(packageManager, componentName),
                activity.getClass().getName());
    }

    public static int getAppVersion() {
        return AndroidPlatformModule.getAppInfoProvider().getVersionCode();
    }
}
