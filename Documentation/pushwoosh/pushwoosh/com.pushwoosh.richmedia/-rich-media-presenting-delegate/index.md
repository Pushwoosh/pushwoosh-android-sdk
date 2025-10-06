//[pushwoosh](../../../index.md)/[com.pushwoosh.richmedia](../index.md)/[RichMediaPresentingDelegate](index.md)

# RichMediaPresentingDelegate

[main]\
interface [RichMediaPresentingDelegate](index.md)

Interface for Rich Media presentation managing.

## Functions

| Name | Summary |
|---|---|
| [onClose](on-close.md) | [main]<br>abstract fun [onClose](on-close.md)(richMedia: [RichMedia](../-rich-media/index.md))<br>Tells the delegate that Rich Media has been closed. |
| [onError](on-error.md) | [main]<br>abstract fun [onError](on-error.md)(richMedia: [RichMedia](../-rich-media/index.md), pushwooshException: [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md))<br>Tells the delegate that error during Rich Media presenting has been occured. |
| [onPresent](on-present.md) | [main]<br>abstract fun [onPresent](on-present.md)(richMedia: [RichMedia](../-rich-media/index.md))<br>Tells the delegate that Rich Media has been displayed. |
| [shouldPresent](should-present.md) | [main]<br>abstract fun [shouldPresent](should-present.md)(richMedia: [RichMedia](../-rich-media/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks the delegate whether the Rich Media should be displayed. |
