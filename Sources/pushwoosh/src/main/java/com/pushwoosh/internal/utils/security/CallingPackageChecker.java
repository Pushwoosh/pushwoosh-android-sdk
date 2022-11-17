package com.pushwoosh.internal.utils.security;

import android.content.ContentProvider;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

public class CallingPackageChecker {

    public static void checkCallingPackage(ContentProvider contentProvider) {
        //automatically pass security check if sdk version is lower than 19
        if (Build.VERSION.SDK_INT < 19) {
            return;
        }
        Context context = contentProvider.getContext();
        String callingPackage = contentProvider.getCallingPackage();

        if (context != null && TextUtils.equals(callingPackage,context.getPackageName())) {
            //called from the same package, pass security check
            return;
        }

        throw new SecurityException("Provider does not allow granting of Uri permissions");
    }

    public static void checkIfTrustedPackage(ContentProvider contentProvider, String[] trustedPackages) {
        //automatically pass security check if sdk version is lower than 19
        if (Build.VERSION.SDK_INT < 19) {
            return;
        }
        String callingPackage = contentProvider.getCallingPackage();

        if (trustedPackages.length > 0) {
            for (String packageName: trustedPackages) {
                if (TextUtils.equals(callingPackage, packageName)) {
                    //called from trusted package name, pass security check
                    return;
                }
            }
        }

        throw new SecurityException(contentProvider.getClass().getName() + "queried by an untrusted package.");
    }
}

