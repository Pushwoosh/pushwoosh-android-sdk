//[pushwoosh](../../../index.md)/[com.pushwoosh.repository](../index.md)/[PushwooshRepository](index.md)

# PushwooshRepository

[main]\
open class [PushwooshRepository](index.md)

## Constructors

| | |
|---|---|
| [PushwooshRepository](-pushwoosh-repository.md) | [main]<br>constructor(requestManager: RequestManager, sendTagsProcessor: SendTagsProcessor, registrationPrefs: RegistrationPrefs, notificationPrefs: NotificationPrefs, requestStorage: RequestStorage) |

## Properties

| Name | Summary |
|---|---|
| [currentInAppCode](current-in-app-code.md) | [main]<br>open var [currentInAppCode](current-in-app-code.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [currentRichMediaCode](current-rich-media-code.md) | [main]<br>open var [currentRichMediaCode](current-rich-media-code.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [currentSessionHash](current-session-hash.md) | [main]<br>open var [currentSessionHash](current-session-hash.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |

## Functions

| Name | Summary |
|---|---|
| [communicationEnabled](communication-enabled.md) | [main]<br>open fun [communicationEnabled](communication-enabled.md)(enable: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)) |
| [getHwid](get-hwid.md) | [main]<br>open fun [getHwid](get-hwid.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getPushHistory](get-push-history.md) | [main]<br>open fun [getPushHistory](get-push-history.md)(): [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[PushMessage](../../com.pushwoosh.notification/-push-message/index.md)&gt; |
| [getTags](get-tags.md) | [main]<br>open fun [getTags](get-tags.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), GetTagsException&gt;) |
| [isCommunicationEnabled](is-communication-enabled.md) | [main]<br>open fun [isCommunicationEnabled](is-communication-enabled.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isDeviceDataRemoved](is-device-data-removed.md) | [main]<br>open fun [isDeviceDataRemoved](is-device-data-removed.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isGdprEnable](is-gdpr-enable.md) | [main]<br>open fun [isGdprEnable](is-gdpr-enable.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [prefetchTags](prefetch-tags.md) | [main]<br>open fun [prefetchTags](prefetch-tags.md)() |
| [removeAllDeviceData](remove-all-device-data.md) | [main]<br>open fun [removeAllDeviceData](remove-all-device-data.md)() |
| [sendAppOpen](send-app-open.md) | [main]<br>open fun [sendAppOpen](send-app-open.md)() |
| [sendEmailTags](send-email-tags.md) | [main]<br>open fun [sendEmailTags](send-email-tags.md)(tags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), email: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), listener: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), PushwooshException&gt;) |
| [sendInappPurchase](send-inapp-purchase.md) | [main]<br>open fun [sendInappPurchase](send-inapp-purchase.md)(sku: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), price: [BigDecimal](https://developer.android.com/reference/kotlin/java/math/BigDecimal.html), currency: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), purchaseTime: [Date](https://developer.android.com/reference/kotlin/java/util/Date.html)) |
| [sendPushDeliveredSync](send-push-delivered-sync.md) | [main]<br>open fun [sendPushDeliveredSync](send-push-delivered-sync.md)(hash: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), metaData: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [Result](../../com.pushwoosh.function/-result/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), NetworkException&gt; |
| [sendPushOpenedSync](send-push-opened-sync.md) | [main]<br>open fun [sendPushOpenedSync](send-push-opened-sync.md)(hash: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), metadata: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [Result](../../com.pushwoosh.function/-result/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), NetworkException&gt; |
| [sendTags](send-tags.md) | [main]<br>open fun [sendTags](send-tags.md)(tags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), listener: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), PushwooshException&gt;) |
