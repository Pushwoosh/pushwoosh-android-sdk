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

package com.pushwoosh.inapp.mapper;

import static org.mockito.Mockito.mock;

import com.pushwoosh.BuildConfig;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.RepositoryTestManager;


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class ResourceMapperTest {
    String htmlData;
    Map<String,String> tags = new HashMap<>();
    String configJson;

    @Before
    public void setUp() throws Exception {
        tags.put("DynamicContentValue", "DynamicContentValue");
        tags.put("Tag2", "Tag2");
        tags.put("Tag3", "Tag3");
        tags.put("tag4", "{DynamicContent|Type|Value");
        tags.put("tag5", "{DynamicContent|Type|}");

        htmlData = "{{LocalizedString1|text|testvalue1}}, {{LocalizedString2|text|testvalue2}}, {{tag3|templateValue3}}," +
                " {DynamicContent|String|DefaultDynamicValue}, " +
                "{DynamicContent|String|}, {DynamicContentValue|String|Default}";
        configJson = "{\"default_language\":\"en\", \"localization\": " +
                "{\"en\": {\"LocalizedString1\": \"LocalizedStringValue1\", \"LocalizedString2\": \"{Tag2|CapitalizeFirst|Tag2Value}\"}}}";

        Config configMock = MockConfig.createMock();
        AndroidPlatformModule.init(RuntimeEnvironment.application, true);
        RegistrationPrefs registrationPrefs = RepositoryTestManager.createRegistrationPrefs(configMock, mock(DeviceRegistrar.class));
        RepositoryModule.setRegistrationPreferences(registrationPrefs);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void map() throws Exception {
        InAppFolderProvider inAppFolderProvider = Mockito.mock(InAppFolderProvider.class);

        File file = File.createTempFile("123",".html");

        Mockito.when(inAppFolderProvider.getInAppFolder("123")).thenReturn(new File("test/inApp1"));
        Mockito.when(inAppFolderProvider.getInAppHtmlFile("123")).thenReturn(file);

        ResourceMapper resourceMapper = new ResourceMapper(inAppFolderProvider);
        Resource resource = new Resource("123", true);

        HtmlData htmlData = resourceMapper.map(resource);

        Assert.assertEquals("123", htmlData.getCode());
        Assert.assertNotNull(htmlData.getUrl());
        Assert.assertEquals("", htmlData.getHtmlContent());
    }

    @Test
    public void getHtmlDataTest() throws Exception {
        File file = File.createTempFile("123",".html");
        FileUtils.writeFile(file,htmlData);

        File configFile = File.createTempFile("pushwoosh",".json");
        FileUtils.writeFile(configFile,configJson);

        InAppFolderProvider inAppFolderProvider = Mockito.mock(InAppFolderProvider.class);
        Mockito.when(inAppFolderProvider.getInAppFolder("123")).thenReturn(new File("test/inApp1"));
        Mockito.when(inAppFolderProvider.getInAppHtmlFile("123")).thenReturn(file);
        Mockito.when(inAppFolderProvider.getConfigFile("123")).thenReturn(configFile);

        ResourceMapper resourceMapper = new ResourceMapper(inAppFolderProvider);
        String result = resourceMapper.getHtmlData("123",tags);
        Assert.assertEquals("LocalizedStringValue1, Tag2, tag3, DefaultDynamicValue, , DynamicContentValue\n",result);
    }
}
