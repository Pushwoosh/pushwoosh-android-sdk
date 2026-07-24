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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.pushwoosh.badge.thirdparty.shortcutbadger.ShortcutBadgeException;
import com.pushwoosh.badge.thirdparty.shortcutbadger.ShortcutBadger;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Regression guard for crash candidate #22 (crash-sonyhomebadger-async-insert-escape), inverted from
 * its instrumented reproduction: it now asserts the fixed graceful behavior (provider rejection
 * swallowed by the guarded background executor) instead of the pre-fix uncaught escape.
 *
 * <p><b>Why on a device, not Robolectric.</b> The escape lives in the real
 * {@code AsyncQueryHandler}: on the main thread {@code SonyHomeBadger.executeBadgeByContentProvider}
 * hands the write to {@code AsyncQueryHandler.startInsert}, which only enqueues and returns — the real
 * {@code resolver.insert} runs later on the "AsyncQueryWorker" HandlerThread, where AOSP does NOT wrap
 * it in a try/catch. Robolectric's {@code ShadowAsyncQueryHandler} rewrites {@code startInsert} to call
 * {@code resolver.insert} SYNCHRONOUSLY on the caller thread, which would make the throw appear caught
 * by {@code applyCountOrThrow} — the opposite of production. Only a real runtime keeps the async worker.
 *
 * <p><b>The stand-in.</b> A non-Sony device has no {@code com.sonymobile.home.resourceprovider}, so
 * the code would take the broadcast path. {@link ThrowingSonyBadgeProvider} (declared in the test
 * manifest) makes the provider resolvable and rejects the insert with a {@link SecurityException} —
 * modelling the device-contingent rejection (missing grant / package-activity mismatch / bad values)
 * the signal names. Everything else is real: real {@code ShortcutBadger.applyCountOrThrow}, real
 * {@code SonyHomeBadger}, real {@code AsyncQueryHandler} worker.
 *
 * <p>{@code SonyHomeBadger} is normally selected only when the HOME launcher is Sony
 * ({@code ShortcutBadger.initBadger}). On a Pixel emulator it never would, so the test injects it into
 * {@code ShortcutBadger.sShortcutBadger} — modelling "the user is on a Sony device". The barrier
 * control drives the SAME throw down the synchronous background-thread path to show the asymmetry.
 */
@RunWith(AndroidJUnit4.class)
public class SonyHomeBadgerAsyncInsertEscapeCrashTest {

    private static final String SONY_AUTHORITY = "com.sonymobile.home.resourceprovider";

    private static void injectSonyBadger(Context ctx) throws Exception {
        ComponentName cn = new ComponentName(ctx.getPackageName(), ctx.getPackageName() + ".Dummy");
        setStatic("sShortcutBadger", new SonyHomeBadger());
        setStatic("sComponentName", cn);
    }

    private static void setStatic(String field, Object value) throws Exception {
        Field f = ShortcutBadger.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(null, value);
    }

    /**
     * Restore {@code ShortcutBadger}'s global lazy-init state so the injected Sony badger does not
     * leak into any androidTest class added to this module later (applyCountOrThrow only re-selects a
     * badger when {@code sShortcutBadger} is null).
     */
    @After
    public void resetBadgerStatics() throws Exception {
        setStatic("sShortcutBadger", null);
        setStatic("sComponentName", null);
    }

    /**
     * Regression guard (inverted from the pre-fix repro). Main-thread call — the deferred branch. The
     * fix routes the write through {@code BackgroundExecutor.execute}, whose {@code catch(Throwable)}
     * barrier swallows a provider rejection and logs it instead of letting it escape uncaught. So:
     * {@code applyCountOrThrow} still returns without throwing, the rejecting insert IS actually reached
     * (non-vacuity), and NO uncaught escape occurs — the opposite of the old AsyncQueryHandler path,
     * which killed the app on the "AsyncQueryWorker" thread.
     */
    @Test
    public void insertOnMainThread_providerRejectionSwallowedNotEscaped() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        injectSonyBadger(ctx);
        ThrowingSonyBadgeProvider.INSERT_ATTEMPTS.set(0);

