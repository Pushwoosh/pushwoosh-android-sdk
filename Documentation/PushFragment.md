# Class PushFragment #

Package `com.pushwoosh.fragment`

PushFragment encapsulates the Pushwoosh logic for Fragment integration.
In order to set up Pushwoosh via PushFragment you should follow these steps:
* PushFragment's parent Activity should implements PushEvenListener
```java
public class PushFragmentActivity extends FragmentActivity implements PushEventListener
```

* In the onCreate method of PushFragment's parent activity you should invoke `init(FragmentActivity)`
```java
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		//Init Pushwoosh fragment
		PushFragment.init(this);
```

* Don't forget to invoke `PushFragment.onNewIntent` on `onNewIntent(Intent intent))` method of PushFragment's parent activity
```java
	@Override
	public void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);

		//Check if we've got new intent with a push notification
		PushFragment.onNewIntent(this, intent);
	}
```


## Method Summary
[public void register()](#register)  
[public void unregister()](#unregister)  
[public static void init(FragmentActivity activity)](#init)  
[public static void onNewIntent(FragmentActivity activity, Intent intent)](#onnewintent)  

---
### register

Registers for push notification.

```java
public void register()
```
* **pushToken** - push token

---
### unregister

Unregisters from push notifications.

```java
public void unregister()
```

---
### init

Initializes PushFragment.

```java
public static void init(FragmentActivity activity)
```
* **activity** - main activity which implemented PushEvenListener

---
### onNewIntent

Call this when main activity receives onNewIntent.

```java
public static void onNewIntent(FragmentActivity activity, Intent intent)
```
