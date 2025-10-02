//[pushwoosh](../../../index.md)/[com.pushwoosh.notification](../index.md)/[NotificationFactory](index.md)/[channelName](channel-name.md)

# channelName

[main]\
open fun [channelName](channel-name.md)(channelName: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)

#### Return

name that you want to assign to the channel on its creation. Note that empty namewill be ignored and default channel name will be assigned to the channel instead

#### Parameters

main

| | |
|---|---|
| channelName | name of the channel specified in Android payload as &quot;pw_channel&quot; attribute.If no attribute was specified, parameter gives default channel name |
