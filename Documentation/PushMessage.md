# Class PushMessage #

Package `com.pushwoosh.sender`

PushMessage provides logic for sending push notifications from Android device.

## Fields
[public String content](#content)  
[public Map<String, Object> notificationParams](#notificationParams)

---
### content

Push notification text 

```java
public String content
```

---
### notificationParams

Additional notification parameters that are merged into request.notifications[0] dictionary of [createMessage](http://docs.pushwoosh.com/docs/createmessage) request

```java
public Map<String, Object> notificationParams
```
