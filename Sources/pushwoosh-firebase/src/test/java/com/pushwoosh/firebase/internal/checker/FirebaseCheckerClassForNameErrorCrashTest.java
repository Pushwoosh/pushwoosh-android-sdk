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

package com.pushwoosh.firebase.internal.checker;

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
 * Regression guard for crash-firebasechecker-classforname-error.
 *
 * <p>FirebaseChecker.check() resolves the hardcoded class name
 * "com.google.firebase.messaging.FirebaseMessaging" with the one-arg Class.forName (initialize=true).
 * If firebase-messaging is present at compile time but broken at runtime (excluded transitive dep,
 * R8-stripped class, POM drift), the JVM surfaces a NoClassDefFoundError / ExceptionInInitializerError
 * / LinkageError — subtypes of Error, not ClassNotFoundException. Before the fix the catch caught ONLY
 * ClassNotFoundException, so that load-time Error slipped past it (and past FcmRegistrar.init(), which
 * has no try/catch), escaped SDK init and crashed the app on startup. The fix widened the catch to
 * ClassNotFoundException | LinkageError, so the load-time Error is now rewrapped into an
 * IllegalStateException — an Exception swallowed upstream by onCreate's catch(Exception), exactly like
 * the missing-class path — instead of escaping.
 *
 * <p>The class name is a fixed SDK constant and the real FirebaseMessaging is on the test classpath, so
 * a throwing-clinit fixture (the sibling forName-Error technique) does not apply here. Instead the
 * broken-build condition is injected at the class-loading boundary: a loader that defines the real
 * FirebaseChecker bytecode fresh (so its Class.forName binds to this loader) and, when asked for
 * FirebaseMessaging, throws an Error. This drives the real check() with the real hardcoded name and the
 * real one-arg Class.forName; only the dependency graph is a stand-in — which is exactly the signal's
 * root cause.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class FirebaseCheckerClassForNameErrorCrashTest {

    private static final String CHECKER = "com.pushwoosh.firebase.internal.checker.FirebaseChecker";
    private static final String FIREBASE_MESSAGING = "com.google.firebase.messaging.FirebaseMessaging";

    @Test
    public void check_firebaseMessagingBrokenAtRuntime_linkageErrorSwallowedIntoIllegalState() throws Exception {
        Throwable thrown =
                invokeCheckWith(new FirebaseLinkFailureClassLoader(getClass().getClassLoader(), true));

        assertNotNull("check() must have thrown", thrown);
        // The crux of the fix: the broken-linkage Error no longer escapes as an Error. The widened
        // catch(ClassNotFoundException | LinkageError) intercepts it and rewraps it into an
        // IllegalStateException — an Exception swallowed upstream by onCreate's catch(Exception),
        // exactly like the missing-class path.
        assertTrue(
                "expected the graceful IllegalStateException, got " + thrown, thrown instanceof IllegalStateException);
        assertFalse(
                "the load-time Error must no longer escape — it is rewrapped into an Exception",
                thrown instanceof Error);
        // Non-vacuous + cause preserved: the ISE wraps the original NoClassDefFoundError (the
        // broken-linkage branch), NOT a ClassNotFoundException (the absent branch of the negative
        // control below). This proves the widened LinkageError catch fired, not the pre-existing
        // ClassNotFoundException one — flip the injected type and this cause changes.
        assertTrue(
                "cause must be the broken-linkage NoClassDefFoundError, got " + thrown.getCause(),
                thrown.getCause() instanceof NoClassDefFoundError);
        // Origin proof: the swallowed error was born at the eager Class.forName in FirebaseChecker.check.
        assertTrue(
                "cause stack must contain FirebaseChecker.check frame, was:\n" + render(thrown.getCause()),
                hasFrame(thrown.getCause(), CHECKER, "check"));
    }

    @Test
    public void check_firebaseMessagingAbsent_classNotFoundIsSwallowedIntoIllegalState() throws Exception {
        // Negative control (the guarded branch): a genuinely missing class surfaces as
        // ClassNotFoundException — an Exception — which check() DOES catch and rewrap into an
        // IllegalStateException (still an Exception, swallowed upstream by onCreate's catch(Exception)).
        // Same call site, same input shape; only the thrown type differs. Flip Error -> Exception and
        // the crash disappears, so "the failure is an Error" is the necessary condition, not noise.
        Throwable thrown =
                invokeCheckWith(new FirebaseLinkFailureClassLoader(getClass().getClassLoader(), false));

        assertNotNull("check() must have thrown", thrown);
        assertTrue("expected IllegalStateException, got " + thrown, thrown instanceof IllegalStateException);
        assertTrue("the guarded type must be an Exception", thrown instanceof Exception);
        assertFalse("the guarded type must NOT be an Error", thrown instanceof Error);
    }

    private Throwable invokeCheckWith(ClassLoader loader) throws Exception {
        Class<?> checkerClass = Class.forName(CHECKER, true, loader);
        Object checker = checkerClass.getDeclaredConstructor().newInstance();
        Method check = checkerClass.getMethod("check");
        try {
            check.invoke(checker);
            fail("expected FirebaseChecker.check() to throw");
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
     * Defines the real FirebaseChecker bytecode itself (child-first) so that check()'s Class.forName
     * binds to THIS loader, then simulates a broken firebase-messaging dependency: when asked for
     * FirebaseMessaging it throws NoClassDefFoundError (broken at runtime — an Error) or
     * ClassNotFoundException (absent — an Exception). Everything else delegates to the parent.
     */
    private static final class FirebaseLinkFailureClassLoader extends ClassLoader {
        private final boolean throwAsError;

        FirebaseLinkFailureClassLoader(ClassLoader parent, boolean throwAsError) {
            super(parent);
            this.throwAsError = throwAsError;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (FIREBASE_MESSAGING.equals(name)) {
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
