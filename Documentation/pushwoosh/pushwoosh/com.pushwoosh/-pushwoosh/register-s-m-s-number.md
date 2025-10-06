//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[registerSMSNumber](register-s-m-s-number.md)

# registerSMSNumber

[main]\
open fun [registerSMSNumber](register-s-m-s-number.md)(number: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Registers an SMS number for the current user. 

 This method associates an SMS phone number with the device, allowing you to send SMS messages through Pushwoosh multichannel campaigns.  Example: 

```kotlin

  // Register SMS number with country code
  Pushwoosh.getInstance().registerSMSNumber("+1234567890");

```

#### Parameters

main

| | |
|---|---|
| number | SMS phone number with country code (e.g., &quot;+1234567890&quot;) |
