/*
 *
 * Copyright (c) 2026. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.location.internal.checker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reproduces crash-gcmlibrarychecker-classforname-error.
 *
 * <p>GcmLibraryChecker.check() resolves three hardcoded GMS class names with the one-arg
 * Class.forName (initialize=true) and each catch handles ONLY ClassNotFoundException. The first
 * resolve — "com.google.android.gms.common.api.GoogleApiClient$ConnectionCallbacks" at
 * GcmLibraryChecker:48 — is the point the signal predicts. If play-services-base is present at
 * compile time but broken at runtime (excluded transitive dep, R8-stripped superclass, incompatible
 * version pin, SDK POM drift), the JVM surfaces the load as a NoClassDefFoundError /
 * ExceptionInInitializerError / LinkageError — subtypes of Error, not ClassNotFoundException — which
 * the catch cannot intercept. LocationPlugin.init() reaches this via checkGcmLibraries() with no
 * try/catch on the synchronous plugin-init path, so the Error escapes SDK init and crashes the app on
 * startup.
 *
 * <p>Structural twin of crash-firebasechecker-classforname-error, DISTINCT sink: another module
 * (pushwoosh-location vs pushwoosh-firebase), another entry (LocationPlugin.init via checkGcmLibraries
 * vs FcmRegistrar.init), different fixed classes (GMS vs Firebase). Same technique: the class names are
 * fixed SDK constants and the real GMS classes are on the test classpath (play-services-base +
 * play-services-location as implementation deps), so a throwing-clinit fixture cannot be named into that
 * slot. Instead the broken-build condition is injected at the class-loading boundary: a loader that
 * defines the real GcmLibraryChecker bytecode fresh (so its Class.forName binds to this loader) and,
 * when asked for the first GMS class, throws an Error. This drives the real check() through the real
 * checkGcmLibraries() static with the real hardcoded name and one-arg Class.forName; only the dependency
 * graph is a stand-in — which is exactly the signal's root cause (a classloader IS the dependency graph).
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class GcmLibraryCheckerClassForNameErrorCrashTest {

    private static final String CHECKER = "com.pushwoosh.location.internal.checker.GcmLibraryChecker";
    // First of the three Class.forName probes (GcmLibraryChecker:48) — the point the signal predicts.
    private static final String GMS_CONNECTION_CALLBACKS =
            "com.google.android.gms.common.api.GoogleApiClient$ConnectionCallbacks";

    @Test
    public void checkGcmLibraries_gmsBrokenAtRuntime_linkageErrorSwallowedIntoIllegalState() throws Exception {
        Throwable thrown = invokeCheckGcmLibrariesWith(
                new GmsLinkFailureClassLoader(getClass().getClassLoader(), true));

        assertNotNull("checkGcmLibraries() must have thrown", thrown);
        // The crux of the fix: the broken-linkage Error no longer escapes as an Error. The widened
        // catch(ClassNotFoundException | LinkageError) at GcmLibraryChecker:49 intercepts it and rewraps
        // it into an IllegalStateException — an Exception swallowed upstream by onCreate's
        // catch(Exception), exactly like the missing-class path.
        assertTrue(
                "expected the graceful IllegalStateException, got " + thrown, thrown instanceof IllegalStateException);
        assertFalse(
                "the load-time Error must no longer escape — it is rewrapped into an Exception",
                thrown instanceof Error);
        // Non-vacuous + cause preserved: the ISE wraps the original NoClassDefFoundError (the
        // broken-linkage branch), NOT a ClassNotFoundException (the absent branch of the negative control
        // below). This proves the widened LinkageError catch fired, not the pre-existing
        // ClassNotFoundException one — flip the injected type and this cause changes.
        assertTrue(
                "cause must be the broken-linkage NoClassDefFoundError, got " + thrown.getCause(),
                thrown.getCause() instanceof NoClassDefFoundError);
        // Origin proof: the swallowed error was born at the eager Class.forName in GcmLibraryChecker.check.
        assertTrue(
                "cause stack must contain GcmLibraryChecker.check frame, was:\n" + render(thrown.getCause()),
                hasFrame(thrown.getCause(), CHECKER, "check"));
    }

    @Test
    public void checkGcmLibraries_gmsAbsent_classNotFoundIsSwallowedIntoIllegalState() throws Exception {
        // Negative control (the guarded branch): a genuinely missing class surfaces as
        // ClassNotFoundException — an Exception — which check() DOES catch at :49 and rewrap into an
        // IllegalStateException (still an Exception, swallowed upstream by onCreate's catch(Exception)).
        // Same call site, same input shape; only the thrown type differs. Flip Error -> Exception and the
        // crash disappears, so "the failure is an Error" is the necessary condition, not noise.
        Throwable thrown = invokeCheckGcmLibrariesWith(
                new GmsLinkFailureClassLoader(getClass().getClassLoader(), false));

        assertNotNull("checkGcmLibraries() must have thrown", thrown);
        assertTrue("expected IllegalStateException, got " + thrown, thrown instanceof IllegalStateException);
        assertTrue("the guarded type must be an Exception", thrown instanceof Exception);
        assertFalse("the guarded type must NOT be an Error", thrown instanceof Error);
    }

    private Throwable invokeCheckGcmLibrariesWith(ClassLoader loader) throws Exception {
        Class<?> checkerClass = Class.forName(CHECKER, true, loader);
        Method checkGcmLibraries = checkerClass.getMethod("checkGcmLibraries");
        try {
            checkGcmLibraries.invoke(null);
            fail("expected GcmLibraryChecker.checkGcmLibraries() to throw");
            return null;
        } catch (InvocationTargetException e) {
            return e.getCause();
        }
    }

    private static boolean hasFrame(Throwable t, String className, String methodName) {
        for (StackTraceElement element : t.getStackTrace()) {
            if (className.equals(element.getClassName()) && methodName.equals(element.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String render(Throwable t) {
        StringBuilder sb = new StringBuilder(t.toString());
        for (StackTraceElement element : t.getStackTrace()) {
            sb.append("\n\tat ").append(element);
        }
        return sb.toString();
    }

    /**
     * Defines the real GcmLibraryChecker bytecode itself (child-first) so that check()'s Class.forName
     * binds to THIS loader, then simulates a broken play-services-base dependency: when asked for the
     * first GMS class it throws NoClassDefFoundError (broken at runtime — an Error) or
     * ClassNotFoundException (absent — an Exception). Everything else delegates to the parent.
     */
    private static final class GmsLinkFailureClassLoader extends ClassLoader {
        private final boolean throwAsError;

        GmsLinkFailureClassLoader(ClassLoader parent, boolean throwAsError) {
            super(parent);
            this.throwAsError = throwAsError;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (GMS_CONNECTION_CALLBACKS.equals(name)) {
                if (throwAsError) {
                    throw new NoClassDefFoundError(name.replace('.', '/'));
                }
                throw new ClassNotFoundException(name);
            }
            if (CHECKER.equals(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    byte[] bytes = readClassBytes(name);
                    loaded = defineClass(name, bytes, 0, bytes.length);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
            return super.loadClass(name, resolve);
        }

        private byte[] readClassBytes(String name) throws ClassNotFoundException {
            String resource = name.replace('.', '/') + ".class";
            try (InputStream in = getParent().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new ClassNotFoundException(name);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return out.toByteArray();
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
