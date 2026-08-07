package com.pushwoosh.inapp.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class NativeConfigLocalizerTest {

    private static Map<String, String> strings(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private static Map<String, String> tags(String... kv) {
        return strings(kv);
    }

    // Every legacy vector goes through here: an explicit empty tag map is the regression guard that the
    // pre-dynamic-content semantics (placeholder -> default) are unchanged when the device has no tags.
    private static String localizeWithoutTags(String configJson, Map<String, String> strings) {
        return NativeConfigLocalizer.localize(configJson, strings, Collections.emptyMap());
    }

    @Test
    public void localize_nestedObjectsAndArrays_replacesEveryStringValue() throws Exception {
        String in = "{\"modal\":{\"title\":{\"text\":\"{{Greeting|text|Hi}}\"},"
                + "\"image\":\"https://cdn/{{Img|text|fallback.png}}\","
                + "\"buttons\":[{\"label\":\"{{Cta|text|Go}}\"}]}}";
        String out = localizeWithoutTags(in, strings("Greeting", "Hello", "Img", "hero.png", "Cta", "Buy"));

        JSONObject modal = new JSONObject(out).getJSONObject("modal");
        assertEquals("Hello", modal.getJSONObject("title").getString("text"));
        assertEquals("https://cdn/hero.png", modal.getString("image"));
        assertEquals("Buy", modal.getJSONArray("buttons").getJSONObject(0).getString("label"));
    }

    @Test
    public void localize_doubleBraceWithDefault_keyPresent_usesValue() throws Exception {
        String out = localizeWithoutTags("{\"a\":\"{{Name|text|Def}}\"}", strings("Name", "Alice"));
        assertEquals("Alice", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_doubleBraceWithDefault_keyAbsent_usesDefault() throws Exception {
        String out = localizeWithoutTags("{\"a\":\"{{Name|text|Def}}\"}", Collections.emptyMap());
        assertEquals("Def", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_doubleBraceNoDefault_keyPresent_usesValue() throws Exception {
        String out = localizeWithoutTags("{\"a\":\"{{Name|text}}\"}", strings("Name", "Alice"));
        assertEquals("Alice", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_doubleBraceNoDefault_keyAbsent_usesKeyName() throws Exception {
        String out = localizeWithoutTags("{\"a\":\"{{Name|text}}\"}", Collections.emptyMap());
        assertEquals("Name", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_realEditorFormatEmptyDefault_keyPresent_usesValue() throws Exception {
        // Dot in key is opaque to the mechanism; present key -> localized value.
        String out =
                localizeWithoutTags("{\"a\":\"{{Z4JXQGaoND.text|text|}}\"}", strings("Z4JXQGaoND.text", "Localized"));
        assertEquals("Localized", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_realEditorFormatEmptyDefault_keyAbsent_usesKeyName() throws Exception {
        // Accidental multi-year contract: {{key|text|}} with a missing key yields the key NAME, not "".
        String out = localizeWithoutTags("{\"a\":\"{{Z4JXQGaoND.text|text|}}\"}", Collections.emptyMap());
        assertEquals("Z4JXQGaoND.text", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_emptyDictionaryValue_producesEmptyString() throws Exception {
        String out = localizeWithoutTags("{\"a\":\"{{Promo|text|def}}\"}", strings("Promo", ""));
        assertEquals("", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_quotesAndNewlineInTranslation_staysValidJson() throws Exception {
        String out = localizeWithoutTags("{\"a\":\"{{Promo|text|def}}\"}", strings("Promo", "line1\nline2 \"q\""));
        // Re-parsing proves the output is still valid JSON despite quotes/newlines in the translation.
        assertEquals("line1\nline2 \"q\"", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_nonStringValuesAndKeys_areNotTouched() throws Exception {
        String in = "{\"count\":5,\"enabled\":true,\"ratio\":1.5,\"nothing\":null,"
                + "\"{{Promo|text|d}}\":\"{{Promo|text|d}}\"}";
        String out = localizeWithoutTags(in, strings("Promo", "V"));

        JSONObject o = new JSONObject(out);
        assertEquals(5, o.getInt("count"));
        assertTrue(o.getBoolean("enabled"));
        assertEquals(1.5, o.getDouble("ratio"), 0.0);
        assertTrue(o.isNull("nothing"));
        // The placeholder-shaped KEY is preserved verbatim; only its VALUE is localized.
        assertTrue(o.has("{{Promo|text|d}}"));
        assertEquals("V", o.getString("{{Promo|text|d}}"));
    }

    @Test
    public void localize_emptyStringsMap_collapsesPlaceholdersToDefaults() throws Exception {
        String out =
                localizeWithoutTags("{\"a\":\"{{Promo|text|Def}}\",\"b\":\"{Timer|type|Fb}\"}", Collections.emptyMap());
        JSONObject o = new JSONObject(out);
        assertEquals("Def", o.getString("a"));
        assertEquals("Fb", o.getString("b"));
    }

    @Test
    public void localize_invalidJson_returnedUnchanged() {
        String in = "not json {{Promo|text|d}}";
        assertEquals(in, localizeWithoutTags(in, strings("Promo", "V")));
    }

    @Test
    public void localize_noPlaceholders_valuesUnchanged() throws Exception {
        String out = localizeWithoutTags("{\"a\":\"plain\",\"b\":\"no placeholders\"}", strings("K", "V"));
        JSONObject o = new JSONObject(out);
        assertEquals("plain", o.getString("a"));
        assertEquals("no placeholders", o.getString("b"));
    }

    @Test
    public void localize_nestedSingleBraceInLocalizedValue_collapsesToDefault() throws Exception {
        // Reuses the ResourceMapperTest vector LocalizedString2 -> {Tag2|CapitalizeFirst|Tag2Value}:
        // with no cached tags, the nested single-brace tag collapses to its default.
        String out = localizeWithoutTags(
                "{\"a\":\"{{Welcome|text|def}}\"}", strings("Welcome", "Hi {Name|CapitalizeFirst|friend}!"));
        assertEquals("Hi friend!", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_singleBraceEmptyDefault_collapsesToEmptyString() throws Exception {
        String out = localizeWithoutTags("{\"a\":\"{Discount|type|}\"}", Collections.emptyMap());
        assertEquals("", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_pathologicalNesting_stackOverflowIsCaughtNotThrown() throws Exception {
        // Deeply nested input must not blow up the worker thread: a StackOverflowError from the parser or
        // the localizeObject/localizeArray recursion is caught (Throwable) and the raw config handed back,
        // so the show is never dropped and the module parser rejects it downstream instead.
        int depth = 50000;
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            deep.append("{\"a\":");
        }
        deep.append("1");
        for (int i = 0; i < depth; i++) {
            deep.append("}");
        }
        String in = deep.toString();

        // Run on a thread with a deliberately tiny stack so the StackOverflowError fires deterministically,
        // independent of the JVM-wide -Xss (a large -Xss on the test worker would let depth=50000 parse
        // cleanly, silently skipping the catch and giving a false green). Capture whatever escapes: if the
        // catch is ever narrowed from Throwable to Exception, the SOE escapes localize() and lands here,
        // failing assertNull. assertEquals proves the fallback hands back the ORIGINAL config, not null.
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> escaped = new AtomicReference<>();
        Thread worker = new Thread(
                null,
                () -> {
                    try {
                        result.set(localizeWithoutTags(in, Collections.emptyMap()));
                    } catch (Throwable t) {
                        escaped.set(t);
                    }
                },
                "native-config-soe-test",
                128 * 1024);
        worker.start();
        worker.join();

        assertNull("localize() must swallow the StackOverflowError, not let it escape", escaped.get());
        assertEquals(in, result.get());
    }

    @Test
    public void localize_singleBraceWithDefault_tagPresent_usesFormattedTagValue() throws Exception {
        String out = NativeConfigLocalizer.localize(
                "{\"a\":\"{UserName|CapitalizeAllFirst|pes}\"}", Collections.emptyMap(), tags("UserName", "alexey"));
        assertEquals("Alexey", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_singleBraceWithDefault_tagAbsent_usesDefault() throws Exception {
        String out = NativeConfigLocalizer.localize(
                "{\"a\":\"{UserName|CapitalizeAllFirst|pes}\"}", Collections.emptyMap(), Collections.emptyMap());
        assertEquals("pes", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_singleBraceEmptyDefault_tagPresent_usesTagValue() throws Exception {
        String out = NativeConfigLocalizer.localize(
                "{\"a\":\"{Discount|text|}\"}", Collections.emptyMap(), tags("Discount", "50%"));
        assertEquals("50%", new JSONObject(out).getString("a"));
    }

    // Ground truth from rich media 0F765-A7953: the config points at pushwoosh.json, and the translation
    // carries the tag placeholder. This is the shape the editor actually exports.
    @Test
    public void localize_tagInsideTranslatedValue_tagPresent_usesTagValue() throws Exception {
        String out = NativeConfigLocalizer.localize(
                "{\"a\":\"{{t3.text|text}}\"}",
                strings("t3.text", "Button {UserName|CapitalizeAllFirst|pes}"),
                tags("UserName", "Alexey"));
        assertEquals("Button Alexey", new JSONObject(out).getString("a"));
    }

    @Test
    public void localize_tagInsideTranslatedValue_tagAbsent_usesDefault() throws Exception {
        String out = NativeConfigLocalizer.localize(
                "{\"a\":\"{{t3.text|text}}\"}",
                strings("t3.text", "Button {UserName|CapitalizeAllFirst|pes}"),
                Collections.emptyMap());
        assertEquals("Button pes", new JSONObject(out).getString("a"));
    }

    // Pass 1 (language, same pattern, DOTALL) consumes every double-brace-with-default in the RAW config
    // before the tag pass can see it, so a double-brace tag written straight into native-config.json
    // resolves to its default. Pinned so nobody "fixes" it by reordering the passes: the order is shared
    // with Rich Media HTML and reordering would change years-old rich media output.
    @Test
    public void localize_doubleBraceInRawConfig_neverReachesTagPasses() throws Exception {
        String out = NativeConfigLocalizer.localize(
                "{\"a\":\"{{UserName|CapitalizeAllFirst|pes}}\"}", Collections.emptyMap(), tags("UserName", "alexey"));
        assertEquals("pes", new JSONObject(out).getString("a"));
    }

    // The only route to pass 2: the double-brace tag placeholder arrives inside a value that pass 1
    // inserted, and pass 1's matcher never rescans its own insertions.
    @Test
    public void localize_doubleBraceInsideTranslatedValue_usesTagValue() throws Exception {
        String out = NativeConfigLocalizer.localize(
                "{\"a\":\"{{Welcome|text|d}}\"}",
                strings("Welcome", "Hello {{UserName|UPPERCASE|friend}}"),
                tags("UserName", "alexey"));
        assertEquals("Hello ALEXEY", new JSONObject(out).getString("a"));
    }

    // Single pipe {key|type} matches none of the 5 passes on either platform, and the backend does not
    // treat it as a placeholder either. It reaches the UI as raw text — deliberate parity with rich media.
    @Test
    public void localize_singlePipeForm_staysRawEvenWhenTagPresent() throws Exception {
        String out = NativeConfigLocalizer.localize(
                "{\"a\":\"{City|UPPERCASE}\"}", Collections.emptyMap(), tags("City", "Berlin"));
        assertEquals("{City|UPPERCASE}", new JSONObject(out).getString("a"));
    }

    // Verifies that the tag dictionary survives the array branch. This is the ground-truth shape of the
    // feature — a button label lives inside an ARRAY and its tag placeholder arrives from pushwoosh.json —
    // yet every other tag test sits on a top-level value, so a tags-to-emptyMap slip in localizeArray (its
    // own string branch or its recursion into localizeObject) would go unnoticed while silently killing
    // personalization for exactly the element the e2e checks.
    @Test
    public void localize_tagInsideArray_usesTagValue() throws Exception {
        String out = NativeConfigLocalizer.localize(
                "{\"buttons\":[{\"label\":\"{{t3.text|text}}\"},\"{UserName|UPPERCASE|friend}\"]}",
                strings("t3.text", "Button {UserName|CapitalizeAllFirst|pes}"),
                tags("UserName", "alexey"));

        JSONArray buttons = new JSONObject(out).getJSONArray("buttons");
        assertEquals("Button Alexey", buttons.getJSONObject(0).getString("label"));
        assertEquals("ALEXEY", buttons.getString(1));
    }
}
