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

package com.pushwoosh.repository;


import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.network.RequestStorage;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceJsonObjectValue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by aevstefeev on 26/03/2018.
 */
public class PushwooshRepositoryTest {

    private PushwooshRepository pushwooshRepository;

    private RequestManager requestManager;
    private SendTagsProcessor sendTagsProcessor;
    private RegistrationPrefs registrationPrefs;
    private NotificationPrefs notificationPrefs;
    private RequestStorage requestStorage;

    @Before
    public void setUp() {
        requestManager = Mockito.mock(RequestManager.class);
        sendTagsProcessor = Mockito.mock(SendTagsProcessor.class);

        registrationPrefs = Mockito.mock(RegistrationPrefs.class);
        PreferenceBooleanValue preferenceBooleanValue = Mockito.mock(PreferenceBooleanValue.class);
        when(preferenceBooleanValue.get()).thenReturn(false);
        when(registrationPrefs.setTagsFailed()).thenReturn(preferenceBooleanValue);
        when(registrationPrefs.communicationEnable()).thenReturn(mock(PreferenceBooleanValue.class));
        when(registrationPrefs.removeAllDeviceData()).thenReturn(mock(PreferenceBooleanValue.class));

        notificationPrefs = Mockito.mock(NotificationPrefs.class);

        when(notificationPrefs.tags()).thenReturn(Mockito.mock(PreferenceJsonObjectValue.class));

        requestStorage = Mockito.mock(RequestStorage.class);

        ServerCommunicationManager serverCommunicationManager = mock(ServerCommunicationManager.class);
        when(serverCommunicationManager.isServerCommunicationAllowed()).thenReturn(true);
        pushwooshRepository = new PushwooshRepository(
                requestManager,
                sendTagsProcessor,
                registrationPrefs,
                notificationPrefs,
                requestStorage,
                serverCommunicationManager);
    }


    @Test
    public void testRemoveTag() throws Exception {
        pushwooshRepository.removeAllDeviceData();
        verify(notificationPrefs.tags()).set(null);
        verify(registrationPrefs.removeAllDeviceData()).set(true);
    }

    @Test
    public void enableCommunication() throws Exception {
        pushwooshRepository.communicationEnabled(true);
        verify(registrationPrefs.communicationEnable()).set(true);
    }

    @Test
    public void disableCommunication() throws Exception {
        pushwooshRepository.communicationEnabled(true);
        verify(registrationPrefs.communicationEnable()).set(true);
    }

}