/*
 *
 * Copyright (c) 2026. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.inbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pushwoosh.function.Callback;
import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.exception.InboxMessagesException;
import com.pushwoosh.inbox.internal.PushwooshInboxModule;
import com.pushwoosh.inbox.internal.data.InboxMessageStatus;
import com.pushwoosh.inbox.repository.InboxRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * SDK-997: inbox card elements that navigate on their own (inline buttons, carousel slides, the
 * video poster) must report the message as opened without performing the message's own action.
 *
 * <p>The open counter in the Control Panel is fed by {@code pushStatAllowed=true} inside
 * {@link InboxRepository#updateStatus(Map, boolean, Callback)} — the private {@code changeStatus}
 * helper and the single-id overload both hardcode {@code false}, so a "mark opened" built on top
 * of them would update the status and silently drop the statistics.
 *
 * <p>The repository is installed straight into {@link PushwooshInboxModule}'s cache field rather
 * than mocked statically: a {@code mockStatic} of the module leaks across Robolectric's shared
 * sandbox in this module and makes {@code DeepLinkActionSecurityExceptionTest} flake.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class PushwooshInboxMarkMessageOpenedTest {

    private static final String CODE = "code-1";

    private InboxRepository repository;

    @Before
    public void setUp() throws Exception {
        repository = mock(InboxRepository.class);
        installRepository(repository);
    }

    @After
    public void tearDown() throws Exception {
        installRepository(null);
    }

    private static void installRepository(InboxRepository repository) throws Exception {
        Field field = PushwooshInboxModule.class.getDeclaredField("sInboxRepository");
        field.setAccessible(true);
        field.set(null, repository);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Map<String, InboxMessageStatus>> mapCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Callback<InboxMessage, InboxMessagesException>> callbackCaptor() {
        return ArgumentCaptor.forClass(Callback.class);
    }

    @Test
    public void markMessageOpened_sendsOpenStatusWithPushStatAllowed() {
        PushwooshInbox.markMessageOpened(CODE);

        ArgumentCaptor<Map<String, InboxMessageStatus>> map = mapCaptor();
        ArgumentCaptor<Boolean> pushStatAllowed = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Callback<InboxMessage, InboxMessagesException>> callback = callbackCaptor();
        verify(repository).updateStatus(map.capture(), pushStatAllowed.capture(), callback.capture());

        assertEquals(1, map.getValue().size());
        assertEquals(InboxMessageStatus.OPEN, map.getValue().get(CODE));
        assertTrue(
                "open statistics must be reported, otherwise the Control Panel counter stays flat",
                pushStatAllowed.getValue());
        assertNull("a success callback is the only route to the payload navigation", callback.getValue());
    }

    @Test
    public void performAction_stillRequestsOpenWithNavigationCallback() {
        PushwooshInbox.performAction(CODE);

        ArgumentCaptor<Map<String, InboxMessageStatus>> map = mapCaptor();
        ArgumentCaptor<Boolean> pushStatAllowed = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Callback<InboxMessage, InboxMessagesException>> callback = callbackCaptor();
        verify(repository).updateStatus(map.capture(), pushStatAllowed.capture(), callback.capture());

        assertEquals(InboxMessageStatus.OPEN, map.getValue().get(CODE));
        assertTrue(pushStatAllowed.getValue());
        assertNotNull("performAction keeps performing the message's own action", callback.getValue());
    }

    @Test
    public void readMessage_stillRequestsReadWithoutStatistics() {
        PushwooshInbox.readMessage(CODE);

        ArgumentCaptor<Map<String, InboxMessageStatus>> map = mapCaptor();
        ArgumentCaptor<Boolean> pushStatAllowed = ArgumentCaptor.forClass(Boolean.class);
        verify(repository)
                .updateStatus(
                        map.capture(),
                        pushStatAllowed.capture(),
                        ArgumentCaptor.forClass(Callback.class).capture());

        assertEquals(InboxMessageStatus.READ, map.getValue().get(CODE));
        assertEquals(Boolean.FALSE, pushStatAllowed.getValue());
    }
}
