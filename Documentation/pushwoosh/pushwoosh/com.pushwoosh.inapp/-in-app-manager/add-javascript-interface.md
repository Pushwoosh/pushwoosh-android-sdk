//[pushwoosh](../../../index.md)/[com.pushwoosh.inapp](../index.md)/[InAppManager](index.md)/[addJavascriptInterface](add-javascript-interface.md)

# addJavascriptInterface

[main]\
open fun [addJavascriptInterface](add-javascript-interface.md)(object: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), name: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Add JavaScript interface for In-Apps extension. All exported methods should be marked with @JavascriptInterface annotation.

#### Parameters

main

| | |
|---|---|
| object | java object that will be available inside In-App page |
| name | specified object will be available as window.`name` |
