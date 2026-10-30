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

    @Test
    public void substitute_inlineScriptSplittingOnPipe_leftByteForByte() {
        String script = "<script>\n(function(){\n  \"use strict\";\n"
                + "  function resolve(value,fallback){if(!value)return fallback;"
                + "if(value.indexOf(\"{\"+\"{\")!==0)return value;var parts=value.slice(2,-2).split(\"|\");"
                + "return parts.length>2?parts.slice(2).join(\"|\").trim():fallback;}\n"
                + "})();\n</script>";
        String out = PlaceholderSubstitutor.substitute(script, Collections.emptyMap(), Collections.emptyMap());
        assertEquals(script, out);
    }

    @Test
    public void substitute_inlineScriptWithBitwiseOr_leftByteForByte() {
        String script = "<script>function flags(){ return READ | WRITE | EXEC; }</script>";
        String out = PlaceholderSubstitutor.substitute(script, Collections.emptyMap(), Collections.emptyMap());
        assertEquals(script, out);
    }

    @Test
    public void substitute_placeholdersNextToInlineScript_substitutedAndScriptKept() {
        String script = "<script>function resolve(v){var p=v.split(\"|\");return p.slice(2).join(\"|\");}</script>";
        String html = "<h1>{{Title|text|Hello}}</h1><p>{Name|CapitalizeFirst|friend}</p>" + script;
        String out = PlaceholderSubstitutor.substitute(html, map("Title", "One market."), map("Name", "omar"));
        assertEquals("<h1>One market.</h1><p>Omar</p>" + script, out);
    }

    @Test
    public void substitute_scriptTagUppercaseAndAttributed_contentProtected() {
        String pipesOnly = "<SCRIPT type=\"text/javascript\">var f = A | B | C;</SCRIPT >";
        assertEquals(
                pipesOnly,
                PlaceholderSubstitutor.substitute(pipesOnly, Collections.emptyMap(), Collections.emptyMap()));

        // Braced body: without case-insensitive matching and the "</SCRIPT >" tolerance this one is eaten.
        String braced = "<SCRIPT type=\"text/javascript\">function f(){ return A | B | C; }</SCRIPT >";
        assertEquals(braced, PlaceholderSubstitutor.substitute(braced, Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void substitute_multipleScriptBlocks_eachProtectedAndTextBetweenSubstituted() {
        String pipesOnly = "<script>a|b|c;</script><p>{Name|text|friend}</p><script>d|e|f;</script>";
        assertEquals(
                "<script>a|b|c;</script><p>friend</p><script>d|e|f;</script>",
                PlaceholderSubstitutor.substitute(pipesOnly, Collections.emptyMap(), Collections.emptyMap()));

        String left = "<script>function l(){ return A | B | C; }</script>";
        String right = "<script>function r(){ return D | E | F; }</script>";
        assertEquals(
                left + "<p>friend</p>" + right,
                PlaceholderSubstitutor.substitute(
                        left + "<p>{Name|text|friend}</p>" + right, Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void substitute_emptyScriptBody_contentUnchangedAroundIt() {
        String html = "<script src=\"app.js\"></script><p>{Name|text|friend}</p>";
        String out = PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap());
        assertEquals("<script src=\"app.js\"></script><p>friend</p>", out);
    }

    @Test
    public void substitute_closingScriptInsideJsStringLiteral_segmentEndsThere() {
        // A browser ends the script at the first </script regardless of JS quoting; the tail is ordinary
        // text and IS substituted. Pinned deliberately as browser-parser parity.
        String pipesOnly = "<script>var s = \"</script>\"; var x = a|b|c;</script>";
        assertEquals(
                pipesOnly,
                PlaceholderSubstitutor.substitute(pipesOnly, Collections.emptyMap(), Collections.emptyMap()));

        String withPlaceholder = "<script>var s = \"</script>\"; var x = {Name|text|friend};</script>";
        assertEquals(
                "<script>var s = \"</script>\"; var x = friend;</script>",
                PlaceholderSubstitutor.substitute(withPlaceholder, Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void substitute_commentedOutScriptTag_doesNotOpenAScriptBlock() {
        // A browser never opens an element for a <script> inside a comment, so the markup that follows is
        // ordinary markup and keeps all five passes.
        String html = "<!-- <script> --><h1>{Promo|text|Save 20%}</h1><script>var a=1;</script>";
        assertEquals(
                "<!-- <script> --><h1>Save 20%</h1><script>var a=1;</script>",
                PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void substitute_scriptTagTextInsideAnotherTagsAttribute_doesNotOpenAScriptBlock() {
        String html = "<div data-tpl=\"<script>\"><p>{Promo|text|Save 20%}</p></div><script>var a=1;</script>";
        assertEquals(
                "<div data-tpl=\"<script>\"><p>Save 20%</p></div><script>var a=1;</script>",
                PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void substitute_elementWhoseNameStartsWithScript_doesNotOpenAScriptBlock() {
        String tail = "<p>{Promo|text|Save 20%}</p><script>var a=1;</script>";
        for (String element : new String[] {"scripts", "script1", "script-widget", "script.x", "script:x"}) {
            String open = "<" + element + " data-id=\"1\"></" + element + ">";
            assertEquals(
                    element,
                    open + "<p>Save 20%</p><script>var a=1;</script>",
                    PlaceholderSubstitutor.substitute(open + tail, Collections.emptyMap(), Collections.emptyMap()));
        }
    }

    @Test
    public void substitute_unterminatedScriptOpeningTag_substitutionStillRuns() {
        String html = "a <script tip {Name|text|friend} tail";
        String out = PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap());
        assertEquals("a <script tip friend tail", out);
    }

    @Test
    public void substitute_scriptOpenedButNeverClosed_restOfDocumentGetsAllPasses() {
        String html = "<script>x<p>{Name|text|friend}</p>";
        String out = PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap());
        assertEquals("<script>x<p>friend</p>", out);
    }

    @Test
    public void substitute_lessThanWithQuoteInText_scriptBodyStillProtected() {
        // "<'" is text, not a tag; read as a tag, the quote would run past the real <script> and expose its body.
        String html = "<p>a <'b</p><script>function f(){ return A | B | C; }</script>";
        assertEquals(html, PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void substitute_doubleBraceInsideScriptBody_substituted() {
        String html = "<script>var name = \"{{UserName|text|Guest}}\";</script>";
        assertEquals(
                "<script>var name = \"Omar\";</script>",
                PlaceholderSubstitutor.substitute(html, map("UserName", "Omar"), Collections.emptyMap()));
        assertEquals(
                "<script>var name = \"Guest\";</script>",
                PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void substitute_doubleBraceAndJsExpressionInSameScriptBody_onlyPlaceholderTouched() {
        String html = "<script>var name = \"{{UserName|text|Guest}}\";"
                + "function pick(a,b,c){ return a || b || c; }"
                + "var parts = raw.split(\"|\");</script>";
        assertEquals(
                "<script>var name = \"Omar\";"
                        + "function pick(a,b,c){ return a || b || c; }"
                        + "var parts = raw.split(\"|\");</script>",
                PlaceholderSubstitutor.substitute(html, map("UserName", "Omar"), map("a", "nope")));
    }

    @Test
    public void substitute_placeholderInScriptOpeningTag_substituted() {
        String html = "<script src=\"https://cdn.example/t.js?uid={UserId|text|}\"></script>";
        assertEquals(
                "<script src=\"https://cdn.example/t.js?uid=42\"></script>",
                PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), map("UserId", "42")));
        assertEquals(
                "<script src=\"https://cdn.example/t.js?uid=\"></script>",
                PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void substitute_greaterThanInsideOpeningTagAttribute_tagStillEndsAtTheRealDelimiter() {
        String html = "<script src=\"a.js\" onload=\"if(1>0)go()\" data-u=\"{UserId|text|}\">"
                + "function f(){ return A | B | C; }</script>";
        assertEquals(
                "<script src=\"a.js\" onload=\"if(1>0)go()\" data-u=\"42\">"
                        + "function f(){ return A | B | C; }</script>",
                PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), map("UserId", "42")));
    }

    @Test
    public void substitute_greaterThanInsideSingleQuotedAttribute_tagStillEndsAtTheRealDelimiter() {
        String html = "<script onload='if(1>0)go()' data-u=\"{UserId|text|}\">"
                + "function f(){ return A | B | C; }</script>";
        assertEquals(
                "<script onload='if(1>0)go()' data-u=\"42\">function f(){ return A | B | C; }</script>",
                PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), map("UserId", "42")));
    }

    @Test
    public void substitute_apostropheOutsideAttributeQuoting_scriptBodyStillProtected() {
        String script = "<script>function f(){ return A | B | C; }</script>";
        for (String tag : new String[] {"<img alt=O'Neil src=a.png>", "<p>x<y isn't</p>"}) {
            assertEquals(
                    tag,
                    tag + script + "<p>friend</p>",
                    PlaceholderSubstitutor.substitute(
                            tag + script + "<p>{Name|text|friend}</p>",
                            Collections.emptyMap(),
                            Collections.emptyMap()));
        }
    }

    @Test(timeout = 2000)
    public void substitute_unterminatedComment_restOfDocumentGetsAllPasses() {
        String html = "<p>{Name|text|friend}</p><!-- <script>{Promo|text|Save}";
        assertEquals(
                "<p>friend</p><!-- <script>Save",
                PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void substitute_placeholderInScriptOpeningTag_bodyStillProtectedFromSingleBrace() {
        String html = "<script type=\"text/javascript\" data-pw=\"{Title|text|Hello}\">"
                + "function f(){ return A | B | C; }</script>";
        assertEquals(
                "<script type=\"text/javascript\" data-pw=\"Hello\">function f(){ return A | B | C; }</script>",
                PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void substitute_doubleBraceDefaultContainingScriptBlock_usesDefault() {
        // The default value of an html placeholder may itself be a script block. Splitting the document
        // before substituting would cut such a placeholder in half and leave it on screen verbatim.
        String html = "<p>{{Promo|html|<script>x()</script>}}</p>";
        assertEquals(
                "<p><script>x()</script></p>",
                PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap()));
    }

    /**
     * Characterisation, not a requirement. A {@code <script type="text/template">} body is a script body like
     * any other, so the double-brace passes rewrite the Mustache-style handle the templating library meant to
     * keep. The pin is here so that a change to this case shows up as a failing test instead of passing
     * unnoticed.
     */
    @Test
    public void substitute_doubleBraceInNonJavaScriptScriptBody_pinsCurrentRewrite() {
        String html = "<script type=\"text/template\">{{ price | currency }}</script>";
        assertEquals(
                "<script type=\"text/template\"> price </script>",
                PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void substitute_styleBlockAndHtmlComment_notProtected() {
        // Only <script> is protected: the style body gets all five passes and survives only because of the
        // two-char minimum on the type group ("x").
        String html = "<style>.a{color|x|red}</style><!-- {Name|text|friend} -->";
        String out = PlaceholderSubstitutor.substitute(html, Collections.emptyMap(), Collections.emptyMap());
        assertEquals("<style>.a{color|x|red}</style><!-- friend -->", out);

        String eaten = "<style>.a{color|xx|red}</style><!-- {Name|text|friend} -->";
        assertEquals(
                "<style>.ared</style><!-- friend -->",
                PlaceholderSubstitutor.substitute(eaten, Collections.emptyMap(), Collections.emptyMap()));
    }
}
