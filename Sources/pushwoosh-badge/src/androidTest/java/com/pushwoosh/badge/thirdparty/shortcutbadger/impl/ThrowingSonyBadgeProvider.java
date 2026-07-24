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

package com.pushwoosh.badge.thirdparty.shortcutbadger.impl;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stand-in for the real Sony home badge content provider
 * ({@code content://com.sonymobile.home.resourceprovider/badge}), which is absent on a non-Sony
 * device. It models the exact device-contingent condition the signal describes: the Sony provider is
 * PRESENT (so {@code SonyHomeBadger.sonyBadgeContentProviderExists} returns true and the code takes
 * the ContentProvider insert path) but REJECTS the insert with a {@link SecurityException} — the
 * outcome Sony's provider produces when the caller's package/activity/values don't satisfy it or the
 * {@code PROVIDER_INSERT_BADGE} grant is missing.
 *
 * <p>The insert runs on the {@code AsyncQueryHandler} worker thread ("AsyncQueryWorker"), where AOSP
 * does not wrap {@code resolver.insert} in a try/catch — so this throw is uncaught cross-thread. Only
 * the throw is a stand-in; the async-escape mechanism it triggers is real framework + real SDK code.
 */
public class ThrowingSonyBadgeProvider extends ContentProvider {

    public static final String REJECT_MARKER = "SONY_REPRO_INSERT_REJECTED";

    /** Counts how many times {@code insert} was actually reached — used for non-vacuity assertions. */
    public static final AtomicInteger INSERT_ATTEMPTS = new AtomicInteger();

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        INSERT_ATTEMPTS.incrementAndGet();
        throw new SecurityException(REJECT_MARKER + " uri=" + uri);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
}
