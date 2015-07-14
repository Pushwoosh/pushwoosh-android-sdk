# Interface PushEventListener #

Interface for Pushwoosh Android SDK integration via Fragments.  
Package `com.arellomobile.android.push.fragment`

## Method Summary
[void doOnRegistered(java.lang.String pushToken)](#initializewithappcodeappname)  
[void doOnMessageReceive(java.lang.String pushPayload)](#initializewithappcodeappname)  
[void doOnUnregistered(java.lang.String prevToken)](#initializewithappcodeappname)  
[void doOnUnregisteredError(java.lang.String error)](#initializewithappcodeappname)  
[void doOnUnregisteredError(java.lang.String error)](#initializewithappcodeappname)  

### doOnRegistered

Registered for push notifications.

```java
void doOnRegistered(java.lang.String pushToken)
```
* **pushToken** - push token


### doOnMessageReceive

Push notification has been received.

```java
void doOnMessageReceive(java.lang.String pushPayload)
```
* **pushPayload** - push notifications payload (JSON object as String)


### doOnUnregistered

Unregistration successful.

```java
void doOnUnregistered(java.lang.String prevToken)
```
* **prevToken** - unregistered push token


### doOnUnregisteredError

Error during unregistration.

```java
void doOnUnregisteredError(java.lang.String error)
```
* **error** - error description


### doOnRegisteredError

Error during registration.

```java
void doOnRegisteredError(java.lang.String error)
```
* **error** - error description
