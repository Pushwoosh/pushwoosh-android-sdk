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

/**
 * Regression guard for crash-pushmessage-getcampaignid-null-hash.
 *
 * A remote-API push payload omits the "p" field, so PushBundleDataProvider.getPushHash() returns
 * null and PushMessage.pushHash is null. All three hash-derived getters funnel through
 * HashDecoder.parseMessageHash(pushHash), which used to deref a null hash with hash.split(...) ->
 * NullPointerException on the host app's onMessageReceived / onMessageOpened stack. The fix
 * null-guards parseMessageHash to return the documented {"0", "", "0"} fallback, so the getters now
 * honor their "0 / empty if not available" javadoc contract instead of crashing.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class PushMessageCampaignIdNullHashTest {

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

    /**
     * Verifies that a remote-API push without a "p" field (pushHash == null) no longer crashes:
     * getCampaignId() returns 0, the documented "0 if not available" value, instead of throwing
     * an NPE in HashDecoder.parseMessageHash.
     */
    @Test
    public void getCampaignId_pushWithoutHash_returnsZero() {
        Bundle remoteApiPush = new Bundle();
        PushMessage pushMessage = new PushMessage(remoteApiPush);

        assertEquals(0L, pushMessage.getCampaignId());
    }

    /**
     * Verifies the sibling getter getMessageId() — same null-hash root, same shared
     * parseMessageHash(pushHash) — is likewise graceful and returns 0.
     */
    @Test
    public void getMessageId_pushWithoutHash_returnsZero() {
        Bundle remoteApiPush = new Bundle();
        PushMessage pushMessage = new PushMessage(remoteApiPush);

        assertEquals(0L, pushMessage.getMessageId());
    }

    /**
     * Verifies the sibling getter getMessageCode() — same null-hash root — returns the documented
     * empty message code instead of throwing.
     */
    @Test
    public void getMessageCode_pushWithoutHash_returnsEmpty() {
        Bundle remoteApiPush = new Bundle();
        PushMessage pushMessage = new PushMessage(remoteApiPush);

        assertEquals("", pushMessage.getMessageCode());
    }

    /**
     * Negative control (unchanged from the repro): a push WITH a "p" field has a non-null hash, so
     * parseMessageHash().split() runs and getCampaignId() returns normally. "abc" is a short hash
     * that falls back to 0, proving the null hash was the necessary crash condition.
     */
    @Test
    public void getCampaignId_pushWithHash_doesNotThrow() {
        Bundle pushWithHash = new Bundle();
        pushWithHash.putString("p", "abc");
        PushMessage pushMessage = new PushMessage(pushWithHash);

        assertEquals(0L, pushMessage.getCampaignId());
    }
}
