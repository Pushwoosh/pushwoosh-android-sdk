/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arellomobile.android.push;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.amazon.device.messaging.ADM;

/**
 * Utilities for device registration.
 * <p/>
 * <strong>Note:</strong> this class uses a private {@link android.content.SharedPreferences}
 * object to keep track of the registration token.
 */
public final class ADMRegistrar
{
	/**
	 * Permission necessary to receive ADM intents.
	 */
	public static final String PERMISSION_ADM_INTENTS = "com.amazon.device.messaging.permission.SEND";
	/**
	 * Intent sent by ADM indicating with the result of a registration request.
	 */
	public static final String INTENT_FROM_ADM_REGISTRATION_CALLBACK = "com.amazon.device.messaging.intent.REGISTRATION";
	/**
	 * Intent sent by ADM containing a message.
	 */
	public static final String INTENT_FROM_ADM_MESSAGE = "com.amazon.device.messaging.intent.RECEIVE";


    private static final String TAG = "ADMRegistrar";

	/**
	 * Checks if the device has the proper dependencies installed.
	 * <p/>
	 * This method should be called when the application starts to verify that
	 * the device supports GCM.
	 *
	 * @param adm Amazon device messaging registration manager.
	 * @throws UnsupportedOperationException if the device does not support GCM.
	 */
	public static void checkDevice(final ADM adm)
	{
		if (!adm.isSupported())
		{
			throw new UnsupportedOperationException("ADM is not supported on the current device");
		}
	}

    /**
     * Checks that the application manifest is properly configured.
     * <p/>
     * A proper configuration means:
     * <ol>
     * <li>It creates a custom permission called
     * {@code PACKAGE_NAME.permission.RECEIVE_ADM_MESSAGE}.
     * <li>It defines at least one {@link android.content.BroadcastReceiver} with category
     * {@code PACKAGE_NAME}.
     * <li>The {@link android.content.BroadcastReceiver}(s) uses the
     * {@value com.arellomobile.android.push.ADMRegistrar#PERMISSION_ADM_INTENTS} permission.
     * <li>The {@link android.content.BroadcastReceiver}(s) handles the 2 ADM intents
     * ({@value com.arellomobile.android.push.ADMRegistrar#INTENT_FROM_ADM_MESSAGE},
     * and {@value com.arellomobile.android.push.ADMRegistrar#INTENT_FROM_ADM_REGISTRATION_CALLBACK}).
     * </ol>
     * ...where {@code PACKAGE_NAME} is the application package.
     * <p/>
     * This method should be used during development time to verify that the
     * manifest is properly set up, but it doesn't need to be called once the
     * application is deployed to the users' devices.
     *
     * @param context application context.
     * @throws IllegalStateException if any of the conditions above is not met.
     */
    public static void checkManifest(Context context)
    {
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        String permissionName = packageName + ".permission.RECEIVE_ADM_MESSAGE";
        // check permission
        try
        {
	        //noinspection ConstantConditions
	        packageManager.getPermissionInfo(permissionName, PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException e)
        {
            throw new IllegalStateException(
                    "Application does not define permission " + permissionName);
        }
        // check receivers
        PackageInfo receiversInfo;
        try
        {
            receiversInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_RECEIVERS);
        } catch (NameNotFoundException e)
        {
            throw new IllegalStateException("Could not get receivers for package " + packageName);
        }
        ActivityInfo[] receivers = receiversInfo.receivers;
        if (receivers == null || receivers.length == 0)
        {
            throw new IllegalStateException("No receiver for package " + packageName);
        }
        if (Log.isLoggable(TAG, Log.VERBOSE))
        {
            Log.v(TAG, "number of receivers for " + packageName + ": " + receivers.length);
        }
        Set<String> allowedReceivers = new HashSet<String>();
        for (ActivityInfo receiver : receivers)
        {
            if (PERMISSION_ADM_INTENTS.equals(receiver.permission))
            {
                allowedReceivers.add(receiver.name);
            }
        }
        if (allowedReceivers.isEmpty())
        {
            throw new IllegalStateException("No receiver allowed to receive " + PERMISSION_ADM_INTENTS);
        }
        checkReceiver(context, allowedReceivers, INTENT_FROM_ADM_REGISTRATION_CALLBACK);
        checkReceiver(context, allowedReceivers, INTENT_FROM_ADM_MESSAGE);
    }

    private static void checkReceiver(Context context, Set<String> allowedReceivers, String action)
    {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        Intent intent = new Intent(action);
        intent.setPackage(packageName);
	    //noinspection ConstantConditions
	    List<ResolveInfo> receivers = pm.queryBroadcastReceivers(intent, PackageManager.GET_INTENT_FILTERS);
        if (receivers.isEmpty())
        {
            throw new IllegalStateException("No receivers for action " + action);
        }
        if (Log.isLoggable(TAG, Log.VERBOSE))
        {
            Log.v(TAG, "Found " + receivers.size() + " receivers for action " + action);
        }
        // make sure receivers match
        for (ResolveInfo receiver : receivers)
        {
	        //noinspection ConstantConditions
	        String name = receiver.activityInfo.name;
            if (!allowedReceivers.contains(name))
            {
                throw new IllegalStateException("Receiver " + name + " is not set with permission " + PERMISSION_ADM_INTENTS);
            }
        }
    }
}
