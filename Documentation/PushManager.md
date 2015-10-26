# Class PushManager #

Package `com.pushwoosh`

Push notifications manager.


## Method Summary
[initializePushManager](#initializepushmanager)  
[getInstance](#getinstance)  
[onStartup](#onstartup)  
[getPushToken](#getpushtoken)  
[getPushwooshHWID](#getpushwooshhwid)  
[registerForPushNotifications](#registerforpushnotifications)  
[unregisterForPushNotifications](#unregisterforpushnotifications)  
[getCustomData](#getcustomdata)  
[sendTags](#sendtags)  
[getLaunchNotification](#getlaunchnotification)  
[clearLaunchNotification](#clearlaunchnotification)  
[setRichPageListener](#setrichpagelistener)  
[getTagsAsync](#gettagsasync)  
[getTagsSync](#gettagssync)  
[setUserId](#setuserid)  
[setMultiNotificationMode](#setmultinotificationmode)  
[setSimpleNotificationMode](#setsimplenotificationmode)  
[setSoundNotificationType](#setsoundnotificationtype)  
[setVibrateNotificationType](#setvibratenotificationtype)  
[setLightScreenOnNotification](#setlightscreenonnotification)  
[setEnableLED](#setenableled)  
[setColorLED](#setcolorled)  
[setBeaconBackgroundMode](#setbeaconbackgroundmode)  
[trackInAppRequest](#trackinapprequest)  
[scheduleLocalNotification](#schedulelocalnotification)  
[clearLocalNotifications](#clearlocalnotifications)  
[clearNotificationCenter](#clearnotificationcenter)  
[startTrackingGeoPushes](#starttrackinggeopushes)  
[stopTrackingGeoPushes](#stoptrackinggeopushes)  
[startTrackingBeaconPushes](#starttrackingbeaconpushes)  
[stopTrackingBeaconPushes](#stoptrackingbeaconpushes)  
[getPushHistory](#getpushhistory)  
[clearPushHistory](#clearpushhistory)  
[setBadgeNumber](#setbadgenumber)  
[addBadgeNumber](#addbadgenumber)  
[getBadgeNumber](#getbadgenumber)  
[setNotificationFactory](#setnotificationfactory)

---
### initializePushManager

Init push manager with Pushwoosh App ID and Project ID for GCM. Use either this function or place the values in `AndroidManifest.xml` as per documentation.

```java
public static void initializePushManager(Context context,
                                         java.lang.String appId,
                                         java.lang.String projectId)
```
* **appId** - Pushwoosh Application ID
* **projectId** - ProjectID from Google GCM

---
### getInstance

Returns current instance of PushManager. Can return null if Project ID or Pushwoosh App Id not given.

```java
public static PushManager getInstance(Context context)
```

---
### onStartup

Must be called after initialization on app start. Tracks app open for Pushwoosh stats. To register for push notifications use `registerForPushNotifications`.

```java
public void onStartup(Context context)
               throws java.lang.Exception
```

* **Throws** - `java.lang.Exception` - push notifications are not available on the device, or prerequisites are not met.

---
### getPushToken

Gets push notification token. May be null if not registered for push notifications yet.

```java
public static java.lang.String getPushToken(Context context)
```

---
### getPushwooshHWID

Gets Pushwoosh HWID. Unique device identifier that is used in API communication with Pushwoosh.

```java
public static java.lang.String getPushwooshHWID(Context context)
```

---
### registerForPushNotifications

Registers for push notifications.

```java
public void registerForPushNotifications()
```

---
### unregisterForPushNotifications

Unregister from push notifications

```java
public void unregisterForPushNotifications()
```

---
### getCustomData

Get push notification user data

```java
public java.lang.String getCustomData(Bundle pushBundle)
```
* **Returns** - String userdata, or null

---
### sendTags

Send tags asynchronously. Calling listener callbacks with result.

```java
public static void sendTags(Context context,
                            java.util.Map<java.lang.String,java.lang.Object> tags,
                            SendPushTagsCallBack listener)
```
* **tags** - Tags to send.
* **listener** - Callback to receive set tags result.

---
### getLaunchNotification

Returns launch notification if the app was started in response to push notification.

```java
public java.lang.String getLaunchNotification()
```
* **Returns** - string-formatted JSON payload of launch push notification

---
### clearLaunchNotification

Clears launch notifiation, getLaunchNotification() will return null after this call.

```java
public void clearLaunchNotification()
```

---
### setRichPageListener

Sets new rich page action listener.

```java
public void setRichPageListener(PushManager.RichPageListener listener)
```

---
### getTagsAsync

Get tags from Pushwoosh service asynchronously.

```java
public static void getTagsAsync(Context context,
                                PushManager.GetTagsListener listener)
```

---
### getTagsSync

Get tags from Pushwoosh service synchronously. Returns tags or null.

```java
public static Map<String, Object> getTagsSync(Context context)
```

---
### setUserId

Set User indentifier. This could be Facebook ID, username or email, or any other user ID. This allows data and events to be matched across multiple user devices.

```java
public void setUserId(Context context,
                      java.lang.String userId)
```

---
### setMultiNotificationMode

Allows multiple notifications in notification bar.

```java
public static void setMultiNotificationMode(Context context)
```

---
### setSimpleNotificationMode

Allows only the last notification in notification bar.

```java
public static void setSimpleNotificationMode(Context context)
```

---
### setSoundNotificationType

Change default sound notification type. Could be overriden from Pushwoosh Control Panel.

```java
public static void setSoundNotificationType(Context context,
                                            SoundType soundNotificationType)
```

---
### setVibrateNotificationType

Change vibration notification type.  Could be overriden from Pushwoosh Control Panel.

```java
public static void setVibrateNotificationType(Context context,
                                              VibrateType vibrateNotificationType)
```

---
### setLightScreenOnNotification

Enable/disable screen light when notification message arrives.

```java
public static void setLightScreenOnNotification(Context context,
                                                boolean lightsOn)
```

---
### setEnableLED

Enable/disable LED highlight when notification message arrives.

```java
public static void setEnableLED(Context context,
                                boolean ledOn)
```

---
### setColorLED

Set LED color for notification messages.

```java
public static void setColorLED(Context context,
                               int color)
```

---
### setColorLED

Set LED color for notification messages.

```java
public static void setColorLED(Context context,
                               int color)
```

---
### setBeaconBackgroundMode

Notify that apps go to background when tracking beacons.

```java
public static void setBeaconBackgroundMode(Context context,
                                           boolean backgroundMode)
```

---
### trackInAppRequest

Track in-app purchase.

```java
public static void trackInAppRequest(Context context,
                                     java.lang.String sku,
                                     java.math.BigDecimal price,
                                     java.lang.String currency,
                                     java.util.Date purchaseTime)
```
* **sku** - purchased product ID
* **price** - price for the product
* **currency** - currency of the price (ex: "USD")
* **purchaseTime** - time of the purchase (ex: new Date())

---
### scheduleLocalNotification

Schedules a local notification. Returns local notification id.

```java
public static int scheduleLocalNotification(Context context,
                                             java.lang.String message,
                                             int seconds)
```
* **message** - notification message
* **seconds** - delay (in seconds) until the message will be sent

---
### scheduleLocalNotification

Schedules a local notification with extras Extras parameters: 
* title - message title, same as message parameter
* l - link to open when notification has been tapped 
* b - banner URL to show in the notification instead of text 
* u - user data 
* i - identifier string of the image from the app to use as the icon in the notification 
* ci - URL of the icon to use in the notification

Returns local notification id.
```java
public static int scheduleLocalNotification(Context context,
                                             java.lang.String message,
                                             Bundle extras,
                                             int seconds)
```
* **message** - notification message
* **extras** - notification extra parameters
* **seconds** - delay (in seconds) until the message will be sent

---
### clearLocalNotification

Removes scheduled local notification with given id

```java
public static void clearLocalNotification(Context context, int id) 
```

---
### clearLocalNotifications

Removes all scheduled local notifications

```java
public static void clearLocalNotifications(Context context)
```

---
### clearNotificationCenter

Removes all notifications from the system tray.

```java
public static void clearNotificationCenter(Context context)
```

---
### startTrackingGeoPushes

Start tracking Geo Push Notifications.

```java
public void startTrackingGeoPushes()
```

---
### stopTrackingGeoPushes

Stop tracking Geo Push Notifications.

```java
public void stopTrackingGeoPushes()
```

---
### startTrackingBeaconPushes

Starts tracking Beacon Push Notifications.

```java
public void startTrackingBeaconPushes()
```

---
### stopTrackingBeaconPushes

Stop tracking Beacon Push Notifications.

```java
public void stopTrackingBeaconPushes()
```

---
### getPushHistory

Returns push history stored locally. Only last **PushManager.PUSH_HISTORY_CAPACITY** pushes are stored.

```java
public ArrayList<String> getPushHistory() 
```

---
### clearPushHistory

Clears all stored push history.

```java
public void clearPushHistory() 
```

---
### setBadgeNumber

Sets application icon badge number.

```java
public void setBadgeNumber(int badgeNumber)
```

---
### addBadgeNumber

Increments application icon badge number.

```java
public void addBadgeNumber(int deltaBadgeNumber)
```

---
### getBadgeNumber

Returns current application icon badge number.

```java
public int getBadgeNumber()
```

---
### setNotificationFactory

Sets notification factory for customizing push notifications. **notificationFactory** class name is stored in preferences so that notificationFactory can be recreated again using reflection when closed application receives push notification. **DefaultNotificatoinFactory** will be used to create notifications if no custom notification factory is set.

*See [Android FAQ](http://docs.pushwoosh.com/docs/android-faq#customizing-push-notifications) for more information.*

```java
public void setNotificationFactory(AbsNotificationFactory notificationFactory)
```
