/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.inapp.mapper;

import com.pushwoosh.internal.utils.PWLog;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies Pushwoosh's 5-pass placeholder substitution to a single string, in the exact order and with
 * the exact per-pass dictionary assignment historically used by {@link ResourceMapper} for Rich Media
 * HTML. Reused by {@link NativeConfigLocalizer} so the two code paths cannot drift.
 *
 * <p>Order is load-bearing: single-brace passes must never see half-eaten double braces. The DOTALL
 * asymmetry is intentional — pass 2 (tags) must NOT use DOTALL, or it false-positively matches
 * JavaScript like { if (a|b|c) {} } spanning newlines.
 *
 * <pre>
 *   1. {{key|type|default}} (DOTALL) &lt;- localizedStrings
 *   2. {{key|type|default}}          &lt;- tags
 *   3. {{key|type}}                  &lt;- localizedStrings
 *   4. {key|type|}                   &lt;- tags
 *   5. {key|type|default}            &lt;- tags
 * </pre>
 *
 * <p>Passes 1-3 run over the whole document first, so a placeholder whose default contains a script block is
 * resolved as one placeholder. The document is then split at script elements: markup and the opening
 * {@code <script ...>} tag get passes 4-5 (attributes carry placeholders such as
 * {@code src="...?uid={UserId|text|}"}); a script body and its {@code </script>} get neither, because the
 * single-brace patterns cannot tell a placeholder from JavaScript like {@code function f(){ return A | B | C; }}.
 *
 * <p>Known limitation: passes 1-3 still run inside script bodies, since templates personalise scripts with
 * {@code {{key|type|default}}}, so JavaScript shaped like a double-brace placeholder is rewritten: e.g.
 * {@code {{ price | currency }}} inside {@code <script type="text/template">} is replaced by its key.
 *
 * <p>Only a real script element opens a block: the document is walked tag by tag, so {@code <script>} inside a
 * comment or another tag's attribute value does not count, and the tag name must be exactly {@code script}
 * ({@code <script-widget>} and {@code <scripts>} do not open one). A block ends at the first {@code </script>},
 * regardless of JavaScript quoting, as in a browser. A {@code <script>} without a closing tag gets all five passes
 * on the rest of the document.
 */
public final class PlaceholderSubstitutor {
    private static final String TAG = "[InApp]PlaceholderSubstitutor";

    private static final Pattern DOUBLE_KEY_TYPE_DEFAULT_DOTALL =
            Pattern.compile("\\{\\{(.[^\\}]+?)\\|(.[^\\}]+?)\\|(.[^\\}]*?)\\}\\}", Pattern.DOTALL);
    private static final Pattern DOUBLE_KEY_TYPE_DEFAULT =
            Pattern.compile("\\{\\{(.[^\\}]+?)\\|(.[^\\}]+?)\\|(.[^\\}]*?)\\}\\}");
    private static final Pattern DOUBLE_KEY_TYPE = Pattern.compile("\\{\\{(.[^\\}]+?)\\|(.[^\\}]+?)\\}\\}");
    private static final Pattern SINGLE_KEY_TYPE_EMPTY = Pattern.compile("\\{(.[^\\}]+?)\\|(.[^\\}]+?)\\|\\}");
    private static final Pattern SINGLE_KEY_TYPE_DEFAULT =
            Pattern.compile("\\{(.[^\\}]+?)\\|(.[^\\}]+?)\\|(.[^\\}]*?)\\}");

    private static final Pattern SCRIPT_CLOSING_TAG = Pattern.compile("</script\\s*>", Pattern.CASE_INSENSITIVE);

    private static final String SCRIPT_TAG = "<script";
    private static final String COMMENT_OPEN = "<!--";
    private static final String COMMENT_CLOSE = "-->";

    private static final int BODY_START = 0;
    private static final int BLOCK_END = 1;

    private PlaceholderSubstitutor() {}

    public static String substitute(String content, Map<String, String> localizedStrings, Map<String, String> tags) {
        content = substituteDoubleBracePasses(content, localizedStrings, tags);

        StringBuilder result = new StringBuilder(content.length());
        int cursor = 0;
        int[] script;
        while ((script = nextScriptBlock(content, cursor)) != null) {
            result.append(substituteSingleBracePasses(content.substring(cursor, script[BODY_START]), tags));
            result.append(content, script[BODY_START], script[BLOCK_END]);
            cursor = script[BLOCK_END];
        }
        result.append(substituteSingleBracePasses(content.substring(cursor), tags));
        return result.toString();
    }

