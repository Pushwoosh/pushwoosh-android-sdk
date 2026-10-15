package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class ActionButtonParsingTest {

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

    private static Bundle bundleWithButtons(String rawButtons) {
        Bundle bundle = new Bundle();
        bundle.putString("pw_buttons", rawButtons);
        return bundle;
    }

    @Test
    public void getButtons_threeValidButtons_parsesEveryField() {
        Bundle bundle = bundleWithButtons("["
                + "{\"id\":\"open_order\",\"label\":\"Open order\",\"action\":\"deeplink\",\"url\":\"myapp://orders/4521\"},"
                + "{\"id\":\"site\",\"label\":\"Site\",\"action\":\"url\",\"url\":\"https://shop.com/4521\"},"
                + "{\"id\":\"cancel_order\",\"label\":\"Cancel\",\"action\":\"custom\",\"style\":\"destructive\"}]");

        List<ActionButton> buttons = new PushMessage(bundle).getButtons();

        assertEquals(3, buttons.size());
        assertEquals("open_order", buttons.get(0).getId());
        assertEquals("Open order", buttons.get(0).getLabel());
        assertEquals(ActionButton.ActionType.DEEPLINK, buttons.get(0).getAction());
        assertEquals("myapp://orders/4521", buttons.get(0).getUrl());
        assertEquals(ActionButton.Style.DEFAULT, buttons.get(0).getStyle());
        assertEquals(ActionButton.ActionType.URL, buttons.get(1).getAction());
        assertEquals(ActionButton.ActionType.CUSTOM, buttons.get(2).getAction());
        assertNull(buttons.get(2).getUrl());
        assertEquals(ActionButton.Style.DESTRUCTIVE, buttons.get(2).getStyle());
    }

    @Test
    public void getButtons_brokenJson_returnsEmptyList() {
        assertTrue(new PushMessage(bundleWithButtons("[{\"id\":")).getButtons().isEmpty());
    }

    @Test
    public void getButtons_oneBrokenEntry_keepsTheOthers() {
        Bundle bundle = bundleWithButtons("["
                + "{\"id\":\"a\",\"label\":\"A\",\"action\":\"open\"},"
                + "{\"id\":\"b\",\"action\":\"open\"},"
                + "{\"id\":\"c\",\"label\":\"C\",\"action\":\"open\"}]");

        List<ActionButton> buttons = new PushMessage(bundle).getButtons();

        assertEquals(2, buttons.size());
        assertEquals("a", buttons.get(0).getId());
        assertEquals("c", buttons.get(1).getId());
    }

    @Test
    public void getButtons_unknownAction_skipsThatButton() {
        Bundle bundle = bundleWithButtons("["
                + "{\"id\":\"a\",\"label\":\"A\",\"action\":\"teleport\"},"
                + "{\"id\":\"b\",\"label\":\"B\",\"action\":\"open\"}]");

        List<ActionButton> buttons = new PushMessage(bundle).getButtons();

        assertEquals(1, buttons.size());
        assertEquals("b", buttons.get(0).getId());
    }

    @Test
    public void getButtons_urlActionWithoutUrl_skipsThatButton() {
        Bundle bundle = bundleWithButtons("["
                + "{\"id\":\"a\",\"label\":\"A\",\"action\":\"url\"},"
                + "{\"id\":\"b\",\"label\":\"B\",\"action\":\"deeplink\"}]");

        assertTrue(new PushMessage(bundle).getButtons().isEmpty());
    }

    // Android org.json: optString(key, null) on an explicit JSON null returns the string "null".
    @Test
    public void getButtons_explicitJsonNull_isTreatedAsAnAbsentField() {
        Bundle bundle = bundleWithButtons("["
                + "{\"id\":\"a\",\"label\":\"A\",\"action\":\"open\",\"url\":null,\"style\":null},"
                + "{\"id\":null,\"label\":\"B\",\"action\":\"open\"},"
                + "{\"id\":\"c\",\"label\":null,\"action\":\"open\"},"
                + "{\"id\":\"d\",\"label\":\"D\",\"action\":\"url\",\"url\":null}]");

        List<ActionButton> buttons = new PushMessage(bundle).getButtons();

        assertEquals(1, buttons.size());
        assertEquals("a", buttons.get(0).getId());
        assertNull(buttons.get(0).getUrl());
        assertEquals(ActionButton.Style.DEFAULT, buttons.get(0).getStyle());
    }

    @Test
    public void getButtons_nonObjectEntry_skipsOnlyThatEntry() {
        Bundle bundle = bundleWithButtons("[" + "\"oops\"," + "{\"id\":\"b\",\"label\":\"B\",\"action\":\"open\"}]");

        assertEquals(1, new PushMessage(bundle).getButtons().size());
    }

    @Test
    public void getButtons_duplicateId_keepsTheFirst() {
        Bundle bundle = bundleWithButtons("["
                + "{\"id\":\"a\",\"label\":\"first\",\"action\":\"open\"},"
                + "{\"id\":\"a\",\"label\":\"second\",\"action\":\"open\"}]");

        List<ActionButton> buttons = new PushMessage(bundle).getButtons();

        assertEquals(1, buttons.size());
        assertEquals("first", buttons.get(0).getLabel());
    }

    @Test
    public void getButtons_unknownStyle_fallsBackToDefault() {
        Bundle bundle = bundleWithButtons(
                "[{\"id\":\"a\",\"label\":\"A\",\"action\":\"open\",\"style\":\"neon\",\"future\":1}]");

        assertEquals(
                ActionButton.Style.DEFAULT,
                new PushMessage(bundle).getButtons().get(0).getStyle());
    }

    @Test
    public void getButtons_fiveValidButtons_keepsFirstThree() {
        Bundle bundle = bundleWithButtons("["
                + "{\"id\":\"1\",\"label\":\"1\",\"action\":\"open\"},"
                + "{\"id\":\"2\",\"label\":\"2\",\"action\":\"open\"},"
                + "{\"id\":\"3\",\"label\":\"3\",\"action\":\"open\"},"
                + "{\"id\":\"4\",\"label\":\"4\",\"action\":\"open\"},"
                + "{\"id\":\"5\",\"label\":\"5\",\"action\":\"open\"}]");

        List<ActionButton> buttons = new PushMessage(bundle).getButtons();

        assertEquals(3, buttons.size());
        assertEquals("3", buttons.get(2).getId());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getButtons_returnedListIsImmutable() {
        Bundle bundle = bundleWithButtons("[{\"id\":\"a\",\"label\":\"A\",\"action\":\"open\"}]");

        new PushMessage(bundle).getButtons().clear();
    }

    @Test
    public void getActions_pwButtonsKeyPresent_returnsEmptyEvenWhenButtonsAreBroken() {
        Bundle bundle = bundleWithButtons("not json at all");
        bundle.putString(
                "pw_actions", "[{\"type\":\"ACTIVITY\",\"title\":\"Track\",\"class\":\"com.app.TrackActivity\"}]");

        PushMessage message = new PushMessage(bundle);

        assertTrue(message.getActions().isEmpty());
        assertTrue(message.getButtons().isEmpty());
    }

    @Test
    public void getActions_noPwButtonsKey_stillParsesLegacyActions() {
        Bundle bundle = new Bundle();
        bundle.putString("pw_actions", "[{\"type\":\"ACTIVITY\",\"title\":\"Track\"}]");

        assertEquals(1, new PushMessage(bundle).getActions().size());
    }

    // The backend could send pw_buttons as a nested JSON value; FCM flattens the
    // bundle to strings, so Bundle.getString returns null and the SDK must stay quiet.
    @Test
    public void getButtons_pwButtonsIsNotAString_noButtonsAndStillSuppressesLegacyActions() {
        Bundle bundle = new Bundle();
        bundle.putInt("pw_buttons", 42);
        bundle.putString("pw_actions", "[{\"type\":\"ACTIVITY\",\"title\":\"Track\"}]");

        PushMessage message = new PushMessage(bundle);

        assertTrue(message.getButtons().isEmpty());
        assertTrue(message.getActions().isEmpty());
    }

    @Test
    public void setTappedButtonId_thenPushMessage_exposesItOnTheMessageAndInJson() throws Exception {
        Bundle bundle = bundleWithButtons("[{\"id\":\"a\",\"label\":\"A\",\"action\":\"open\"}]");
        PushBundleDataProvider.setTappedButtonId(bundle, "a");

        PushMessage message = new PushMessage(bundle);

        assertEquals("a", message.getTappedButtonId());
        assertEquals("a", message.toJson().getString("tapped_button_id"));
    }
}
