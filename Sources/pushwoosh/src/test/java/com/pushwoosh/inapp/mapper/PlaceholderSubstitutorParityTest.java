package com.pushwoosh.inapp.mapper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Android half of the cross-platform placeholder parity matrix. {@code PWResourcePlaceholderParityTest.m}
 * in the iOS SDK mirrors it; the two case tables must stay row-for-row identical, so a row added, removed or
 * renamed here has to be mirrored there in the same change.
 *
 * <p>Groups A-D are forms both platforms agree on. Group E records the four known divergences: those tests
 * pin what Android does today, they do not state what it ought to do.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PlaceholderSubstitutorParityTest {

    /**
     * The 18 modifiers {@link InAppTagFormatModifier} knows plus {@code regular}, which the platform emits and
     * no SDK handles, so it falls through to the unmodified value.
     */
    private static final String[] MODIFIERS = {
        "lowercase",
        "UPPERCASE",
        "CapitalizeFirst",
        "CapitalizeAllFirst",
        "cent",
        "dollar",
        "comma",
        "euro",
        "jpy",
        "lira",
        "M-d-y",
        "m-d-y",
        "M d y",
        "M d Y",
        "l",
        "M d",
        "H:i",
        "m-d-y H:i",
        "regular",
    };

    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private static String substitute(String content) {
        return PlaceholderSubstitutor.substitute(content, Collections.emptyMap(), Collections.emptyMap());
    }

    // --- Group A: placeholder forms, key and tag both absent ---------------------------------------------

    @Test
    public void groupA_doubleBraceWithDefault_usesDefault() {
        assertEquals("Save", substitute("{{Promo|text|Save}}"));
    }

    @Test
    public void groupA_doubleBraceWithoutDefault_usesKeyName() {
        assertEquals("Promo", substitute("{{Promo|text}}"));
    }

    @Test
    public void groupA_doubleBraceColorDefault_usesDefault() {
        assertEquals("#ffffff", substitute("{{Promo|color|#ffffff}}"));
    }

    @Test
    public void groupA_doubleBraceHtmlDefault_usesDefaultVerbatim() {
        assertEquals("<b>x</b>", substitute("{{Promo|html|<b>x</b>}}"));
    }

    @Test
    public void groupA_singleBraceWithDefault_usesDefault() {
        assertEquals("Save", substitute("{Promo|text|Save}"));
    }

    @Test
    public void groupA_singleBraceEmptyDefault_usesEmptyString() {
        assertEquals("", substitute("{Promo|text|}"));
    }

    @Test
    public void groupA_keyWithSpace_usesDefault() {
        assertEquals("Save", substitute("{Promo Code|text|Save}"));
    }

    // --- Group B: modifier sweep, tag absent -------------------------------------------------------------

    @Test
    public void groupB_everyModifierWithTagAbsent_leavesDigitDefaultAlone() {
        for (String modifier : MODIFIERS) {
            String withDefault = "{Promo|" + modifier + "|7}";
            String emptyDefault = "{Promo|" + modifier + "|}";

            // Deviation from the shared table: the type group is "(.[^}]+?)", so a one-character modifier
            // ("l") never matches and the placeholder survives verbatim - same quirk as a one-character key.
            boolean matchable = modifier.length() > 1;

            assertEquals(modifier, matchable ? "7" : withDefault, substitute(withDefault));
            assertEquals(modifier, matchable ? "" : emptyDefault, substitute(emptyDefault));
        }
    }

    // --- Group C: modifier applied to a present tag ------------------------------------------------------

    @Test
    public void groupC_uppercaseModifierOnPresentTag_appliesModifier() {
        assertEquals(
                "ALICE BOB",
                PlaceholderSubstitutor.substitute(
                        "{Name|UPPERCASE|d}", Collections.emptyMap(), map("Name", "aLICE bob")));
    }

    @Test
    public void groupC_lowercaseModifierOnPresentTag_appliesModifier() {
        assertEquals(
                "alice bob",
                PlaceholderSubstitutor.substitute(
                        "{Name|lowercase|d}", Collections.emptyMap(), map("Name", "aLICE bob")));
    }

    // --- Group D: localization ---------------------------------------------------------------------------

    @Test
    public void groupD_localizedValueWithNewlines_insertedVerbatim() {
        assertEquals(
                "Line1\nLine2",
                PlaceholderSubstitutor.substitute(
                        "{{Welcome|text|def}}", map("Welcome", "Line1\nLine2"), Collections.emptyMap()));
    }

    @Test
    public void groupD_multilineDefault_keyAbsent_usesDefault() {
        assertEquals("multi\nline\ndefault", substitute("{{Welcome|text|multi\nline\ndefault}}"));
    }

    @Test
    public void groupD_localizedValueContainingPlaceholder_collapsedByLaterTagPass() {
        assertEquals(
                "Hi friend!",
                PlaceholderSubstitutor.substitute(
                        "{{Welcome|text|def}}", map("Welcome", "Hi {Name|text|friend}!"), Collections.emptyMap()));
    }

    // --- Group E: known Android/iOS divergences, pinned as-is --------------------------------------------

    /**
     * D1: iOS renders {@code {{Promo|text|}}} as the empty string. Android falls through to the two-group
     * pass and yields the key name.
     */
    @Test
    public void groupE_doubleBraceEmptyDefault_platformDivergenceD1() {
        assertEquals("Promo", substitute("{{Promo|text|}}"));
    }

    /**
     * D2: iOS applies the modifier to the default value and renders {@code SAVE}. Android only ever applies a
     * modifier to a tag value, so an absent tag leaves the default untouched.
     */
    @Test
    public void groupE_modifierOnAbsentTagDefault_platformDivergenceD2() {
        assertEquals("save", substitute("{Promo|UPPERCASE|save}"));
    }

    /**
     * D3: iOS renders {@code {X|type|Fb}} as {@code Fb}. Android's key group is {@code (.[^}]+?)} and needs
     * two characters, so a one-character key leaves the placeholder verbatim.
     */
    @Test
    public void groupE_oneCharacterKey_platformDivergenceD3() {
        assertEquals("{X|type|Fb}", substitute("{X|type|Fb}"));
    }

    /**
     * D4: iOS treats the one-character date modifier {@code l} as a modifier and renders {@code {Promo|l|7}}
     * as {@code 7}. Android's type group is {@code (.[^}]+?)} and needs two characters, so the whole form
     * survives verbatim. Group B's modifier sweep asserts the same Android value in passing; this row is
     * where the divergence is named.
     */
    @Test
    public void groupE_oneCharacterModifier_platformDivergenceD4() {
        assertEquals("{Promo|l|7}", substitute("{Promo|l|7}"));
        assertEquals("{Promo|l|}", substitute("{Promo|l|}"));
    }
}
