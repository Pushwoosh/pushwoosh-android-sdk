package com.pushwoosh.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class SdkStateProviderTest {

    @Mock
    private Runnable task1;

    @Mock
    private Runnable task2;

    @Mock
    private Runnable taskThatThrows;

    private AutoCloseable mocks;
    private SdkStateProvider provider;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        provider = SdkStateProvider.getInstance();
        provider.resetForTesting();
    }

    @After
    public void tearDown() throws Exception {
        provider.resetForTesting();
        mocks.close();
    }

    // Verifies that a task runs immediately when SDK state is READY.
    @Test
    public void executeOrQueue_whenReady_runsTaskImmediately() {
        provider.setReady();

        provider.executeOrQueue(task1);

        verify(task1, times(1)).run();
        assertEquals(SdkStateProvider.SdkState.READY, provider.getCurrentState());
    }

    // Verifies that tasks queued during INITIALIZING are drained in order on setReady().
    @Test
    public void executeOrQueue_whenInitializing_queuesAndDrainsOnSetReady() {
        provider.executeOrQueue(task1);
        provider.executeOrQueue(task2);

        verify(task1, never()).run();
        verify(task2, never()).run();

        provider.setReady();

        InOrder inOrder = Mockito.inOrder(task1, task2);
        inOrder.verify(task1).run();
        inOrder.verify(task2).run();
        assertTrue(provider.isReady());
    }

    // Verifies that setReady() transitions state to READY when no tasks are queued.
    @Test
    public void setReady_withEmptyQueue_transitionsToReady() {
        provider.setReady();

        assertEquals(SdkStateProvider.SdkState.READY, provider.getCurrentState());
        assertTrue(provider.isReady());
    }

    // Verifies that a task is ignored when SDK state is ERROR.
    @Test
    public void executeOrQueue_whenError_ignoresTask() {
        provider.setError();

        provider.executeOrQueue(task1);

        verify(task1, never()).run();
        assertEquals(SdkStateProvider.SdkState.ERROR, provider.getCurrentState());
        assertFalse(provider.isReady());
    }

    // Verifies that setError() drops queued tasks and subsequent setReady() does not run them.
    @Test
    public void setError_dropsQueuedTasksAndBlocksSetReady() {
        provider.executeOrQueue(task1);
        provider.executeOrQueue(task2);

        provider.setError();
        provider.setReady();

        verify(task1, never()).run();
        verify(task2, never()).run();
        assertEquals(SdkStateProvider.SdkState.ERROR, provider.getCurrentState());
    }

    // Verifies that setReady() is idempotent and does not re-run tasks executed after first setReady().
    @Test
    public void setReady_idempotent_doesNotRerunTasks() {
        provider.setReady();
        provider.executeOrQueue(task1);

        provider.setReady();

        verify(task1, times(1)).run();
        assertEquals(SdkStateProvider.SdkState.READY, provider.getCurrentState());
    }

    // Verifies that setReady() does not lift SDK out of ERROR state.
    @Test
    public void setReady_afterSetError_doesNotRecoverState() {
        provider.setError();

        provider.setReady();

        assertEquals(SdkStateProvider.SdkState.ERROR, provider.getCurrentState());
        assertFalse(provider.isReady());
    }

    // Verifies that an exception from one queued task does not prevent execution of other tasks.
    @Test
    public void setReady_swallowsTaskException_andContinuesDraining() {
        doThrow(new RuntimeException("boom")).when(taskThatThrows).run();

        provider.executeOrQueue(taskThatThrows);
        provider.executeOrQueue(task1);

        provider.setReady();

        verify(taskThatThrows).run();
        verify(task1).run();
        assertEquals(SdkStateProvider.SdkState.READY, provider.getCurrentState());
    }
}
