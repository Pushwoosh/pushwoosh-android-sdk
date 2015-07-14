# Interface PushEventListener #

Package `com.arellomobile.android.push.fragment`

Interface for Pushwoosh Android SDK integration via Fragments.  


## Method Summary
[void doOnRegistered(java.lang.String pushToken)](#doonregistered)  
[void doOnMessageReceive(java.lang.String pushPayload)](#doonmessagereceive)  
[void doOnUnregistered(java.lang.String prevToken)](#doonunregistered)  
[void doOnUnregisteredError(java.lang.String error)](#doonunregisterederror)  
[void doOnRegisteredError(java.lang.String error)](#doonregisterederror)  

---
### doOnRegistered

Registered for push notifications.

```java
void doOnRegistered(java.lang.String pushToken)
```
* **pushToken** - push token

---
### doOnMessageReceive

Push notification has been received.

```java
void doOnMessageReceive(java.lang.String pushPayload)
```
* **pushPayload** - push notifications payload (JSON object as String)

---
### doOnUnregistered

Unregistration successful.

```java
void doOnUnregistered(java.lang.String prevToken)
```
* **prevToken** - unregistered push token

---
### doOnUnregisteredError

Error during unregistration.

```java
void doOnUnregisteredError(java.lang.String error)
```
* **error** - error description

---
### doOnRegisteredError

Error during registration.

```java
void doOnRegisteredError(java.lang.String error)
```
* **error** - error description
