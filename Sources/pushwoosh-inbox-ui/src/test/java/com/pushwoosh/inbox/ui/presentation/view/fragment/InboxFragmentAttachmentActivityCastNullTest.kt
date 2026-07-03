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

package com.pushwoosh.inbox.ui.presentation.view.fragment

import android.content.Context
import android.content.Intent
import android.view.View
import com.pushwoosh.inbox.ui.presentation.view.activity.AttachmentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Regression guard for crash-inboxfragment-attachment-activity-cast-null (fix #29).
 *
 * [InboxFragment.onAttachmentClicked] is invoked from a banner-image `View.OnClickListener`
 * (`InboxViewHolder.kt:127`) via the fragment's own `attachmentClickListener` closure built at
 * `InboxFragment.kt:73`. Before the fix its first statement built `Intent(activity, …)` on a null
 * `getActivity()` (a detached / never-attached fragment), and Kotlin passed the nullable `activity`
 * to the platform-type `Intent(Context!, Class)` parameter with no inserted null-check, so the ctor
 * NPEd inside `ComponentName.<init>` — escaping unwrapped through the View callback to the main thread.
 *
 * The fix adds `val activity = activity ?: return` at the top of `onAttachmentClicked`, mirroring the
 * sibling safe-call discipline already at `:323` (`activity?.overridePendingTransition(...)`). A
 * detached-fragment banner click is now a graceful no-op: the method early-returns before building the
 * Intent or calling `startActivity`, so nothing is thrown.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InboxFragmentAttachmentActivityCastNullTest {

    /** Pull the EXACT prod closure InboxFragment registers at :73 (the banner OnClickListener target). */
    @Suppress("UNCHECKED_CAST")
    private fun prodClickListener(fragment: InboxFragment): (String, View) -> Unit {
        val field = InboxFragment::class.java.getDeclaredField("attachmentClickListener")
        field.isAccessible = true
        return field.get(fragment) as (String, View) -> Unit
    }

    // ---- Regression guard: the REAL prod closure (:73) on a detached fragment is a graceful no-op ----
    // Verifies that a banner click while getActivity()==null early-returns instead of NPE-ing at the
    // Intent ctor: the call completes without throwing AND never reaches startActivity. The
    // assertNull(getNextStartedActivity()) discriminator proves the method exited via the early-return
    // BEFORE reaching the body — without the guard the body throws (NPE at the Intent ctor on the null
    // activity), so a started activity would be impossible; with the guard the body is skipped entirely.

    @Test
    fun detachedFragmentBannerClick_isGracefulNoOp() {
        val fragment = InboxFragment() // never attached -> getActivity() == null
        // sanity: the fragment genuinely yields a null activity (the production condition)
        assertNull(fragment.activity)
        val view = View(RuntimeEnvironment.getApplication())

        prodClickListener(fragment).invoke("https://example.com/banner.png", view)

        assertNull(
            "detached-fragment banner click must early-return, not start AttachmentActivity",
            Shadows.shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        )
    }

    // ---- Negative controls isolate the necessary condition at the EXACT crash mechanism ------------
    // onAttachmentClicked:316 is `Intent(activity, AttachmentActivity::class.java)`. Attaching a real
    // InboxFragment to drive getActivity()!=null is off-target — it triggers the fragment's full
    // lifecycle (InboxPresenter -> PushwooshInbox.loadCachedMessages -> uninitialized-SDK
    // IllegalArgumentException), a different failure entirely. So the controls exercise the identical
    // `Intent(context, AttachmentActivity::class.java)` construction the crash site builds, varying
    // ONLY the context (null vs a real one) — that is precisely the `getActivity()` value the defect
    // turns on.

    private val CRASH_CLASS = "android.content.ComponentName"
    private val CRASH_METHOD = "<init>"

    @Test
    fun negativeControl_realContext_doesNotThrow() {
        val realContext: Context = RuntimeEnvironment.getApplication()
        // Same statement as :316, with a non-null context: builds cleanly, no ComponentName NPE.
        val intent = Intent(realContext, AttachmentActivity::class.java)
        assertNotNull("Intent must build with a real context", intent.component)
        assertEquals(
            "wrong target component",
            AttachmentActivity::class.java.name,
            intent.component?.className
        )
    }

    @Test
    fun negativeControl_nullContextIsTheNecessaryCondition() {
        // (a) null context (the getActivity()==null state) -> NPE at ComponentName.<init>
        val nullContext: Context? = null
        var threwOnNull = false
        try {
            @Suppress("DEPRECATION")
            Intent(nullContext, AttachmentActivity::class.java)
        } catch (t: NullPointerException) {
            if (t.stackTrace.any { it.className == CRASH_CLASS && it.methodName == CRASH_METHOD }) {
                threwOnNull = true
            }
        }
        assertTrue("Intent(null context, …) must NPE at ComponentName.<init>", threwOnNull)

        // (b) real context (the getActivity()!=null state) -> no NPE
        var threwOnReal = false
        try {
            Intent(RuntimeEnvironment.getApplication() as Context, AttachmentActivity::class.java)
        } catch (t: NullPointerException) {
            if (t.stackTrace.any { it.className == CRASH_CLASS && it.methodName == CRASH_METHOD }) {
                threwOnReal = true
            }
        }
        assertFalse("Intent(real context, …) must NOT NPE", threwOnReal)
    }
}
