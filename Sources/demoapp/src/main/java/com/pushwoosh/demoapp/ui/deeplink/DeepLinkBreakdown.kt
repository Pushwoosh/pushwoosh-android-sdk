package com.pushwoosh.demoapp.ui.deeplink

/**
 * Formats the parts of a deep link URI into the lines shown on the deep link screen.
 *
 * Deliberately free of `android.net.Uri` so it stays a plain JVM unit test — the demoapp has no
 * Robolectric on its test classpath.
 */
object DeepLinkBreakdown {

    private const val ABSENT = "—"

    /**
     * @param host URI host, e.g. `demo` in `pwdemo://demo/screen?id=42`
     * @param path URI path, e.g. `/screen`
     * @param queryParams query parameters in the order they should be displayed
     * @return one `name=value` line per part; missing host or path render as an em dash
     */
    fun rows(host: String?, path: String?, queryParams: List<Pair<String, String>>): List<String> {
        val rows = mutableListOf<String>()
        rows.add("host=" + host.orAbsent())
        rows.add("path=" + path.orAbsent())
        queryParams.forEach { (name, value) -> rows.add("$name=$value") }
        return rows
    }

    private fun String?.orAbsent(): String = if (isNullOrEmpty()) ABSENT else this
}
