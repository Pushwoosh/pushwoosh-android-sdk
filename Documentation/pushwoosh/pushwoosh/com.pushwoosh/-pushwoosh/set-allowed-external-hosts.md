//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setAllowedExternalHosts](set-allowed-external-hosts.md)

# setAllowedExternalHosts

[main]\
open fun [setAllowedExternalHosts](set-allowed-external-hosts.md)(allowedExternalHosts: [ArrayList](https://developer.android.com/reference/kotlin/java/util/ArrayList.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;)

Sets the list of allowed external hosts for secure push content. 

 This method configures which external hosts are allowed to serve content referenced in push notifications (such as images). This is a security feature to prevent unauthorized content from being loaded. Only hosts in this list will be allowed.  Example: 

```kotlin

  ArrayList<String> allowedHosts = new ArrayList<>();
  allowedHosts.add("cdn.mycompany.com");
  allowedHosts.add("images.example.com");

  Pushwoosh.getInstance().setAllowedExternalHosts(allowedHosts);

```

#### Parameters

main

| | |
|---|---|
| allowedExternalHosts | list of allowed external host names |
