/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.chain.Chain;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.preference.PreferenceArrayListValue;
import com.pushwoosh.notification.handlers.notification.PushNotificationOpenHandler;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.Iterator;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class NotificationOpenHandlerTest {

    private NotificationOpenHandler notificationOpenHandler;

    Chain<PushNotificationOpenHandler> notificationOpenHandlerChainMock;
    private PlatformTestManager platformTestManager;

    private PushNotificationOpenHandler pushNotificationOpenHandler1;
    private PushNotificationOpenHandler pushNotificationOpenHandler2;

    @Before
    public void setUp() throws Exception {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();

        notificationOpenHandlerChainMock = Mockito.mock(Chain.class);
        Iterator<PushNotificationOpenHandler> iterator = createIterator();
        when(notificationOpenHandlerChainMock.getIterator()).thenReturn(iterator);
        notificationOpenHandler = new NotificationOpenHandler(notificationOpenHandlerChainMock);
    }

    @NonNull private Iterator<PushNotificationOpenHandler> createIterator() {
        pushNotificationOpenHandler1 = mock(PushNotificationOpenHandler.class);
        pushNotificationOpenHandler2 = mock(PushNotificationOpenHandler.class);
        Iterator<PushNotificationOpenHandler> iterator = Mockito.mock(Iterator.class);
        when(iterator.hasNext()).thenReturn(true, true, false);
        when(iterator.next()).thenReturn(pushNotificationOpenHandler1, pushNotificationOpenHandler2);
        return iterator;
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    private Bundle createTestBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("one", "1");
        bundle.putBoolean("two", true);
        bundle.putInt("three", 3);
        bundle.putString("l", "http:\\\\link");
        return bundle;
    }

    @Test
    public void startPushLauncherActivity() {
        PushMessage pushMessage = new PushMessage(createTestBundle());
        notificationOpenHandler.startPushLauncherActivity(pushMessage);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(AndroidPlatformModule.getApplicationContext()).startActivity(intentArgumentCaptor.capture());
        Assert.assertEquals(1, intentArgumentCaptor.getAllValues().size());
        Intent intent = intentArgumentCaptor.getValue();
        Assert.assertEquals("com.pushwoosh.test.MESSAGE", intent.getAction());
        Assert.assertEquals(
                "Bundle[{PUSH_RECEIVE_EVENT={\"l\":\"http:\\\\\\\\link\",\"one\":\"1\",\"two\":true,\"three\":3}}]",
                intent.getExtras().toString());
    }

    @Test
    public void postHandleNotification() {
        Bundle bundle = createTestBundle();
        notificationOpenHandler.postHandleNotification(bundle);

        Mockito.verify(pushNotificationOpenHandler1).postHandleNotification(eq(bundle));
        Mockito.verify(pushNotificationOpenHandler2).postHandleNotification(eq(bundle));
    }

    @SuppressWarnings("unchecked")
    private MockedStatic<RepositoryModule> stubAllowedExternalHosts(ArrayList<String> hosts) {
        NotificationPrefs prefs = mock(NotificationPrefs.class);
        PreferenceArrayListValue<String> listValue = mock(PreferenceArrayListValue.class);
        when(listValue.get()).thenReturn(hosts);
        when(prefs.allowedExternalHosts()).thenReturn(listValue);
        MockedStatic<RepositoryModule> mocked = mockStatic(RepositoryModule.class, Mockito.CALLS_REAL_METHODS);
        mocked.when(RepositoryModule::getNotificationPreferences).thenReturn(prefs);
        return mocked;
    }

    @Test
    public void preHandleNotification_noLinkKey_returnsFalse() {
        Context context = AndroidPlatformModule.getApplicationContext();
        Mockito.clearInvocations(context);

        boolean result = notificationOpenHandler.preHandleNotification(new Bundle());

        Assert.assertFalse(result);
        verify(context, never()).startActivity(any(Intent.class));
    }

    @Test
    public void preHandleNotification_whitelistedHost_firesExternalViewIntent() {
        Context context = AndroidPlatformModule.getApplicationContext();
        Mockito.clearInvocations(context);
        Bundle bundle = new Bundle();
        bundle.putString("l", "https://allowed.example.com/promo");
        ArrayList<String> allowed = new ArrayList<>();
        allowed.add("allowed.example.com");

        boolean result;
        try (MockedStatic<RepositoryModule> mocked = stubAllowedExternalHosts(allowed)) {
            result = notificationOpenHandler.preHandleNotification(bundle);
        }

        Assert.assertTrue(result);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context, times(1)).startActivity(captor.capture());
        Intent intent = captor.getValue();
        Assert.assertEquals(Intent.ACTION_VIEW, intent.getAction());
        Assert.assertEquals("https://allowed.example.com/promo", intent.getDataString());
        Assert.assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void preHandleNotification_unresolvableLink_returnsFalse() {
        Context context = AndroidPlatformModule.getApplicationContext();
        Mockito.clearInvocations(context);
        Bundle bundle = new Bundle();
        bundle.putString("l", "https://unknown.example.com/path");
        // Whitelist empty + Robolectric has no resolver for this URI by default → resolveActivity == null.

        boolean result;
        try (MockedStatic<RepositoryModule> mocked = stubAllowedExternalHosts(new ArrayList<>())) {
            result = notificationOpenHandler.preHandleNotification(bundle);
        }

        Assert.assertFalse(result);
        verify(context, never()).startActivity(any(Intent.class));
    }

    @Test
    public void preHandleNotification_nonWhitelistedResolvableLink_returnsTrue() {
        Context context = AndroidPlatformModule.getApplicationContext();
        Mockito.clearInvocations(context);
        Bundle bundle = new Bundle();
        bundle.putString("l", "myapp://deep/link");

        // Register a resolver for the deep link so resolveActivity returns non-null.
        PackageManager pm = context.getPackageManager();
        ShadowPackageManager shadowPm = Shadows.shadowOf(pm);
        Intent matcher = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("myapp://deep/link"));
        ResolveInfo info = new ResolveInfo();
        info.activityInfo = new android.content.pm.ActivityInfo();
        info.activityInfo.packageName = "com.pushwoosh.test";
        info.activityInfo.name = "FakeDeepLinkActivity";
        shadowPm.addResolveInfoForIntent(matcher, info);

        boolean result;
        try (MockedStatic<RepositoryModule> mocked = stubAllowedExternalHosts(new ArrayList<>())) {
            result = notificationOpenHandler.preHandleNotification(bundle);
        }

        // The non-whitelisted resolvable path is taken: code reached PendingIntent.send() without
        // returning false. PendingIntent.send() dispatches through Robolectric's Instrumentation
        // bypassing our context spy, so the intent itself cannot be captured via verify(context).
        // We assert (a) the method returned true, and (b) the external-host short-circuit was NOT
        // taken (no plain startActivity invocation captured on the spy).
        Assert.assertTrue(result);
        verify(context, never()).startActivity(any(Intent.class));
    }

    @Test
    public void startPushLauncherActivity_activityNotFound_fallsBackToLauncher() {
        Context context = AndroidPlatformModule.getApplicationContext();
        Mockito.clearInvocations(context);

        // Register a default launcher intent for the package so the fallback succeeds.
        PackageManager pm = context.getPackageManager();
        ShadowPackageManager shadowPm = Shadows.shadowOf(pm);
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setPackage("com.pushwoosh.test");
        ResolveInfo info = new ResolveInfo();
        info.activityInfo = new android.content.pm.ActivityInfo();
        info.activityInfo.packageName = "com.pushwoosh.test";
        info.activityInfo.name = "FakeLauncherActivity";
        shadowPm.addResolveInfoForIntent(launchIntent, info);

        // Throw ActivityNotFoundException ONLY for the primary intent (action ends with ".MESSAGE").
        doThrow(new ActivityNotFoundException("primary missing"))
                .when(context)
                .startActivity(argThat(intent -> intent != null
                        && intent.getAction() != null
                        && intent.getAction().endsWith(".MESSAGE")));

        PushMessage message = new PushMessage(createTestBundle());
        notificationOpenHandler.startPushLauncherActivity(message);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context, times(2)).startActivity(captor.capture());
        Intent fallbackIntent = captor.getAllValues().get(1);
        Assert.assertTrue(
                "fallback must declare CATEGORY_LAUNCHER",
                fallbackIntent.getCategories() != null
                        && fallbackIntent.getCategories().contains(Intent.CATEGORY_LAUNCHER));
        int expectedFlags =
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP;
        Assert.assertEquals(expectedFlags, fallbackIntent.getFlags() & expectedFlags);
        Assert.assertNotNull(
                "fallback intent must carry PUSH_RECEIVE_EVENT extra",
                fallbackIntent.getStringExtra(com.pushwoosh.Pushwoosh.PUSH_RECEIVE_EVENT));
    }

    @Test
    public void startPushLauncherActivity_fallbackAlsoThrows_doesNotPropagate() {
        Context context = AndroidPlatformModule.getApplicationContext();
        Mockito.clearInvocations(context);

        // Register a launcher resolver so getIntent() returns non-null, then both startActivity calls throw.
        PackageManager pm = context.getPackageManager();
        ShadowPackageManager shadowPm = Shadows.shadowOf(pm);
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setPackage("com.pushwoosh.test");
        ResolveInfo info = new ResolveInfo();
        info.activityInfo = new android.content.pm.ActivityInfo();
        info.activityInfo.packageName = "com.pushwoosh.test";
        info.activityInfo.name = "FakeLauncherActivity";
        shadowPm.addResolveInfoForIntent(launchIntent, info);

        doThrow(new ActivityNotFoundException("always missing")).when(context).startActivity(any(Intent.class));

        PushMessage message = new PushMessage(createTestBundle());
        // Must not propagate.
        notificationOpenHandler.startPushLauncherActivity(message);

        verify(context, times(2)).startActivity(any(Intent.class));
    }

    @Test
    public void startPushLauncherActivity_noLauncherIntent_swallowsActivityNotFound() {
        Context context = AndroidPlatformModule.getApplicationContext();
        Mockito.clearInvocations(context);

        // Throw on first call so the fallback branch is entered; package has no LAUNCHER intent (default).
        doThrow(new ActivityNotFoundException("primary missing"))
                .when(context)
                .startActivity(argThat(intent -> intent != null
                        && intent.getAction() != null
                        && intent.getAction().endsWith(".MESSAGE")));

        // Stub ManagerProvider.getPackageManager() to return null so getIntent() throws.
        ManagerProvider managerProvider = mock(ManagerProvider.class);
        when(managerProvider.getPackageManager()).thenReturn(null);

        PushMessage message = new PushMessage(createTestBundle());

        try (MockedStatic<AndroidPlatformModule> platformMock =
                mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS)) {
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            // Must not propagate even though getIntent() throws ActivityNotFoundException.
            notificationOpenHandler.startPushLauncherActivity(message);
        }

        // Only the primary startActivity attempt happened — fallback bailed before reaching context.startActivity.
        verify(context, times(1)).startActivity(any(Intent.class));
    }
}
