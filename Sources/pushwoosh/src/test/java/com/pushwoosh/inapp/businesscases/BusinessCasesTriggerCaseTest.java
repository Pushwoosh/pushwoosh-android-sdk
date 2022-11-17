package com.pushwoosh.inapp.businesscases;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.utils.AppVersionProvider;
import com.pushwoosh.internal.utils.TimeProvider;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BusinessCasesManager.class, Looper.class})
public class BusinessCasesTriggerCaseTest {
    public static final String CODE_1 = "code1";
    public static final long UPDATED_WELCOME = 1L;

    private BusinessCasesManager businessCasesManager;
    @Mock
    private PrefsProvider prefsProviderMock;
    @Mock
    private AppInfoProvider appInfoProviderMock;
    @Mock
    private TimeProvider timeProviderMock;
    @Mock
    private AppVersionProvider appVersionProvider;
    @Mock
    private BusinessCase businessCaseWelcome;
    @Mock
    private SharedPreferences sharedPreferencesMock;
    @Mock
    private Bundle metaDataMock;
    @Mock
    private ApplicationInfo applicationInfoMock;

    private final static ScheduledExecutorService mainThread = Executors.newSingleThreadScheduledExecutor();

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Looper.class);
        Looper mockMainThreadLooper = PowerMockito.mock(Looper.class);
        when(Looper.getMainLooper()).thenReturn(mockMainThreadLooper);
        Handler mockMainThreadHandler = PowerMockito.mock(Handler.class);
        Answer<Boolean> handlerPostAnswer = new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = invocation.getArgumentAt(0, Runnable.class);
                Long delay = 0L;
                if (invocation.getArguments().length > 1) {
                    delay = invocation.getArgumentAt(1, Long.class);
                }
                if (runnable != null) {
                    mainThread.schedule(runnable, delay, TimeUnit.MILLISECONDS);
                }
                return true;
            }
        };
        doAnswer(handlerPostAnswer).when(mockMainThreadHandler).post(any(Runnable.class));
        doAnswer(handlerPostAnswer).when(mockMainThreadHandler).postDelayed(any(Runnable.class), anyLong());
        PowerMockito.whenNew(Handler.class).withAnyArguments().thenReturn(mockMainThreadHandler);
        HandlerThread handlerThread = PowerMockito.mock(HandlerThread.class);
        PowerMockito.whenNew(HandlerThread.class)
                .withAnyArguments()
                .thenReturn(handlerThread);

        applicationInfoMock.metaData = metaDataMock;
        when(appInfoProviderMock.getApplicationInfo()).thenReturn(applicationInfoMock);
        when(prefsProviderMock.providePrefs("PWBusinessCasesState")).thenReturn(sharedPreferencesMock);
        businessCasesManager = new BusinessCasesManager(prefsProviderMock, appInfoProviderMock, timeProviderMock, appVersionProvider);
        Map<String, BusinessCase> businessCaseMap = new HashMap<>();
        businessCaseMap.put(BusinessCasesManager.WELCOME_CASE, businessCaseWelcome);
        Whitebox.setInternalState(businessCasesManager, "businessCases", businessCaseMap);

        InAppStorage inAppStorage = mock(InAppStorage.class);
        InAppModule.setInAppStorage(inAppStorage);
        Resource welcomeResource = new Resource(CODE_1, "url1", "hash1", UPDATED_WELCOME, InAppLayout.DIALOG, null, false, 3, BusinessCasesManager.WELCOME_CASE, "");

        when(inAppStorage.getResource(eq(CODE_1))).thenReturn(welcomeResource);
    }

    @Test
    @Ignore("java.lang.NoSuchMethodError")
    public void triggerCase() throws Exception {
        BusinessCase.BusinessCaseCallback businessCaseCallback = result -> { };
        businessCasesManager.triggerCase(BusinessCasesManager.WELCOME_CASE, businessCaseCallback);
        verify(businessCaseWelcome).trigger(businessCaseCallback);
    }

}
