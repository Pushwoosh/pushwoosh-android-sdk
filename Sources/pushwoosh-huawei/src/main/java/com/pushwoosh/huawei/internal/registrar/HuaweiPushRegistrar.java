package com.pushwoosh.huawei.internal.registrar;

import android.content.Context;

import com.pushwoosh.PushwooshWorkManagerHelper;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;

import static com.pushwoosh.internal.platform.AndroidPlatformModule.NULL_CONTEXT_MESSAGE;

public class HuaweiPushRegistrar implements PushRegistrar {

    private Impl impl;

    @Override
    public void init() {
        impl = new Impl();
    }

    @Override
    public void checkDevice(String appId) throws Exception {
        impl.checkDevice(appId);
    }

    @Override
    public void registerPW() {
        impl.registerPW();
    }

    @Override
    public void unregisterPW() {
        impl.unregisterPW();
    }

    private static class Impl {
        private static final String TAG = "PushRegistrarHMS";

        /**
         * Permission necessary to receive HMS intents.
         */
        private static final String PERMISSION_HMS_INTENTS = "";

        @Nullable
        private final Context context;
        private final RegistrationPrefs registrationPrefs;

        private Impl() {
            context = AndroidPlatformModule.getApplicationContext();
            registrationPrefs = RepositoryModule.getRegistrationPreferences();
        }

        void checkDevice(final String appId) throws Exception {
            String senderId = registrationPrefs.projectId().get();

            GeneralUtils.checkNotNullOrEmpty(appId, "mAppId");
            GeneralUtils.checkNotNullOrEmpty(senderId, "mSenderId");

            // Make sure the manifest was properly set - comment out this line
            // while developing the app, then uncomment it when it's ready.
            if (context == null) {
                PWLog.error(NULL_CONTEXT_MESSAGE);
                return;
            }

            checkManifest(context);
        }

        void registerPW() {
            Data inputData = new Data.Builder()
                    .putBoolean(HmsRegistrarWorker.DATA_REGISTER, true)
                    .build();
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(HmsRegistrarWorker.class)
                    .setInputData(inputData)
                    .setConstraints(PushwooshWorkManagerHelper.getNetworkAvailableConstraints())
                    .build();
            PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(request, HmsRegistrarWorker.TAG, ExistingWorkPolicy.REPLACE);
        }

        void unregisterPW() {
            Data inputData = new Data.Builder()
                    .putBoolean(HmsRegistrarWorker.DATA_UNREGISTER, true)
                    .build();
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(HmsRegistrarWorker.class)
                    .setInputData(inputData)
                    .setConstraints(PushwooshWorkManagerHelper.getNetworkAvailableConstraints())
                    .build();
            PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(request, HmsRegistrarWorker.TAG, ExistingWorkPolicy.REPLACE);
        }

        /**
         * Checks that the application manifest is properly configured.
         * <p/>
         * A proper configuration means:
         * <ol>
         * {@value HuaweiPushRegistrar.Impl#PERMISSION_HMS_INTENTS} permission.
         * </ol>
         * <p/>
         * This method should be used during development time to verify that the
         * manifest is properly set up, but it doesn't need to be called once the
         * application is deployed to the users' devices.
         *
         * @param context application context.
         * @throws IllegalStateException if any of the conditions above is not met.
         */
        @SuppressWarnings("WrongConstant")
        static void checkManifest(@NonNull Context context) {

        }
    }
}
