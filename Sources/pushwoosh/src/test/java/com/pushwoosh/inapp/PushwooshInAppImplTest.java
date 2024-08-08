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

package com.pushwoosh.inapp;

import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Incubating;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class PushwooshInAppImplTest {
    private PlatformTestManager platformTestManager;

    private PushwooshInAppImpl pushwooshInApp;
    private InAppRepository inAppRepositoryMock;
    private InAppStorage inAppStorage;
    private RegistrationPrefs registrationPrefs;

    @Mock
    PushwooshInAppService pushwooshInAppService;




    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        ServerCommunicationManager serverCommunicationManager = mock(ServerCommunicationManager.class);
        when(serverCommunicationManager.isServerCommunicationAllowed()).thenReturn(true);
        pushwooshInApp = new PushwooshInAppImpl(pushwooshInAppService, serverCommunicationManager);

        registrationPrefs = platformTestManager.getRegistrationPrefs();
        inAppRepositoryMock = platformTestManager.getInAppRepositoryMock();
        inAppStorage = platformTestManager.getInAppStorage();
        Resource resource = new Resource("r-12345-ABCDE", false);
        Mockito.when(inAppStorage.getResourceGDPRConsent()).thenReturn(resource);

        Resource resource2 = new Resource("r-ABCDE-12345", false);
        Mockito.when(inAppStorage.getResourceGDPRDeletion()).thenReturn(resource2);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    @Ignore()
    public void showGDPRConsentInApp() {
        pushwooshInApp.showGDPRConsentInApp();
        Mockito.verify(inAppStorage).getResourceGDPRConsent();
    }

    @Test
    @Ignore
    public void showGDPRDeletionInApp() {
        pushwooshInApp.showGDPRDeletionInApp();
        Mockito.verify(inAppStorage).getResourceGDPRDeletion();
    }

    @Test
    public void reloadInApps() {
        pushwooshInApp.reloadInApps(null);
        Mockito.verify(inAppRepositoryMock).loadInApps();
    }
}