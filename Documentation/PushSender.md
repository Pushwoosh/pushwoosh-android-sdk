# Class PushSender #

Package `com.pushwoosh.sender`

PushSender provides logic for sending push notifications from Android device.

Usage:
```java
PushSender sender = new PushSender(context, "YOUR_API_ACCESS_TOKEN");
PushMessage message = new PushMessage();
message.content = "Hello World!";
sender.sendPush(message);
```

## Method Summary
[public PushSender(Context context, String apiToken)](#pushsender)  
[public void sendPush(PushMessage message)](#sendpush)  

---
### PushSender

Constructs PushSender object.

```java
public PushSender(Context context, String accessToken)
```
* **context** - Android context
* **accessToken** - remote API [access token](https://cp.pushwoosh.com/api_access)

---
### sendPush

Send push message with given parameters to [PW_APPID](https://github.com/Pushwoosh/pushwoosh-android-sdk/blob/master/Documentation/AndroidManifest.md) subscribers

```java
public void sendPush(PushMessage message)
```