    /**
     * Body start and block end of the next script element at or after {@code from}, or {@code null} when the
     * rest of the document holds none. The document is walked tag by tag instead of searching for the
     * next {@code <script}, so only an element opens a block — the same text inside a comment or inside another
     * tag's attribute value does not.
     */
    private static int[] nextScriptBlock(String content, int from) {
        int cursor = from;
        while (cursor < content.length()) {
            int tagStart = content.indexOf('<', cursor);
            if (tagStart < 0) {
                return null;
            }
            if (content.startsWith(COMMENT_OPEN, tagStart)) {
                int commentEnd = content.indexOf(COMMENT_CLOSE, tagStart + COMMENT_OPEN.length());
                cursor = commentEnd < 0 ? content.length() : commentEnd + COMMENT_CLOSE.length();
                continue;
            }
            if (!startsTag(content, tagStart)) {
                cursor = tagStart + 1;
                continue;
            }
            int tagEnd = endOfTag(content, tagStart);
            if (tagEnd < 0) {
                return null;
            }
            if (!opensScript(content, tagStart)) {
                cursor = tagEnd;
                continue;
            }
            Matcher closing = SCRIPT_CLOSING_TAG.matcher(content);
            if (!closing.find(tagEnd)) {
                return null;
            }
            return new int[] {tagEnd, closing.end()};
        }
        return null;
    }

    private static boolean startsTag(String content, int tagStart) {
        int name = tagStart + 1;
        if (name < content.length() && content.charAt(name) == '/') {
            name++;
        }
        return name < content.length() && Character.isLetter(content.charAt(name));
    }

    private static boolean opensScript(String content, int tagStart) {
        if (!content.regionMatches(true, tagStart, SCRIPT_TAG, 0, SCRIPT_TAG.length())) {
            return false;
        }
        int afterName = tagStart + SCRIPT_TAG.length();
        return afterName >= content.length() || endsTagName(content.charAt(afterName));
    }

    private static boolean endsTagName(char c) {
        return Character.isWhitespace(c) || c == '/' || c == '>';
    }

    /**
     * Index past the {@code >} that closes the tag opened at {@code tagStart}, or -1 if it is unterminated.
     * A {@code >} inside a quoted attribute value does not close the tag — {@code onload="if(1>0)go()"} is one
     * attribute, not the end of the element. As in a browser, a quote opens a value only right after {@code =}:
     * the apostrophe in {@code alt=O'Neil} is part of an unquoted value and opens nothing.
     */
    private static int endOfTag(String content, int tagStart) {
        char quote = 0;
        char previous = 0;
        for (int i = tagStart; i < content.length(); i++) {
            char c = content.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if ((c == '"' || c == '\'') && previous == '=') {
                quote = c;
            } else if (c == '>') {
                return i + 1;
            }
            if (!Character.isWhitespace(c)) {
                previous = c;
            }
        }
        return -1;
    }

    /** Passes 1-3 — they run over the whole document, script bodies included, so they run before it is split. */
    private static String substituteDoubleBracePasses(
            String content, Map<String, String> localizedStrings, Map<String, String> tags) {
        content = postProcess(content, DOUBLE_KEY_TYPE_DEFAULT_DOTALL, localizedStrings);
        content = postProcess(content, DOUBLE_KEY_TYPE_DEFAULT, tags);
        return postProcess(content, DOUBLE_KEY_TYPE, localizedStrings);
    }

    /** Passes 4-5 — markup and the opening {@code <script ...>} tag only; they collide with JavaScript. */
    private static String substituteSingleBracePasses(String content, Map<String, String> tags) {
        content = postProcess(content, SINGLE_KEY_TYPE_EMPTY, tags);
        return postProcess(content, SINGLE_KEY_TYPE_DEFAULT, tags);
    }

    private static String postProcess(String content, Pattern pattern, Map<String, String> values) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            if (matcher.groupCount() == 3) {
                content = replace(
                        content, matcher.group(0), matcher.group(1), matcher.group(2), matcher.group(3), values);
            } else if (matcher.groupCount() == 2) {
                if (pattern == SINGLE_KEY_TYPE_EMPTY) {
                    content = replace(content, matcher.group(0), matcher.group(1), matcher.group(2), "", values);
                }
                content = replace(
                        content, matcher.group(0), matcher.group(1), matcher.group(2), matcher.group(1), values);
            } else {
                PWLog.warn(TAG, "Incorrect matching count");
            }
        }
        return content;
    }

    private static String replace(
            String content, String totalKey, String key, String type, String defaultValue, Map<String, String> values) {
        String value = defaultValue;
        if (values.containsKey(key)) {
            value = InAppTagFormatModifier.format(values.get(key), type);
        }
        return content.replace(totalKey, value);
    }
}
