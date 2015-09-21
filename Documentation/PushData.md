# Class PushData #

Package `com.pushwoosh.notification`

Push notification settings.

## Method Summary
[getExtras](#getextras)  
[getHeader](#getheader)  
[getMessage](#getmessage)  
[isAppOnForeground](#isapponforeground)  
[getSoundType](#getsoundtype)  
[getVibrateType](#getvibratetype)  
[isSilent](#issilent)  
[isLocal](#islocal)  
[getIconBackgroundColor](#geticonbackgroundcolor)  
[getLed](#getled)  
[getSound](#getsound)  
[getVibration](#getvibration)  
[getTicker](#getticker)  
[getBigPicture](#getbigpicture)  
[getLargeIcon](#getlargeicon)  
[getSmallIcon](#getsmallicon)  
[getPriority](#getpriority)  
[getBadges](#getbadges)  
[getVisibility](#getvisibility)  

---
### getExtras

Returns push notification bundle extras.

```java
public Bundle getExtras()
```

---
### getHeader

Returns notification title.

```java
public String getHeader()
```

---
### getMessage

Returns notification text message.

```java
public String getMessage()
```

---
### isAppOnForeground

Indicates that notification is received in foreground.

```java
public boolean isAppOnForeground()
```

---
### getSoundType

Returns notification sound type.

```java
public SoundType getSoundType()
```

---
### getVibrateType

Returns notification vibrate type.

```java
public VibrateType getVibrateType()
```

---
### isSilent

Indicates that push notification is silent.

```java
public boolean isSilent()
```

---
### isLocal

Indicates that notification is local.

```java
public boolean isLocal()
```

---
### getIconBackgroundColor

Returns notification icon background color or null if no color is set.

```java
public Integer getIconBackgroundColor()
```

---
### getLed

Returns led color for notification or null if no color is set.

```java
public Integer getLed()
```

---
### getSound

Returns custom sound rousource name or null if no sound is set.

```java
public String getSound()
```

---
### getVibration

Returns true if remote vibration is set.

```java
public boolean getVibration()
```

---
### getTicker

Returns notification ticker.

```java
public String getTicker()
```

---
### getBigPicture

Returns bitmap for BigPictureStyle.

```java
public Bitmap getBigPicture()
```

---
### getLargeIcon

Returns bitmap for notification large icon.

```java
public Bitmap getLargeIcon()
```

---
### getSmallIcon

Returns resource id for small icon.

```java
public int getSmallIcon()
```

---
### getPriority

Returns notification priority.

```java
public int getPriority()
```

---
### getBadges

Returns notification icon badge number.

```java
public int getBadges()
```

---
### getVisibility

Returns notification visibility.

```java
public int getVisibility()
```
