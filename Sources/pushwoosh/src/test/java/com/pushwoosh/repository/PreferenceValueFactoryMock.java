/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.repository;

import android.content.SharedPreferences;

import com.pushwoosh.internal.preference.PreferenceArrayListValue;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.internal.preference.PreferenceValueFactory;

import org.mockito.Mockito;

import static org.mockito.Mockito.spy;

/**
 * Created by aevstefeev on 21/03/2018.
 */

class PreferenceValueFactoryMock extends PreferenceValueFactory {
   private PreferenceArrayListValue<String> preferenceArrayListValue;
   private PreferenceIntValue preferenceIntValue;

    public PreferenceArrayListValue<String> getPreferenceArrayListValue() {
        return preferenceArrayListValue;
    }

    public PreferenceIntValue getPreferenceIntValue() {
        return preferenceIntValue;
    }

    @Override
    public PreferenceIntValue buildPreferenceIntValue(SharedPreferences preferences, String key, int defaultValue) {
        preferenceIntValue = Mockito.mock(PreferenceIntValue.class);
        return preferenceIntValue;
    }

    @Override
    public PreferenceArrayListValue<String> buildPreferenceArrayListValue(SharedPreferences preferences, String key, int capacity) {
        preferenceArrayListValue = Mockito.mock(PreferenceArrayListValue.class);
        return preferenceArrayListValue;
    }
}
