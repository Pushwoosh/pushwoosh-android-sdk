//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[registerWhatsappNumber](register-whatsapp-number.md)

# registerWhatsappNumber

[main]\
open fun [registerWhatsappNumber](register-whatsapp-number.md)(number: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Registers a WhatsApp number for the current user. 

 This method associates a WhatsApp number with the device, allowing you to send WhatsApp messages through Pushwoosh multichannel campaigns.  Example: 

```kotlin

  // Register WhatsApp number with country code
  Pushwoosh.getInstance().registerWhatsappNumber("+1234567890");

```

#### Parameters

main

| | |
|---|---|
| number | WhatsApp phone number with country code (e.g., &quot;+1234567890&quot;) |
