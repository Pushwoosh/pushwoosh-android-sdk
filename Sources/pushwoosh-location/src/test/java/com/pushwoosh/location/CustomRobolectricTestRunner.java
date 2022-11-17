package com.pushwoosh.location;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.manifest.BroadcastReceiverData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomRobolectricTestRunner extends RobolectricTestRunner {
    private Set<String> deleteBroadcastPackage = new HashSet<>();
    /**
     * Creates a runner to run {@code testClass}. Looks in your working directory for your AndroidManifest.xml file
     * and res directory by default. Use the {@link Config} annotation to configure.
     *
     * @param testClass the test class to be run
     * @throws InitializationError if junit says so
     */
    public CustomRobolectricTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
        deleteBroadcastPackage.add("com.pushwoosh.location.geofencer.GeofenceReceiver");
    }

    @Override
    protected AndroidManifest getAppManifest(Config config) {
        AndroidManifest manifest = super.getAppManifest(config);
        List<BroadcastReceiverData> broadcastReceivers = manifest.getBroadcastReceivers();
        List<BroadcastReceiverData> removeList = new ArrayList<>();
        for (BroadcastReceiverData receiverData : broadcastReceivers) {
            if (isDeletePackage(receiverData.getClassName())) {
                removeList.add(receiverData);
            }
        }
        broadcastReceivers.removeAll(removeList);
        return  manifest;
    }

    private boolean isDeletePackage(String className) {
        for (String s : deleteBroadcastPackage) {
            if (className.startsWith(s)) {
                return true;
            }
        }
        return false;
    }
}
