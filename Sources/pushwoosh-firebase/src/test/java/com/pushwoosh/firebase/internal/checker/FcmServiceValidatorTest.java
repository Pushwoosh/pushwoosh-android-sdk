package com.pushwoosh.firebase.internal.checker;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import com.pushwoosh.firebase.PushFcmIntentService;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class FcmServiceValidatorTest {
    private static final String TAG = "FcmServiceValidator";
    private static final String FCM_FALLBACK = "com.google.firebase.messaging.FirebaseMessagingService";

    private MockedStatic<PWLog> pwLogMock;
    private MockedStatic<AndroidPlatformModule> platformMock;
    private MockedStatic<RepositoryModule> repoMock;
    private Context context;
    private PackageManager packageManager;
    private PreferenceBooleanValue flag;

    @Before
    public void setUp() {
        pwLogMock = mockStatic(PWLog.class);
        platformMock = mockStatic(AndroidPlatformModule.class);
        repoMock = mockStatic(RepositoryModule.class);

        context = mock(Context.class);
        packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getPackageName()).thenReturn("com.pushwoosh.test");
        platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);

        RegistrationPrefs prefs = mock(RegistrationPrefs.class);
        flag = mock(PreferenceBooleanValue.class);
        when(prefs.fcmMessageReceivedSinceInstallOrUpdate()).thenReturn(flag);
        when(flag.get()).thenReturn(false);
        repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(prefs);
    }

    @After
    public void tearDown() {
        FcmServiceValidator.resetForTesting();
        repoMock.close();
        platformMock.close();
        pwLogMock.close();
    }

    private static ResolveInfo service(String className) {
        ResolveInfo info = new ResolveInfo();
        info.serviceInfo = new ServiceInfo();
        info.serviceInfo.packageName = "com.pushwoosh.test";
        info.serviceInfo.name = className;
        return info;
    }

    private void givenResolution(ResolveInfo winner, List<ResolveInfo> registered) {
        when(packageManager.resolveService(any(Intent.class), anyInt())).thenReturn(winner);
        when(packageManager.queryIntentServices(any(Intent.class), anyInt())).thenReturn(registered);
    }

    private void verifySilent() {
        pwLogMock.verify(() -> PWLog.warn(anyString(), anyString()), never());
        pwLogMock.verify(() -> PWLog.info(anyString(), anyString()), never());
    }

    // 1. Pushwoosh service wins and the app has none of its own: stay silent.
    @Test
    public void pushwooshServiceWins_silent() {
        ResolveInfo winner = service(PushFcmIntentService.class.getName());
        givenResolution(winner, asList(winner, service(FCM_FALLBACK)));

        FcmServiceValidator.validate();

        verifySilent();
    }

    // 2. Winner extends the router: stay silent, forwarding is guaranteed by the type.
    @Test
    public void routerSubclassWins_silent() {
        ResolveInfo winner = service(FakeRouterWinnerService.class.getName());
        givenResolution(winner, singletonList(winner));

        FcmServiceValidator.validate();

        verifySilent();
    }

    // 3. Foreign winner, flag not set: verdict A at info level, naming the winner and the condition under
    // which nothing is wrong — a correct manual integration passes through this state until the first push.
    @Test
    public void foreignWinner_flagNotSet_informsNamingWinner() {
        when(flag.get()).thenReturn(false);
        ResolveInfo winner = service(FakeForeignService.class.getName());
        givenResolution(winner, singletonList(winner));

        FcmServiceValidator.validate();

        pwLogMock.verify(() -> PWLog.info(
                eq(TAG),
                argThat((String s) -> s.contains(FakeForeignService.class.getName())
                        && s.contains("installed or last updated")
                        && s.contains("clears after the first Pushwoosh message"))));
        pwLogMock.verify(() -> PWLog.warn(anyString(), anyString()), never());
    }

    // 4. Foreign winner, flag set: stay silent, the manual integration has been observed.
    @Test
    public void foreignWinner_flagSet_silent() {
        when(flag.get()).thenReturn(true);
        ResolveInfo winner = service(FakeForeignService.class.getName());
        givenResolution(winner, singletonList(winner));

        FcmServiceValidator.validate();

        verifySilent();
    }

    // 5. A losing service of the app is named explicitly.
    @Test
    public void losingAppService_namedInWarning() {
        ResolveInfo winner = service(FakeRouterWinnerService.class.getName());
        givenResolution(winner, asList(winner, service(FakeForeignService.class.getName())));

        FcmServiceValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq(TAG),
                argThat((String s) ->
                        s.contains(FakeForeignService.class.getName()) && s.contains("will not deliver"))));
    }

    // 6. The firebase-messaging fallback and PushFcmIntentService are not named among the losers.
    @Test
    public void fallbackAndPushwooshService_notNamedAmongLosers() {
        ResolveInfo winner = service(FakeRouterWinnerService.class.getName());
        givenResolution(winner, asList(winner, service(FCM_FALLBACK), service(PushFcmIntentService.class.getName())));

        FcmServiceValidator.validate();

        verifySilent();
    }

    // 7. The firebase-messaging fallback wins: the SDK service was cut from the manifest and the app
    // declares none. Nothing clears this state, so it is a warning, not a notice.
    @Test
    public void fallbackWins_warnsNoServiceReceivesPushwoosh() {
        ResolveInfo winner = service(FCM_FALLBACK);
        givenResolution(winner, singletonList(winner));

        FcmServiceValidator.validate();

        pwLogMock.verify(() -> PWLog.warn(
                eq(TAG),
                argThat((String s) ->
                        s.contains("No service in this app receives") && s.contains("PushFcmIntentService"))));
        pwLogMock.verify(() -> PWLog.warn(eq(TAG), argThat((String s) -> s.contains("will not deliver"))), never());
        pwLogMock.verify(() -> PWLog.info(anyString(), anyString()), never());
    }

    // Seam: the verdict follows resolveService, not the first element of queryIntentServices.
    // The Robolectric shadow of resolveService returns the first query element, hence plain mocks here.
    @Test
    public void verdictFollowsResolveService_notFirstQueryElement() {
        ResolveInfo foreignWinner = service(FakeForeignService.class.getName());
        // queryIntentServices lies: Pushwoosh service first. resolveService knows better.
        givenResolution(foreignWinner, asList(service(PushFcmIntentService.class.getName()), foreignWinner));

        FcmServiceValidator.validate();

        pwLogMock.verify(() -> PWLog.info(
                eq(TAG),
                argThat((String s) ->
                        s.contains(FakeForeignService.class.getName()) && s.contains("installed or last updated"))));
        pwLogMock.verify(() -> PWLog.warn(eq(TAG), argThat((String s) -> s.contains("will not deliver"))), never());
    }

    // Idempotence of the one-shot gate, same shape as ManifestValidatorTest.
    @Test
    public void scheduleValidationIsIdempotentAcrossCalls() {
        givenResolution(null, null);
        assertTrue(FcmServiceValidator.scheduleValidation(Runnable::run));
        assertFalse(FcmServiceValidator.scheduleValidation(Runnable::run));
        assertFalse(FcmServiceValidator.scheduleValidation(Runnable::run));
    }

    // An exception inside validate is swallowed and reported through PWLog.exception.
    @Test
    public void scheduleValidationSwallowsExceptionAndReportsViaPWLog() {
        when(context.getPackageManager()).thenThrow(new RuntimeException("boom"));
        FcmServiceValidator.scheduleValidation(Runnable::run);
        pwLogMock.verify(() -> PWLog.exception(any(Throwable.class)));
    }

    // The winner is decided by filter priority, not by registration order: the low-priority foreign
    // service is registered FIRST but still loses to the router with priority=1.
    @Test
    public void shadowResolution_priorityBeatsRegistrationOrder() throws Exception {
        Context app = RuntimeEnvironment.getApplication();
        platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(app);

        org.robolectric.shadows.ShadowPackageManager shadowPm =
                org.robolectric.Shadows.shadowOf(app.getPackageManager());

        android.content.ComponentName foreign =
                new android.content.ComponentName(app.getPackageName(), FakeForeignService.class.getName());
        IntentFilter foreignFilter = new IntentFilter("com.google.firebase.MESSAGING_EVENT");
        foreignFilter.setPriority(0);
        shadowPm.addServiceIfNotPresent(foreign);
        shadowPm.addIntentFilterForService(foreign, foreignFilter);

        android.content.ComponentName routerCn =
                new android.content.ComponentName(app.getPackageName(), FakeRouterWinnerService.class.getName());
        IntentFilter routerFilter = new IntentFilter("com.google.firebase.MESSAGING_EVENT");
        routerFilter.setPriority(1);
        shadowPm.addServiceIfNotPresent(routerCn);
        shadowPm.addIntentFilterForService(routerCn, routerFilter);

        FcmServiceValidator.validate();

        // Router wins -> verdict A silent; foreign is the loser named by verdict B.
        pwLogMock.verify(() -> PWLog.warn(
                eq(TAG),
                argThat((String s) ->
                        s.contains(FakeForeignService.class.getName()) && s.contains("will not deliver"))));
        pwLogMock.verify(
                () -> PWLog.info(eq(TAG), argThat((String s) -> s.contains("installed or last updated"))), never());
    }
}