        assertNotNull(
                "Sony provider stand-in must be resolvable, else executeBadge takes the broadcast path",
                ctx.getPackageManager().resolveContentProvider(SONY_AUTHORITY, 0));

        CountDownLatch escaped = new CountDownLatch(1);
        AtomicReference<Throwable> captured = new AtomicReference<>();
        AtomicReference<String> escapedThreadName = new AtomicReference<>();
        AtomicReference<Throwable> mainThrew = new AtomicReference<>();

        Thread.UncaughtExceptionHandler original = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            escapedThreadName.set(t.getName());
            captured.set(e);
            escaped.countDown();
        });
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                try {
                    ShortcutBadger.applyCountOrThrow(ctx, 1);
                } catch (Throwable t) {
                    mainThrew.set(t);
                }
            });

            // Non-vacuity: wait for the deferred insert to actually reach the rejecting provider on
            // the guarded background thread. Without this, "no escape" could just mean the write never
            // ran.
            long deadline = System.currentTimeMillis() + 10_000;
            while (ThrowingSonyBadgeProvider.INSERT_ATTEMPTS.get() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }

            assertNull(
                    "applyCountOrThrow on the main thread must return without throwing: the write is "
                            + "deferred off the main thread. Got: " + mainThrew.get(),
                    mainThrew.get());
            assertTrue(
                    "the deferred insert must actually reach the rejecting provider (non-vacuous): " + "attempts="
                            + ThrowingSonyBadgeProvider.INSERT_ATTEMPTS.get(),
                    ThrowingSonyBadgeProvider.INSERT_ATTEMPTS.get() >= 1);
            assertFalse(
                    "the provider rejection must be swallowed by the guarded executor, not escape "
                            + "uncaught (the pre-fix AsyncQueryHandler crash). Escaped: " + captured.get()
                            + " on thread " + escapedThreadName.get(),
                    escaped.await(2, TimeUnit.SECONDS));
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(original);
        }
    }

    /**
     * Barrier control — the SAME rejecting insert, but driven from a background thread (no Looper), so
     * {@code executeBadgeByContentProvider} takes the SYNCHRONOUS {@code insertBadgeSync} branch. The
     * throw is now synchronous inside {@code executeBadge}, so {@code applyCountOrThrow}'s
     * {@code try/catch(Exception)} catches it and rethrows a {@code ShortcutBadgeException}. No uncaught
     * escape. Same throw, opposite outcome — the async main-thread path is what defeats the guard.
     */
    @Test
    public void insertOnBackgroundThread_syncThrowIsCaughtByApplyCountOrThrow() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        injectSonyBadger(ctx);

        CountDownLatch escaped = new CountDownLatch(1);
        Thread.UncaughtExceptionHandler original = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> escaped.countDown());

        AtomicReference<Throwable> caughtBySdk = new AtomicReference<>();
        try {
            Thread worker = new Thread(
                    () -> {
                        try {
                            ShortcutBadger.applyCountOrThrow(ctx, 1);
                        } catch (Throwable t) {
                            caughtBySdk.set(t);
                        }
                    },
                    "repro-bg-caller");
            worker.start();
            worker.join(10_000);

            Throwable t = caughtBySdk.get();
            assertNotNull(
                    "on a background thread the insert is synchronous inside executeBadge, so "
                            + "applyCountOrThrow's try/catch must convert it to ShortcutBadgeException",
                    t);
            assertTrue(
                    "expected ShortcutBadgeException wrapping the SecurityException, got: " + t,
                    t instanceof ShortcutBadgeException);
            assertNotNull("ShortcutBadgeException must wrap the provider cause", t.getCause());
            assertTrue(
                    "wrapped cause must be the provider SecurityException, got: " + t.getCause(),
                    t.getCause() instanceof SecurityException
                            && t.getCause().getMessage() != null
                            && t.getCause().getMessage().contains(ThrowingSonyBadgeProvider.REJECT_MARKER));
            assertFalse(
                    "no uncaught escape on the synchronous path — same throw, opposite outcome",
                    escaped.await(2, TimeUnit.SECONDS));
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(original);
        }
    }
}
