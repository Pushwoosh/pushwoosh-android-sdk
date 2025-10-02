//[pushwoosh-inbox-ui](../../index.md)/[com.pushwoosh.inbox.ui.model.customizing.formatter](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [DefaultDateFormatter](-default-date-formatter/index.md) | [androidJvm]<br>class [DefaultDateFormatter](-default-date-formatter/index.md)(dateFormat: [DateFormat](https://developer.android.com/reference/kotlin/java/text/DateFormat.html) = SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.getDefault())) : [InboxDateFormatter](-inbox-date-formatter/index.md) |
| [Formatter](-formatter/index.md) | [androidJvm]<br>interface [Formatter](-formatter/index.md)&lt;in [Input](-formatter/index.md), out [Output](-formatter/index.md)&gt; |
| [InboxDateFormatter](-inbox-date-formatter/index.md) | [androidJvm]<br>interface [InboxDateFormatter](-inbox-date-formatter/index.md) : [Formatter](-formatter/index.md)&lt;[Date](https://developer.android.com/reference/kotlin/java/util/Date.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt; <br>Set up default format of date into inbox cell |

## Properties

| Name | Summary |
|---|---|
| [DEFAULT_DATE_FORMAT](-d-e-f-a-u-l-t_-d-a-t-e_-f-o-r-m-a-t.md) | [androidJvm]<br>const val [DEFAULT_DATE_FORMAT](-d-e-f-a-u-l-t_-d-a-t-e_-f-o-r-m-a-t.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>Default format of the inbox date |
