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
 * Regression guard for crash-pushmessage-getmessageid-null-hash.
 *
 * Sibling of crash-pushmessage-getcampaignid-null-hash: a remote-API push without a "p" field
 * leaves pushHash null, and getMessageId() funnels through the same shared
 * HashDecoder.parseMessageHash(pushHash) which used to deref null via hash.split(...) -> NPE on the
 * host app's onMessageReceived / onMessageOpened stack. The shared null-guard in parseMessageHash
 * (returning the documented {"0", "", "0"} fallback) makes getMessageId() honor its "0 if not
 * available" javadoc contract instead of crashing.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class PushMessageMessageIdNullHashTest {

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
     * getMessageId() returns 0, the documented "0 if not available" value, instead of throwing an
     * NPE in HashDecoder.parseMessageHash.
     */
    @Test
    public void getMessageId_pushWithoutHash_returnsZero() {
        Bundle remoteApiPush = new Bundle();
        PushMessage pushMessage = new PushMessage(remoteApiPush);

        assertEquals(0L, pushMessage.getMessageId());
    }

    /**
     * Negative control (unchanged from the repro): a push WITH a "p" field has a non-null hash, so
     * parseMessageHash().split() runs and getMessageId() returns normally. "abc" is a short hash
     * that falls back to 0, proving the null hash was the necessary crash condition.
     */
    @Test
    public void getMessageId_pushWithHash_doesNotThrow() {
        Bundle pushWithHash = new Bundle();
        pushWithHash.putString("p", "abc");
        PushMessage pushMessage = new PushMessage(pushWithHash);

        assertEquals(0L, pushMessage.getMessageId());
    }
}
