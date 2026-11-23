package com.pushwoosh.inline.internal.html

import com.pushwoosh.inapp.view.js.PushwooshJSInterface
import com.pushwoosh.inline.internal.html.InlineHtmlTapCoordinator.Kind
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

/** Scripts an HTML banner runs in its page. */
internal object InlineScripts {

    /** The JS interface [HEIGHT] reports to. */
    const val HEIGHT_INTERFACE = "pushwooshInlineHeight"

    /**
     * Posts the document height (CSS px) to [HEIGHT_INTERFACE] whenever it changes: the root's height
     * in standards mode; in quirks mode, where the root fills the viewport, the lowest child of the
     * body plus its bottom spacing; or the bottom of a lower absolute or fixed box. Same algorithm as iOS.
     */
    const val HEIGHT = """
        (function() {
            var root = document.documentElement, reported = -1;
            function bottomOf(rect, bottom) {
                return rect.width || rect.height ? Math.max(bottom, rect.bottom) : bottom;
            }
            function quirksBottom(body) {
                var range = document.createRange(), bottom = 0;
                for (var i = 0; i < body.childNodes.length; i++) {
                    var node = body.childNodes[i];
                    if (node.nodeType === 1) {
                        bottom = bottomOf(node.getBoundingClientRect(), bottom);
                    } else if (node.nodeType === 3) {
                        range.selectNode(node);
                        bottom = bottomOf(range.getBoundingClientRect(), bottom);
                    }
                }
                if (!bottom) return 0;
                var style = getComputedStyle(body);
                return bottom + parseFloat(style.paddingBottom) + parseFloat(style.borderBottomWidth) +
                    parseFloat(style.marginBottom);
            }
            function positionedBottom(body) {
                var bottom = 0, nodes = body.getElementsByTagName('*');
                for (var i = 0; i < nodes.length; i++) {
                    var position = getComputedStyle(nodes[i]).position;
                    if (position === 'absolute' || position === 'fixed') {
                        bottom = bottomOf(nodes[i].getBoundingClientRect(), bottom);
                    }
                }
                return bottom;
            }
            function report() {
                var body = document.body, height = 0;
                if (document.compatMode === 'CSS1Compat') {
                    height = root.getBoundingClientRect().height;
                } else if (body) {
                    height = quirksBottom(body);
                }
                if (body) height = Math.max(height, positionedBottom(body));
                height = Math.ceil(height + window.scrollY);
                if (height !== reported) {
                    reported = height;
                    pushwooshInlineHeight.postMessage(height);
                }
            }
            var resizes = new ResizeObserver(report);
            function observe() {
                resizes.observe(root);
                var nodes = root.getElementsByTagName('*');
                for (var i = 0; i < nodes.length; i++) resizes.observe(nodes[i]);
            }
            new MutationObserver(function() { observe(); report(); })
                .observe(root, { childList: true, subtree: true });
            window.addEventListener('load', report);
            observe();
            report();
        })();
        """

    /** Turns off text selection and the long-press callout in the banner page, as iOS does. */
    const val NO_SELECTION = """
        (function() {
            var style = document.createElement('style');
            style.textContent = '* { -webkit-user-select: none; user-select: none; -webkit-touch-callout: none; }';
            (document.head || document.documentElement).appendChild(style);
        })();
        """

    private const val OWN_ACTION_SELECTOR = "a[href],button,input,select,textarea,label,[onclick],[role=button],[data-pw-id]"

    /** What a tap hit, answered by [hitTest]. */
    class Hit(val kind: Kind, val id: String?)

    /** Finds what the page shows at ([xCss], [yCss]) CSS px of the viewport; the answer is read by [parseHit]. */
    fun hitTest(xCss: Double, yCss: Double): String = """
        (function(x, y) {
            var e = document.elementFromPoint(x, y);
            if (!e || !e.closest('$OWN_ACTION_SELECTOR')) return { kind: 'none', id: null };
            var marked = e.closest('[data-pw-id]');
            if (marked) return { kind: 'marked', id: marked.getAttribute('data-pw-id') };
            return { kind: 'control', id: null };
        })($xCss, $yCss);
    """.trimIndent()

    /** `null` = the page gave no answer. */
    fun parseHit(result: String?): Hit? {
        val json = try {
            JSONObject(result ?: return null)
        } catch (e: JSONException) {
            return null
        }
        val kind = when (json.optString("kind")) {
            "none" -> Kind.NONE
            "marked" -> Kind.MARKED
            "control" -> Kind.CONTROL
            else -> return null
        }
        val id = if (json.isNull("id")) null else json.optString("id")
        return Hit(kind, id)
    }

    /** `window.pushwoosh` for an inline banner: the user's values and empty in-app and Rich Media codes. */
    fun pushwooshApi(hwid: String, version: String, appCode: String, userId: String, deviceType: String): String =
        String.format(
            Locale.ROOT,
            PushwooshJSInterface.PUSHWOOSH_JS,
            jsString(hwid),
            jsString(version),
            jsString(appCode),
            jsString(userId),
            "",
            jsString(deviceType),
            "",
            "",
        )

    /** [value] escaped for a double-quoted JS string literal. */
    private fun jsString(value: String): String = JSONObject.quote(value).let { it.substring(1, it.length - 1) }
}
