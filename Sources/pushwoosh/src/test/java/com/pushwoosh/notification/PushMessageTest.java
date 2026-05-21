package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;

import android.os.Bundle;

import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class PushMessageTest {

    private static final String KEY_PUSH_HASH = "p";
    private static final String KEY_METADATA = "md";

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
    }

    @After
    public void tearDown() {
        platformTestManager.tearDown();
    }

    private static Bundle bundleWith(String hash, String metadata) {
        Bundle bundle = new Bundle();
        if (hash != null) {
            bundle.putString(KEY_PUSH_HASH, hash);
        }
        if (metadata != null) {
            bundle.putString(KEY_METADATA, metadata);
        }
        return bundle;
    }

    @Test
    public void getPushwooshNotificationId_metadataHasUid_returnsUidLong() {
        // 99999999999 is larger than Integer.MAX_VALUE so org.json parses it as Long
        // and Bundle.getLong("uid", -1) returns the value (Bundle does not auto-promote Integer).
        Bundle bundle = bundleWith(null, "{\"uid\": 99999999999}");

        PushMessage pushMessage = new PushMessage(bundle);

        assertEquals(99999999999L, pushMessage.getPushwooshNotificationId());
    }

    @Test
    public void getPushwooshNotificationId_metadataAbsent_returnsMinusOne() {
        PushMessage pushMessage = new PushMessage(bundleWith(null, null));

        assertEquals(-1L, pushMessage.getPushwooshNotificationId());
    }

    @Test
    public void getCampaignIdAndMessageId_validHash_decodesBase62Parts() {
        // Hash format: <prefix>_<campaignBase62>_<messageBase62>_<code>
        // alphabetDecode("1") == 1, alphabetDecode("2") == 2.
        // parseMessageHash returns [messageID, code, campaignID].
        // PushMessage.getCampaignId reads parts[2] (campaign), getMessageId reads parts[0] (message).
        Bundle bundle = bundleWith("p_1_2_code", null);

        PushMessage pushMessage = new PushMessage(bundle);

        assertEquals(1L, pushMessage.getCampaignId());
        assertEquals(2L, pushMessage.getMessageId());
    }

    @Test
    public void getCampaignIdAndMessageId_shortHash_fallsBackToZero() {
        // Hash without enough underscore-separated parts triggers the fallback
        // branch in HashDecoder.parseMessageHash returning {"0", "", "0"}.
        Bundle bundle = bundleWith("abc", null);

        PushMessage pushMessage = new PushMessage(bundle);

        assertEquals(0L, pushMessage.getCampaignId());
        assertEquals(0L, pushMessage.getMessageId());
    }
}
