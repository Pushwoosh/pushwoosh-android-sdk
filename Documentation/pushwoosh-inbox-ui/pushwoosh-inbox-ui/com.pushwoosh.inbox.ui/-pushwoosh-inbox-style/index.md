//[pushwoosh-inbox-ui](../../../index.md)/[com.pushwoosh.inbox.ui](../index.md)/[PushwooshInboxStyle](index.md)

# PushwooshInboxStyle

[androidJvm]\
object [PushwooshInboxStyle](index.md)

## Properties

| Name | Summary |
|---|---|
| [accentColor](accent-color.md) | [androidJvm]<br>var [accentColor](accent-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>Accent color of inbox cell. By default used AppCompat R.attr.colorAccent |
| [backgroundColor](background-color.md) | [androidJvm]<br>var [backgroundColor](background-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The color of cell background. By default used Android android.R.attr.windowBackground |
| [barAccentColor](bar-accent-color.md) | [androidJvm]<br>var [barAccentColor](bar-accent-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The bar accent color. By default used Android android.R.attr.textColorSecondary |
| [barBackgroundColor](bar-background-color.md) | [androidJvm]<br>var [barBackgroundColor](bar-background-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The bar color. By default used Android android.R.attr.textColorSecondary |
| [barTextColor](bar-text-color.md) | [androidJvm]<br>var [barTextColor](bar-text-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The bar text color. By default used Android android.R.attr.textColorPrimary |
| [barTitle](bar-title.md) | [androidJvm]<br>var [barTitle](bar-title.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?<br>The bar text title. |
| [dateColor](date-color.md) | [androidJvm]<br>var [dateColor](date-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The date color of unread messages. By default used Android android.R.attr.textColorSecondary |
| [dateFormatter](date-formatter.md) | [androidJvm]<br>var [dateFormatter](date-formatter.md): [InboxDateFormatter](../../com.pushwoosh.inbox.ui.model.customizing.formatter/-inbox-date-formatter/index.md)<br>Inbox message date format. By default used {@link com.pushwoosh.inbox.ui.model.customizing.formatter.InboxDateFormatter#DEFAULT_DATE_FORMAT} format |
| [dateTextSize](date-text-size.md) | [androidJvm]<br>var [dateTextSize](date-text-size.md): [Float](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-float/index.html)?<br>The date text size. |
| [defaultImageIcon](default-image-icon.md) | [androidJvm]<br>@[DrawableRes](https://developer.android.com/reference/kotlin/androidx/annotation/DrawableRes.html)<br>var [defaultImageIcon](default-image-icon.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The icon shown near the message if there's no icon in push payload. If not specified, the app icon is used |
| [defaultImageIconDrawable](default-image-icon-drawable.md) | [androidJvm]<br>var [defaultImageIconDrawable](default-image-icon-drawable.md): [Drawable](https://developer.android.com/reference/kotlin/android/graphics/drawable/Drawable.html)? |
| [descriptionColor](description-color.md) | [androidJvm]<br>var [descriptionColor](description-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The text color of unread messages. By default used Android android.R.attr.textColorSecondary |
| [descriptionTextSize](description-text-size.md) | [androidJvm]<br>var [descriptionTextSize](description-text-size.md): [Float](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-float/index.html)?<br>The description text size. |
| [dividerColor](divider-color.md) | [androidJvm]<br>var [dividerColor](divider-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The divider color. By default used Android android.R.attr.listDivider |
| [EMPTY_ANIMATION](-e-m-p-t-y_-a-n-i-m-a-t-i-o-n.md) | [androidJvm]<br>const val [EMPTY_ANIMATION](-e-m-p-t-y_-a-n-i-m-a-t-i-o-n.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Use this property for clearing list animation {@link #listAnimationResource} |
| [highlightColor](highlight-color.md) | [androidJvm]<br>var [highlightColor](highlight-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The color of cell highlight. By default used AppCompat R.attr.colorControlHighlight |
| [imageTypeColor](image-type-color.md) | [androidJvm]<br>var [imageTypeColor](image-type-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The color of the unread message action icon (Deep Link, URL, etc.). By default used {@link #accentColor} |
| [listAnimation](list-animation.md) | [androidJvm]<br>var [listAnimation](list-animation.md): [Animation](https://developer.android.com/reference/kotlin/android/view/animation/Animation.html)? |
| [listAnimationResource](list-animation-resource.md) | [androidJvm]<br>@[AnimRes](https://developer.android.com/reference/kotlin/androidx/annotation/AnimRes.html)<br>var [listAnimationResource](list-animation-resource.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Item appearing animation. Set {@link #EMPTY_ANIMATION} for clear animation |
| [listEmptyImage](list-empty-image.md) | [androidJvm]<br>var [listEmptyImage](list-empty-image.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The image which is displayed if the list of inbox messages is empty |
| [listEmptyImageDrawable](list-empty-image-drawable.md) | [androidJvm]<br>var [listEmptyImageDrawable](list-empty-image-drawable.md): [Drawable](https://developer.android.com/reference/kotlin/android/graphics/drawable/Drawable.html)? |
| [listEmptyText](list-empty-text.md) | [androidJvm]<br>var [listEmptyText](list-empty-text.md): [CharSequence](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-char-sequence/index.html)?<br>The text which is displayed if the list of inbox messages is empty |
| [listErrorImage](list-error-image.md) | [androidJvm]<br>var [listErrorImage](list-error-image.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The image which is displayed if an error occurs and the list of inbox messages is empty |
| [listErrorImageDrawable](list-error-image-drawable.md) | [androidJvm]<br>var [listErrorImageDrawable](list-error-image-drawable.md): [Drawable](https://developer.android.com/reference/kotlin/android/graphics/drawable/Drawable.html)? |
| [listErrorMessage](list-error-message.md) | [androidJvm]<br>var [listErrorMessage](list-error-message.md): [CharSequence](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-char-sequence/index.html)?<br>The error text which is displayed when an error occurs |
| [readDateColor](read-date-color.md) | [androidJvm]<br>var [readDateColor](read-date-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The date color of read messages. By default used Android android.R.attr.textColorSecondary |
| [readDescriptionColor](read-description-color.md) | [androidJvm]<br>var [readDescriptionColor](read-description-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The text color of read messages. By default used Android android.R.attr.textColorSecondary |
| [readImageTypeColor](read-image-type-color.md) | [androidJvm]<br>var [readImageTypeColor](read-image-type-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The color of the read message action icon. By default used {@link #readDateColor} |
| [readTitleColor](read-title-color.md) | [androidJvm]<br>var [readTitleColor](read-title-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The title color of read messages. By default used Android android.R.attr.textColorSecondary |
| [titleColor](title-color.md) | [androidJvm]<br>var [titleColor](title-color.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)?<br>The title color of unread messages. By default used Android android.R.attr.textColorPrimary |
| [titleTextSize](title-text-size.md) | [androidJvm]<br>var [titleTextSize](title-text-size.md): [Float](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-float/index.html)?<br>The title text size. |

## Functions

| Name | Summary |
|---|---|
| [clearColors](clear-colors.md) | [androidJvm]<br>fun [clearColors](clear-colors.md)()<br>Clear all setting colors |
| [setDateFont](set-date-font.md) | [androidJvm]<br>fun [setDateFont](set-date-font.md)(typeface: [Typeface](https://developer.android.com/reference/kotlin/android/graphics/Typeface.html)) |
| [setDescriptionFont](set-description-font.md) | [androidJvm]<br>fun [setDescriptionFont](set-description-font.md)(typeface: [Typeface](https://developer.android.com/reference/kotlin/android/graphics/Typeface.html)) |
| [setTitleFont](set-title-font.md) | [androidJvm]<br>fun [setTitleFont](set-title-font.md)(typeface: [Typeface](https://developer.android.com/reference/kotlin/android/graphics/Typeface.html)) |
