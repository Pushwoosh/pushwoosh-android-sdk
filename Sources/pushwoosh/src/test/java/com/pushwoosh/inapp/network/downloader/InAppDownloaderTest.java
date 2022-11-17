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

package com.pushwoosh.inapp.network.downloader;

import androidx.annotation.NonNull;

import com.pushwoosh.BaseTest;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Created by aevstefeev on 05/03/2018.
 */

@Ignore
public class InAppDownloaderTest extends BaseTest {
    public static final String CODE_1 = "code1";
    public static final String CODE_2 = "code2";
    public static final String CODE_3 = "code3";

    private InAppDownloader inAppDownloader;

    @Mock
    private File fileMock1;
    @Mock
    private File fileMock2;
    @Mock
    private File fileMock3;
    @Mock
    InAppFolderProvider inAppFolderProvider;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        when(inAppFolderProvider.getInAppFolder(CODE_1)).thenReturn(fileMock1);
        when(inAppFolderProvider.getInAppFolder(CODE_2)).thenReturn(fileMock2);
        when(inAppFolderProvider.getInAppFolder(CODE_3)).thenReturn(fileMock3);
        when(fileMock1.exists()).thenReturn(true);

        inAppDownloader = new InAppDownloader(inAppFolderProvider);
    }

    @Test
    public void downloadAndDeploy() throws Exception {
        List<Resource> resourceList = getResourceList();
        DownloadResult downloadResult = inAppDownloader.downloadAndDeploy(resourceList);

        Assert.assertEquals(1,downloadResult.getSuccess().size());
        Assert.assertEquals(2,downloadResult.getFailed().size());

    }

    @NonNull
    private List<Resource> getResourceList() {
        List<Resource> resourceList = new ArrayList<>();
        resourceList.add(new Resource(CODE_1, true));
        resourceList.add(new Resource(CODE_2, true));
        resourceList.add(new Resource(CODE_3, false));
        return resourceList;
    }

    @Test
    public void isDownloading() throws Exception {
        Resource resource = new Resource("code1", true);
        Resource resource2 = new Resource("code4", true);

        inAppDownloader.downloadAndDeploy(getResourceList());

        boolean result = inAppDownloader.isDownloading(resource);
        Assert.assertEquals(true, result);

        boolean result2 = inAppDownloader.isDownloading(resource2);
        Assert.assertEquals(false, result2);
    }

    @Test
    public void removeResourceFiles() throws Exception {
    }

}