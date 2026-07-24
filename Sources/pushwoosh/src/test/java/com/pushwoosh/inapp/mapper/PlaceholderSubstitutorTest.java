package com.pushwoosh.inapp.mapper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PlaceholderSubstitutorTest {

    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    public void substitute_localizationKeyPresent_replacesWithValueFormattedByType() {
        String out = PlaceholderSubstitutor.substitute(
                "{{Name|UPPERCASE|Def}}", map("Name", "alice"), Collections.emptyMap());
        assertEquals("ALICE", out);
    }

    @Test
    public void substitute_localizationKeyAbsent_usesDefault() {
        String out =
                PlaceholderSubstitutor.substitute("{{Name|text|Def}}", Collections.emptyMap(), Collections.emptyMap());
        assertEquals("Def", out);
    }

    @Test
    public void substitute_doubleBraceNoDefault_keyAbsent_usesKeyName() {
        String out = PlaceholderSubstitutor.substitute("{{Name|text}}", Collections.emptyMap(), Collections.emptyMap());
        assertEquals("Name", out);
    }

    @Test
    public void substitute_emptyDefaultDoubleBrace_keyAbsent_usesKeyName() {
        // Real editor export form {{key|type|}}: the 3-group pattern needs >=1 default char, so this is
        // caught by the 2-group pass and an absent key yields the key NAME (accidental contract, protected).
        String out =
                PlaceholderSubstitutor.substitute("{{Greeting|text|}}", Collections.emptyMap(), Collections.emptyMap());
        assertEquals("Greeting", out);
    }

    @Test
    public void substitute_emptyDefaultDoubleBrace_keyPresent_usesValue() {
        String out =
                PlaceholderSubstitutor.substitute("{{Greeting|text|}}", map("Greeting", "Hi"), Collections.emptyMap());
        assertEquals("Hi", out);
    }

    @Test
    public void substitute_singleBraceEmptyDefault_tagAbsent_usesEmptyString() {
        String out =
                PlaceholderSubstitutor.substitute("{Discount|type|}", Collections.emptyMap(), Collections.emptyMap());
        assertEquals("", out);
    }

    @Test
    public void substitute_singleBraceWithDefault_tagAbsent_usesDefault() {
        String out = PlaceholderSubstitutor.substitute(
                "{Discount|type|Fallback}", Collections.emptyMap(), Collections.emptyMap());
        assertEquals("Fallback", out);
    }

    @Test
    public void substitute_singleCharKey_leftUntouched_twoCharMinimumQuirk() {
        // Historical quirk inherited from Rich Media: the single-brace key group "(.[^}]+?)" starts with a
        // literal "." so it requires >=2 chars — a 1-char key never matches and the placeholder is left
        // verbatim. Real Pushwoosh keys (tag names, editor element ids) are always longer, so this never
        // surfaces in production. Pinned here so nobody "simplifies" the pattern and changes the contract.
        String out = PlaceholderSubstitutor.substitute("{X|type|Fb}", Collections.emptyMap(), Collections.emptyMap());
        assertEquals("{X|type|Fb}", out);
    }

    @Test
    public void substitute_passOrder_singleBraceInsideLocalizedValue_collapsesToDefault() {
        // Localization pass injects a value that itself contains a single-brace tag; the later tag pass
        // (empty map) must then collapse it to its default -> proves pass ordering.
        String out = PlaceholderSubstitutor.substitute(
                "{{Welcome|text|def}}", map("Welcome", "Hi {Name|CapitalizeFirst|friend}!"), Collections.emptyMap());
        assertEquals("Hi friend!", out);
    }
}
