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

package com.pushwoosh.inapp.businesscases;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.utils.AppVersionProvider;
import com.pushwoosh.internal.utils.TimeProvider;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class BusinessCasesManagerTest {
    public static final String CODE_1 = "code1";
    public static final String CODE_2 = "code2";
    public static final String CODE_3 = "code3";
    public static final long UPDATED_WELCOME = 1L;
    public static final long UPDATED_UPDATE_IN_APP = 2L;
    public static final long UPDATED_RECOVERY = 3L;

    private BusinessCasesManager businessCasesManager;

    @Mock
    private PrefsProvider prefsProviderMock;
    @Mock
    private AppInfoProvider appInfoProviderMock;
    @Mock
    private TimeProvider timeProviderMock;

    @Mock
    private ApplicationInfo applicationInfoMock;
    @Mock
    private Bundle metaDataMock;
    @Mock
    private SharedPreferences sharedPreferencesMock;

    private PlatformTestManager platformTestManager;

    @Mock
    private BusinessCase businessCaseWelcome;
    @Mock
    private BusinessCase businessCaseRecovery;
    @Mock
    private BusinessCase businessCaseUpdate;
    @Mock
    private AppVersionProvider appVersionProvider;

    private Resource welcomeResource;
    private Resource updateResource;
    private Resource recoveryResource;
    private ArrayList<Resource> resourceList;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();



        when(appInfoProviderMock.getApplicationInfo()).thenReturn(applicationInfoMock);
        applicationInfoMock.metaData = metaDataMock;
        when(prefsProviderMock.providePrefs("PWBusinessCasesState")).thenReturn(sharedPreferencesMock);

        businessCasesManager = new BusinessCasesManager(prefsProviderMock, appInfoProviderMock, timeProviderMock, appVersionProvider);
        buildResourcesList();
        setMockBusinessCase();
    }

    private void setMockBusinessCase() {
        Map<String, BusinessCase> businessCaseMap = new HashMap<>();

        businessCaseMap.put(BusinessCasesManager.WELCOME_CASE, businessCaseWelcome);
        when(businessCaseWelcome.getUid()).thenReturn(BusinessCasesManager.WELCOME_CASE);

        businessCaseMap.put(BusinessCasesManager.PUSH_RECOVER_CASE, businessCaseRecovery);
        when(businessCaseRecovery.getUid()).thenReturn(BusinessCasesManager.PUSH_RECOVER_CASE);

        businessCaseMap.put(BusinessCasesManager.APP_UPDATE_CASE, businessCaseUpdate);
        when(businessCaseUpdate.getUid()).thenReturn(BusinessCasesManager.APP_UPDATE_CASE);

        WhiteboxHelper.setInternalState(businessCasesManager, "businessCases", businessCaseMap);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @NonNull
    private Map<String, BusinessCaseData> getStringBusinessCaseDataMap() {
        Map<String, BusinessCaseData> dataMap = new HashMap<>();

        BusinessCaseData welcomeCase = BusinessCaseData.create(CODE_1, UPDATED_WELCOME);
        BusinessCaseData appUpdateCase = BusinessCaseData.create(CODE_2, UPDATED_UPDATE_IN_APP);
        BusinessCaseData pushRecoverCase = BusinessCaseData.create(CODE_3, UPDATED_RECOVERY);
        dataMap.put(BusinessCasesManager.WELCOME_CASE, welcomeCase);
        dataMap.put(BusinessCasesManager.APP_UPDATE_CASE, appUpdateCase);
        dataMap.put(BusinessCasesManager.PUSH_RECOVER_CASE, pushRecoverCase);
        return dataMap;
    }

    @Test
    public void processInAppsData() {
        BusinessCasesManager.processInAppsData(resourceList);
    }


    private void buildResourcesList() {
        resourceList = new ArrayList<>();
        welcomeResource = new Resource(CODE_1, "url1", "hash1", UPDATED_WELCOME, InAppLayout.DIALOG, null, false, 3, BusinessCasesManager.WELCOME_CASE, "");
        updateResource = new Resource(CODE_2, "url2", "hash2", UPDATED_UPDATE_IN_APP, InAppLayout.BOTTOM, null, true, 2, BusinessCasesManager.APP_UPDATE_CASE, "");
        recoveryResource = new Resource(CODE_3, "url3", "hash3", UPDATED_RECOVERY, InAppLayout.FULLSCREEN, null, true, 1, BusinessCasesManager.PUSH_RECOVER_CASE, "");
    }

    @Test
    public void processBusinessCasesData() {
        Map<String, BusinessCaseData> dataMap = getStringBusinessCaseDataMap();
        businessCasesManager.processBusinessCasesData(dataMap, true);

        verify(businessCaseWelcome).setInAppId(eq(CODE_1));
        verify(businessCaseUpdate).setInAppId(eq(CODE_2));
        verify(businessCaseRecovery).setInAppId(eq(CODE_3));
    }

    @Test
    public void processBusinessCasesDataNotReady() {
        addResourceMockStorage();

        Map<String, BusinessCaseData> dataMap = getStringBusinessCaseDataMap();
        businessCasesManager.processBusinessCasesData(dataMap, false);

        verify(businessCaseWelcome).setInAppId(eq(CODE_1));
        verify(businessCaseUpdate).setInAppId(eq(CODE_2));
        verify(businessCaseRecovery).setInAppId(eq(CODE_3));
    }

    private void addResourceMockStorage() {
        InAppStorage inAppStorage = Mockito.mock(InAppStorage.class);
        InAppModule.setInAppStorage(inAppStorage);
        when(inAppStorage.getResource(eq(CODE_1))).thenReturn(welcomeResource);
        when(inAppStorage.getResource(eq(CODE_2))).thenReturn(updateResource);
        when(inAppStorage.getResource(eq(CODE_3))).thenReturn(recoveryResource);
    }

    @Test
    public void resetBusinessCasesFrequencyCapping() {
        SharedPreferences.Editor editorMock = mock(SharedPreferences.Editor.class);
        when(sharedPreferencesMock.edit()).thenReturn(editorMock);
        when(editorMock.clear()).thenReturn(editorMock);

        businessCasesManager.resetBusinessCasesFrequencyCapping();

        Mockito.verify(editorMock).clear();
        Mockito.verify(editorMock).apply();
    }
}