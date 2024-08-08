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


import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.internal.utils.TimeProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class BusinessCaseTest {
    public static final int TIMEOUT = 20000;
    @Mock
    private SharedPreferences sharedPreferencesMock;
    @Mock
    private TimeProvider timeProvider;
    @Mock
    private BusinessCase.BusinessCaseCallback callbackMock;
    @Mock
    private SharedPreferences.Editor editor;
    @Mock
    private InAppStorage inAppStorage;

    private BusinessCase.Condition conditionTrue = () -> true;
    private BusinessCase.Condition conditionFalser = () -> false;

    private long currentTime = 123L;
    private String inAppId = "inAppId1";


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(timeProvider.getCurrentTime()).thenReturn(currentTime);

        when(sharedPreferencesMock.edit()).thenReturn(editor);
        when(editor.putLong(BusinessCasesManager.WELCOME_CASE, currentTime)).thenReturn(editor);

        InAppModule.setInAppStorage(inAppStorage);
        Mockito.when(inAppStorage.getResource(inAppId)).thenReturn(new Resource("code1", "url1", "hash1", 1L, InAppLayout.DIALOG, null, false, 3, BusinessCasesManager.WELCOME_CASE, ""));

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void triggerConditionFalse() {
        BusinessCase businessCase = new BusinessCase(BusinessCasesManager.WELCOME_CASE, BusinessCasesManager.NO_CAPPING, sharedPreferencesMock, conditionFalser, timeProvider);
        businessCase.trigger(callbackMock);

        verify(callbackMock).onShowFail(BusinessCaseResult.CONDITION_NOT_SATISFIED);
    }

    @Test
    public void triggerCapExceeded() {
        when(timeProvider.getCurrentDate()).thenReturn(new Date(0));
        when(sharedPreferencesMock.getLong(BusinessCasesManager.WELCOME_CASE, Long.MIN_VALUE)).thenReturn(0L);

        BusinessCase businessCase = new BusinessCase(BusinessCasesManager.WELCOME_CASE, BusinessCasesManager.DEFAULT_CAPPING, sharedPreferencesMock, conditionTrue, timeProvider);
        businessCase.trigger(callbackMock);

        verify(callbackMock).onShowFail(BusinessCaseResult.TRIGGER_CAP_EXCEEDED);
    }

    @Test
    @Ignore("Fix after ArgumentCaptor is figured out")
    public void triggerNoCappingLoadingFailed() throws InterruptedException {
        AtomicReference<BusinessCaseResult> result = new AtomicReference<>();
        BusinessCase.BusinessCaseCallback callback = result::set;


        when(inAppStorage.getResource(inAppId)).thenReturn(null);

        BusinessCase businessCase = new BusinessCase(BusinessCasesManager.WELCOME_CASE, BusinessCasesManager.NO_CAPPING, sharedPreferencesMock, conditionTrue, timeProvider);
        businessCase.setInAppId(inAppId);
        businessCase.trigger(callback);

        Assert.assertEquals(result.get(), BusinessCaseResult.LOADING_FAILED);
    }

    @Test
    @Ignore("Fix after ArgumentCaptor is figured out")
    public void triggerNoCappingSuccessShow() throws InterruptedException {
        AtomicReference<BusinessCaseResult> result = new AtomicReference<>();
        BusinessCase.BusinessCaseCallback callback = result::set;

        BusinessCase businessCase = new BusinessCase(BusinessCasesManager.WELCOME_CASE, BusinessCasesManager.NO_CAPPING, sharedPreferencesMock, conditionTrue, timeProvider);

        businessCase.setInAppId(inAppId);
        businessCase.trigger(callback);

        verify(editor).apply();
        Assert.assertNull(result.get());
    }


}