package com.pushwoosh;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import com.pushwoosh.exception.SetUserIdException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.event.UserIdChangeRefusedEvent;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class UserIdChangeSignalsTest {
    private PlatformTestManager platformTestManager;
    private RegistrationPrefs prefs;
    private InAppRepository repository;
    private final List<String> idAtRefusal = new ArrayList<>();
    private final EventListener<UserIdChangeRefusedEvent> listener =
            e -> idAtRefusal.add(prefs.userId().get());

    @Before
    public void setUp() throws Exception {
        Config configMock = MockConfig.createMock();
        platformTestManager = new PlatformTestManager(configMock);
        platformTestManager.onApplicationCreated();
        prefs = platformTestManager.getRegistrationPrefs();
        repository = Mockito.mock(InAppRepository.class);
        setField("registrationPrefs", prefs);
        setField("inAppRepository", repository);
        setField("isInitialized", true);
        SdkStateProvider.getInstance().setReady();
        EventBus.subscribe(UserIdChangeRefusedEvent.class, listener);
    }

    @After
    public void tearDown() throws Exception {
        EventBus.unsubscribe(UserIdChangeRefusedEvent.class, listener);
        SdkStateProvider.getInstance().resetForTesting();
        platformTestManager.tearDown();
    }

    private static void setField(String name, Object value) throws Exception {
        Field field = Pushwoosh.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(Pushwoosh.getInstance(), value);
    }

    private void answerSetUserId(Result<Boolean, SetUserIdException> result) {
        Mockito.doAnswer(invocation -> {
                    Callback<Boolean, SetUserIdException> callback = invocation.getArgument(1);
                    callback.process(result);
                    return null;
                })
                .when(repository)
                .setUserId(Mockito.eq("new"), Mockito.any());
    }

    // Verifies that a refused registerUser reverts the id and only then announces the refusal.
    @Test
    public void testRefusedUserChangeIsAnnouncedAfterRevert() {
        prefs.userId().set("old");
        answerSetUserId(Result.fromException(new SetUserIdException("offline")));

        Pushwoosh.getInstance().setUserId("new");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertThat(idAtRefusal, contains("old"));
    }

    // Verifies that a confirmed registerUser keeps the new id and announces no refusal.
    @Test
    public void testConfirmedUserChangeIsNotAnnouncedAsRefused() {
        prefs.userId().set("old");
        answerSetUserId(Result.fromData(true));

        Pushwoosh.getInstance().setUserId("new");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertThat(idAtRefusal, empty());
        assertThat(prefs.userId().get(), org.hamcrest.Matchers.equalTo("new"));
    }

    // Verifies that each refused attempt is announced, since core has no settled-id notion.
    @Test
    public void testEveryRefusedAttemptIsAnnounced() {
        prefs.userId().set("old");
        answerSetUserId(Result.fromException(new SetUserIdException("offline")));

        Pushwoosh.getInstance().setUserId("new");
        Pushwoosh.getInstance().setUserId("new");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertThat(idAtRefusal, hasSize(2));
    }
}
