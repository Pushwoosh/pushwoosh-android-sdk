package com.pushwoosh.repository;

import android.os.Bundle;

import com.pushwoosh.repository.util.PushBundleDatabaseEntry;
import java.util.List;

public interface PushBundleStorage {
    long putPushBundle(Bundle pushBundle) throws Exception;
    Bundle getPushBundle(long id) throws Exception;
    void removePushBundle(long id);

    long putGroupPushBundle(Bundle pushBundle, int id, String groupId) throws Exception;
    List<Bundle> getGroupPushBundles();
    void removeGroupPushBundle(long id);
    void removeGroupPushBundles();

    PushBundleDatabaseEntry getLastPushBundleEntryForGroup(String groupId) throws Exception;
}
