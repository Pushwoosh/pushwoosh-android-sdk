//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[sendInappPurchase](send-inapp-purchase.md)

# sendInappPurchase

[main]\
open fun [sendInappPurchase](send-inapp-purchase.md)(sku: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), price: [BigDecimal](https://developer.android.com/reference/kotlin/java/math/BigDecimal.html), currency: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Sends in-app purchase statistics to Pushwoosh. 

 Purchase information is automatically stored in the following default tags: 

- &quot;In-app Product&quot; - product SKU
- &quot;In-app Purchase&quot; - purchase amount
- &quot;Last In-app Purchase date&quot; - purchase timestamp

 Example: ```kotlin

  // Track in-app purchase
  Pushwoosh.getInstance().sendInappPurchase(
      "premium_subscription",
      new BigDecimal("9.99"),
      "USD"
  );

```

#### Parameters

main

| | |
|---|---|
| sku | purchased product ID |
| price | price of the product |
| currency | currency of the price (ex: &quot;USD&quot;) |
