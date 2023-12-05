#Change xxx.DemoMessageRreceiver to the full class name defined in your app
-keep class com.pushwoosh.xiaomi.PushwooshXiaomiMessageReceiver {*;}

#SDK has been obfuscated and compressed to avoid class not found error due to re-obfuscation.
-keep class com.xiaomi.**

#If the compiling Android version you are using is 23, you can prevent getting a false warning which makes it impossible to compile.
-dontwarn com.xiaomi.push.**

-keep class com.xiaomi.mipush.sdk.MiPushMessage {*;}
-keep class com.xiaomi.mipush.sdk.MiPushCommandMessage {*;}

-keep class com.xiaomi.mipush.sdk.PushMessageReceiver {*;}

-keep class com.xiaomi.mipush.sdk.MessageHandleService {*;}

-keep class com.xiaomi.push.service.XMJobService {*;}

-keep class com.xiaomi.push.service.XMPushService {*;}

-keep class com.xiaomi.mipush.sdk.PushMessageHandler {*;}

-keep class com.xiaomi.push.service.receivers.NetworkStatusReceiver {*;}

-keep class com.xiaomi.push.service.receivers.PingReceiver {*;}

-keep class com.xiaomi.mipush.sdk.NotificationClickedActivity {*;}