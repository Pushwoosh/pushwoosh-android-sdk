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
 * Regression guard for crash-pushmessage-getmessagecode-null-hash.
 *
 * Third escape hatch on the same shared root as crash-pushmessage-getcampaignid-null-hash and
 * crash-pushmessage-getmessageid-null-hash: a remote-API push without a "p" field leaves pushHash
 * null, and getMessageCode() returns HashDecoder.parseMessageHash(pushHash)[1] — which used to
 * deref null via hash.split(...) -> NPE on the host app's onMessageReceived / onMessageOpened stack.
 * The shared null-guard in parseMessageHash (returning the documented {"0", "", "0"} fallback) makes
 * getMessageCode() return the empty message code instead of crashing.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class PushMessageMessageCodeNullHashTest {

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
     * getMessageCode() returns the empty message code (the documented sentinel) instead of throwing
     * an NPE in HashDecoder.parseMessageHash.
     */
    @Test
    public void getMessageCode_pushWithoutHash_returnsEmpty() {
        Bundle remoteApiPush = new Bundle();
        PushMessage pushMessage = new PushMessage(remoteApiPush);

        assertEquals("", pushMessage.getMessageCode());
    }

    /**
     * Negative control (unchanged from the repro): a push WITH a "p" field has a non-null hash, so
     * parseMessageHash().split() runs and getMessageCode() returns normally. "abc" is a short hash
     * (<= 3 parts) so parseMessageHash returns the {"0","","0"} fallback -> code == "".
     */
    @Test
    public void getMessageCode_pushWithHash_doesNotThrow() {
        Bundle pushWithHash = new Bundle();
        pushWithHash.putString("p", "abc");
        PushMessage pushMessage = new PushMessage(pushWithHash);

        assertEquals("", pushMessage.getMessageCode());
    }
}
