package com.pushwoosh.huawei.internal.mapper;

import android.os.Bundle;

import com.huawei.hms.push.RemoteMessage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RemoteMessageMapperTest {

    @Test
    public void mapToBundle_dataPassthrough() {
        Map<String, String> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");

        RemoteMessage remoteMessage = Mockito.mock(RemoteMessage.class);
        Mockito.when(remoteMessage.getDataOfMap()).thenReturn(data);
        Mockito.when(remoteMessage.getCollapseKey()).thenReturn(null);

        Bundle bundle = RemoteMessageMapper.mapToBundle(remoteMessage);

        Assert.assertEquals("value1", bundle.getString("key1"));
        Assert.assertEquals("value2", bundle.getString("key2"));
    }

    @Test
    public void mapToBundle_nullData_returnsEmptyBundle() {
        RemoteMessage remoteMessage = Mockito.mock(RemoteMessage.class);
        Mockito.when(remoteMessage.getDataOfMap()).thenReturn(null);
        Mockito.when(remoteMessage.getCollapseKey()).thenReturn(null);

        Bundle bundle = RemoteMessageMapper.mapToBundle(remoteMessage);

        Assert.assertNotNull(bundle);
        Assert.assertTrue(bundle.isEmpty());
    }

    @Test
    public void mapToBundle_collapseKeyPresent_setsPwMsgTag() {
        RemoteMessage remoteMessage = Mockito.mock(RemoteMessage.class);
        Mockito.when(remoteMessage.getDataOfMap()).thenReturn(new HashMap<>());
        Mockito.when(remoteMessage.getCollapseKey()).thenReturn("my-collapse-key");

        Bundle bundle = RemoteMessageMapper.mapToBundle(remoteMessage);

        Assert.assertEquals("my-collapse-key", bundle.getString("pw_msg_tag"));
    }

    @Test
    public void mapToBundle_collapseKeyAndExplicitTag_keepsExplicit() {
        Map<String, String> data = new HashMap<>();
        data.put("pw_msg_tag", "explicit-tag");

        RemoteMessage remoteMessage = Mockito.mock(RemoteMessage.class);
        Mockito.when(remoteMessage.getDataOfMap()).thenReturn(data);
        Mockito.when(remoteMessage.getCollapseKey()).thenReturn("collapse-key");

        Bundle bundle = RemoteMessageMapper.mapToBundle(remoteMessage);

        Assert.assertEquals("explicit-tag", bundle.getString("pw_msg_tag"));
    }

    @Test
    public void mapToBundle_collapseKeyNull_doesNotSetTag() {
        RemoteMessage remoteMessage = Mockito.mock(RemoteMessage.class);
        Mockito.when(remoteMessage.getDataOfMap()).thenReturn(new HashMap<>());
        Mockito.when(remoteMessage.getCollapseKey()).thenReturn(null);

        Bundle bundle = RemoteMessageMapper.mapToBundle(remoteMessage);

        Assert.assertNull(bundle.getString("pw_msg_tag"));
    }

    @Test
    public void mapToBundle_collapseKeyEmpty_doesNotSetTag() {
        RemoteMessage remoteMessage = Mockito.mock(RemoteMessage.class);
        Mockito.when(remoteMessage.getDataOfMap()).thenReturn(new HashMap<>());
        Mockito.when(remoteMessage.getCollapseKey()).thenReturn("");

        Bundle bundle = RemoteMessageMapper.mapToBundle(remoteMessage);

        Assert.assertNull(bundle.getString("pw_msg_tag"));
    }
}
