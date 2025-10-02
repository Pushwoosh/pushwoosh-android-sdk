//[pushwoosh](../../../index.md)/[com.pushwoosh.notification](../index.md)/[VibrateType](index.md)

# VibrateType

enum [VibrateType](index.md)

Push notification vibration setting. Application must use [VIBRATE](https://developer.android.com/reference/android/Manifest.permission.html#VIBRATE) permission in order for vibration to work.

#### See also

| |
|---|
| [PushwooshNotificationSettings](../-pushwoosh-notification-settings/set-vibrate-notification-type.md) |

## Entries

| | |
|---|---|
| [DEFAULT_MODE](-d-e-f-a-u-l-t_-m-o-d-e/index.md) | [main]<br>[DEFAULT_MODE](-d-e-f-a-u-l-t_-m-o-d-e/index.md)<br>Notification causes vibration if AudioManager ringer mode is [RINGER_MODE_VIBRATE](https://developer.android.com/reference/android/media/AudioManager.html#RINGER_MODE_VIBRATE). |
| [NO_VIBRATE](-n-o_-v-i-b-r-a-t-e/index.md) | [main]<br>[NO_VIBRATE](-n-o_-v-i-b-r-a-t-e/index.md)<br>Notification will not cause vibration. |
| [ALWAYS](-a-l-w-a-y-s/index.md) | [main]<br>[ALWAYS](-a-l-w-a-y-s/index.md)<br>Notification will always cause vibration. |

## Properties

| Name | Summary |
|---|---|
| [value](value.md) | [main]<br>val [value](value.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [fromInt](from-int.md) | [main]<br>open fun [fromInt](from-int.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [VibrateType](index.md) |
| [valueOf](value-of.md) | [main]<br>open fun [valueOf](value-of.md)(name: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [VibrateType](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [main]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[VibrateType](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
