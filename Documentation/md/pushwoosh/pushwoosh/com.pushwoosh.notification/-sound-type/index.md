//[pushwoosh](../../../index.md)/[com.pushwoosh.notification](../index.md)/[SoundType](index.md)

# SoundType

enum [SoundType](index.md)

Push notification sound setting.

#### See also

| |
|---|
| [PushwooshNotificationSettings](../-pushwoosh-notification-settings/set-sound-notification-type.md) |

## Entries

| | |
|---|---|
| [DEFAULT_MODE](-d-e-f-a-u-l-t_-m-o-d-e/index.md) | [main]<br>[DEFAULT_MODE](-d-e-f-a-u-l-t_-m-o-d-e/index.md)<br>Sound is played when notification arrives if AudioManager ringer mode is [RINGER_MODE_NORMAL](https://developer.android.com/reference/android/media/AudioManager.html#RINGER_MODE_NORMAL). |
| [NO_SOUND](-n-o_-s-o-u-n-d/index.md) | [main]<br>[NO_SOUND](-n-o_-s-o-u-n-d/index.md)<br>Sound is never played when notification arrives. |
| [ALWAYS](-a-l-w-a-y-s/index.md) | [main]<br>[ALWAYS](-a-l-w-a-y-s/index.md)<br>Sound is always played when notification arrives. |

## Properties

| Name | Summary |
|---|---|
| [value](value.md) | [main]<br>val [value](value.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [fromInt](from-int.md) | [main]<br>open fun [fromInt](from-int.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [SoundType](index.md) |
| [valueOf](value-of.md) | [main]<br>open fun [valueOf](value-of.md)(name: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [SoundType](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [main]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[SoundType](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
