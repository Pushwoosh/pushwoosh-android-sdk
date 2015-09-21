# Class AbsNotificationFactory #

Package `com.pushwoosh.notification`

Abstract notification factory.

## Method Summary
[onGenerateNotification](#ongeneratenotification)  
[onPushReceived](#onpushreceived)  
[onPushHandle](#onpushhandle)  
[addCancel](#addcancel)  
[addLED](#addled)  
[addVibration](#addvibration)  
[addSound](#addsound)  
[setNotifyIntent](#setnotifyintent)  
[getPushData](#getpushdata)  
[getContext](#getcontext)  

---
### **Abstract methods**

### onGenerateNotification

Callback for notification generation. Is called every time application receives remote or local notification. Is not called for silent pushes.

Returns notification to show. If onGenerateNotification returns null no notification will be shown.

```java
public abstract Notification onGenerateNotification(PushData pushData)
```
* **pushData** - push notification settings

---
### onPushReceived

Notification delivery callback. Is called every time application receives remote or local notification.

```java
public abstract void onPushReceived(PushData pushData)
```
* **pushData** - push notification settings

---
### onPushHandle

Notification tap callback. Is called every time user opens notification.

```java
public abstract void onPushHandle(Activity activity)
```
* **activity** - activity that handles push notification


---
### **Helper methods for generating notification**

### addCancel

Makes notification cancellable.

```java
protected final void addCancel(Notification notification)
```

---
### addLED

Sets led color.

```java
protected final void addLED(Notification notification, boolean enable, int color)
```

---
### addVibration

Adds vibration.

```java
protected final void addVibration(Notification notification, final String vibration)
```

---
### addSound

Adds custom sound.

```java
protected final void addSound(Notification notification, String sound)
```
* **sound** - resource name for custom sound.

---
### setNotifyIntent

Sets custom notify intent for all notifications.

```java
public void setNotifyIntent(Intent notifyIntent)
```

---
### getNotifyIntent

Returns current notify intent that is used for all notifications.

```java
public Intent getNotifyIntent()
```

---
### getPushData

Returns notification settings for currently received notification.

```java
public PushData getPushData()
```

---
### getContext

Returns context.

```java
protected final Context getContext()
```
