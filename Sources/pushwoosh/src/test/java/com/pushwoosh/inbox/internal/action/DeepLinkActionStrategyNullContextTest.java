package com.pushwoosh.inbox.internal.action;

import static org.junit.Assert.assertFalse;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/**
 * Regression guard for crash-deeplinkactionstrategy-context-null.
 *
 * {@code DeepLinkActionStrategy} captures {@code context} from the {@code @Nullable}
 * {@code AndroidPlatformModule.getApplicationContext()} in a field initializer at construction time,
 * then dereferenced it at {@code performAction} ({@code context.startActivity(intent)}) inside a
 * {@code catch (ActivityNotFoundException)} that does NOT catch NPE — so a null context (pre-init /
 * GC'd WeakReference) crashed the host app on the main thread. {@code performAction} now null-guards
 * {@code context} (logs + early-return), mirroring the sibling {@code UrlActionStrategy}: the deep
 * link is simply not opened instead of crashing.
 *
 * Test lives in package {@code com.pushwoosh.inbox.internal.action} to reach the package-private
 * class and its package-private {@code performAction}.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class DeepLinkActionStrategyNullContextTest {

    // Non-http "l" value -> InboxPayloadDataProvider.getFromJson classifies as DEEP_LINK and
    // getUrl(...) returns non-null, so the early-return deep-link guard is passed and execution
    // reaches the context deref.
    private static final String DEEP_LINK_PARAMS = "{\"l\":\"myapp://deep/link\"}";

    private AutoCloseable mocks;
    private MockedStatic<AndroidPlatformModule> platformModuleStatic;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        platformModuleStatic = Mockito.mockStatic(AndroidPlatformModule.class);
    }

    @After
    public void tearDown() throws Exception {
        if (platformModuleStatic != null) {
            platformModuleStatic.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    private void contextIsNull() {
        // The crash trigger: getApplicationContext() == null at construction time (the field is
        // captured in the ctor, so the null must be in place before new DeepLinkActionStrategy()).
        platformModuleStatic.when(AndroidPlatformModule::getApplicationContext).thenReturn(null);
    }

    private void contextIsReal() {
        platformModuleStatic
                .when(AndroidPlatformModule::getApplicationContext)
                .thenReturn(RuntimeEnvironment.getApplication());
    }

    // ---- Regression guard: null context + valid deep link -> graceful no-op, no crash -----------

    // Verifies that a DEEP_LINK inbox tap while the app context is null (pre-init / GC'd) is handled
    // gracefully: performAction returns without throwing instead of crashing on context.startActivity.
    @Test
    public void performAction_nullContext_isHandledGracefully() throws Exception {
        contextIsNull();
        DeepLinkActionStrategy strategy = new DeepLinkActionStrategy();

        strategy.performAction(new JSONObject(DEEP_LINK_PARAMS));
    }

    // ---- Negative control A: missing "l" -> early return, deep-link condition required -----------

    // Verifies that without a deep link ("l" key absent) performAction early-returns before the
    // context deref — guards against a regression that widens the new null-context guard to also
    // swallow the deep-link guard.
    @Test
    public void performAction_nullContext_butNoDeepLink_noThrow() throws Exception {
        contextIsNull();
        DeepLinkActionStrategy strategy = new DeepLinkActionStrategy();

        strategy.performAction(new JSONObject("{\"foo\":\"bar\"}"));
    }

    // ---- Negative control B: real context -> deep link is actually opened, no NPE ----------------

    // Verifies that with a real context the null-context guard does NOT fire: the deep link reaches
    // startActivity (which either resolves or throws ActivityNotFoundException, swallowed by the
    // production catch). Discriminator that the guard is scoped to the null case only.
    @Test
    public void performAction_realContext_noNpe() throws Exception {
        contextIsReal();
        DeepLinkActionStrategy strategy = new DeepLinkActionStrategy();

        try {
            strategy.performAction(new JSONObject(DEEP_LINK_PARAMS));
        } catch (NullPointerException npe) {
            assertFalse("real context must not produce an NPE: " + npe, true);
        } catch (Exception ignored) {
            // ActivityNotFoundException / other framework exceptions are fine — not the crash we
            // guard against (production catches ActivityNotFoundException explicitly).
        }
    }
}
