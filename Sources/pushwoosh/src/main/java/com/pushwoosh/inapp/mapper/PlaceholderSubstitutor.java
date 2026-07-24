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

    private PlaceholderSubstitutor() {}

    public static String substitute(String content, Map<String, String> localizedStrings, Map<String, String> tags) {
        content = postProcess(content, DOUBLE_KEY_TYPE_DEFAULT_DOTALL, localizedStrings);
        content = postProcess(content, DOUBLE_KEY_TYPE_DEFAULT, tags);
        content = postProcess(content, DOUBLE_KEY_TYPE, localizedStrings);
        content = postProcess(content, SINGLE_KEY_TYPE_EMPTY, tags);
        content = postProcess(content, SINGLE_KEY_TYPE_DEFAULT, tags);
        return content;
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
