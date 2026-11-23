package com.pushwoosh.inline.internal.view

import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.pushwoosh.inline.internal.FakeClock
import com.pushwoosh.inline.internal.FakeIdentity
import com.pushwoosh.inline.internal.FakeScheduler
import com.pushwoosh.inline.internal.FakeTransport
import com.pushwoosh.inline.internal.InlineManager
import com.pushwoosh.inline.internal.model.InlineAction
import com.pushwoosh.inline.internal.model.InlineBanner
import com.pushwoosh.inline.internal.nativeBanner
import com.pushwoosh.inline.internal.net.InlineEventSink
import com.pushwoosh.inline.internal.store.InlineStore
import com.pushwoosh.tags.TagsBundle
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowToast
import java.io.File
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InlineActionPerformerTest {

    @get:Rule
    val folder = TemporaryFolder()

    /** What happened, in order: sent events, started activities, posted events. */
    private val effects = mutableListOf<String>()
    private lateinit var clock: FakeClock
    private lateinit var identity: FakeIdentity
    private lateinit var store: InlineStore
    private lateinit var manager: InlineManager
    private lateinit var context: RecordingContext

    /** A fresh manager holding banner `b1` in slot `a`, with its own cache file. */
    private fun prepare(extra: JSONObject.() -> Unit = {}) {
        clock = FakeClock(NOW_MS)
        val scheduler = FakeScheduler(clock)
        identity = FakeIdentity()
        store = InlineStore(File(folder.newFolder(), "pushwoosh/inline-banners.json"), clock, scheduler, Executor { it.run() })
        manager = InlineManager(store, FakeTransport(), RecordingSink(), identity, clock, scheduler)
        manager.onForeground()
        manager.onEventAnswer(JSONObject().put("slots", JSONObject().put("a", nativeBanner("b1", extra = extra))), identity.userId, identity.appCode)
        context = RecordingContext(RuntimeEnvironment.getApplication())
    }

    private class Case(
        val name: String,
        val action: InlineAction,
        val expected: List<String>,
        val unconfirmedUser: Boolean = false,
        val noActivity: Boolean = false,
        val blocked: Boolean = false,
        val crashes: Boolean = false,
        val clipboard: String? = null,
        val toast: String? = null,
    )

    // Verifies that url/deeplink start ACTION_VIEW after the click event (file: links and failing starts do not crash), copy fills the clipboard, postEvent is skipped in the unconfirmed window.
    @Test
    fun testActionsRunAfterClick() {
        val cases = listOf(
            Case("url", InlineAction.Url("https://example.com/x"), listOf("click:cta", "view:https://example.com/x new_task")),
            Case("deeplink", InlineAction.Deeplink("app://product/1"), listOf("click:cta", "view:app://product/1 new_task")),
            Case("no activity for the link", InlineAction.Deeplink("nobody://x"), listOf("click:cta", "view:nobody://x new_task"), noActivity = true),
            Case("link blocked by a security policy", InlineAction.Url("https://example.com/b"), listOf("click:cta", "view:https://example.com/b new_task"), blocked = true),
            Case("deeplink to a local file", InlineAction.Deeplink("file:///sdcard/x.html"), listOf("click:cta")),
            Case("deeplink to a local file, upper case", InlineAction.Deeplink("FILE:///sdcard/x.html"), listOf("click:cta")),
            Case("link start fails at runtime", InlineAction.Deeplink("app://boom"), listOf("click:cta", "view:app://boom new_task"), crashes = true),
            Case(
                "postEvent",
                InlineAction.PostEvent("restock", JSONObject().put("sku", "42")),
                listOf("click:cta", "postEvent:restock sku=42"),
            ),
            Case("postEvent while the user change is unconfirmed", InlineAction.PostEvent("restock", null), listOf("click:cta"), unconfirmedUser = true),
            Case("copy", InlineAction.Copy("CODE10", null), listOf("click:cta"), clipboard = "CODE10", toast = "Copied"),
            Case("copy with toast", InlineAction.Copy("CODE20", "Code copied"), listOf("click:cta"), clipboard = "CODE20", toast = "Code copied"),
            Case("close", InlineAction.Close, listOf("dismiss:null")),
        )
        for (c in cases) {
            prepare()
            effects.clear()
            context.noActivity = c.noActivity
            context.blocked = c.blocked
            context.crashes = c.crashes
            if (c.unconfirmedUser) identity.userId = "u2"

            performer().perform("a", banner(), "cta", c.action)

            assertEquals("case ${c.name}", c.expected, effects)
            c.clipboard?.let { assertEquals("case ${c.name}", it, clipboardText()) }
            c.toast?.let { assertEquals("case ${c.name}", it, ShadowToast.getTextOfLatestToast()) }
        }
    }

    // Verifies that no url, deeplink, postEvent or copy action runs and no click is reported for a banner that expired before the tap while its timer did not run.
    @Test
    fun testExpiredBannerRunsNoAction() {
        val actions = listOf(
            InlineAction.Url("https://example.com/x"),
            InlineAction.Deeplink("app://product/1"),
            InlineAction.PostEvent("restock", null),
            InlineAction.Copy("CODE10", null),
        )
        for (action in actions) {
            prepare { put("expireDate", NOW_MS / 1000 + 60) }
            val banner = banner()
            clock.nowMillis += 3_600_000L
            effects.clear()

            performer().perform("a", banner, "cta", action)

            assertEquals("action $action", emptyList<String>(), effects)
            assertNull("action $action", clipboardText())
            assertNull("action $action", store.banner("a"))
        }
    }

    // Verifies that a deeplink a strict URI parser refuses reaches ACTION_VIEW exactly as the server sent it, after its click.
    @Test
    fun testDeeplinkReachesActionViewUnchanged() {
        for (link in listOf("myapp://", "my_app://screen", "myapp://path?x=a b", "https://cdn.example.com/a[1].png")) {
            prepare { put("action", JSONObject().put("type", "deeplink").put("value", link)) }
            effects.clear()
            val banner = banner()

            performer().perform("a", banner, null, checkNotNull(banner.action))

            assertEquals("link $link", listOf("click:null", "view:$link new_task"), effects)
        }
    }

    private fun performer() = InlineActionPerformer(context, manager) { event, attributes ->
        effects += "postEvent:$event" + attributes.describe()
    }

    private fun TagsBundle?.describe(): String =
        this?.toJson()?.let { json -> json.keys().asSequence().sorted().joinToString("") { " $it=${json.get(it)}" } }.orEmpty()

    private fun banner(): InlineBanner = checkNotNull(store.banner("a"))

    private fun clipboardText(): String? {
        val clipboard = RuntimeEnvironment.getApplication().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    }

    private inner class RecordingSink : InlineEventSink {
        override fun send(event: JSONObject, onResult: (delivered: Boolean) -> Unit) {
            effects += "${event.getString("type")}:${event.opt("elementId") ?: "null"}"
        }
    }

    /**
     * Records started activities; [noActivity] = nothing on the device handles the intent, [blocked] = the start is
     * not permitted, [crashes] = the start throws another runtime exception (as `FileUriExposedException` does).
     */
    private inner class RecordingContext(base: Context) : ContextWrapper(base) {
        var noActivity = false
        var blocked = false
        var crashes = false

        override fun startActivity(intent: Intent) {
            val newTask = if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) " new_task" else ""
            effects += "view:${intent.data}$newTask".takeIf { intent.action == Intent.ACTION_VIEW } ?: "other:${intent.action}"
            if (noActivity) throw ActivityNotFoundException()
            if (blocked) throw SecurityException()
            if (crashes) throw IllegalStateException()
        }
    }

    private companion object {
        const val NOW_MS = 1_800_000_000_123L
    }
}
