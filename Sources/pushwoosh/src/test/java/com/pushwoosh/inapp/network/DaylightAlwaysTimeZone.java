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

package com.pushwoosh.inapp.network;

import java.util.Date;
import java.util.TimeZone;

final class DaylightAlwaysTimeZone extends TimeZone {
    private static final int HOUR_MILLIS = 3_600_000;

    DaylightAlwaysTimeZone() {
        setID("Test/DaylightAlways");
    }

    @Override
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis) {
        return HOUR_MILLIS;
    }

    @Override
    public int getOffset(long date) {
        return HOUR_MILLIS;
    }

    @Override
    public void setRawOffset(int offsetMillis) {}

    @Override
    public int getRawOffset() {
        return 0;
    }

    @Override
    public boolean useDaylightTime() {
        return true;
    }

    @Override
    public boolean inDaylightTime(Date date) {
        return true;
    }

    @Override
    public int getDSTSavings() {
        return HOUR_MILLIS;
    }
}
