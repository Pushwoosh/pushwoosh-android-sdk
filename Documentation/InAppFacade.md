# Class InAppFacade #

Package `com.pushwoosh.inapp`

In-App Messages API.


## Method Summary
[postEvent](#postevent)  
[addJavascriptInterface](#addjavascriptinterface)  
[removeJavascriptInterface](#removejavascriptinterface)  

---
### postEvent

Post events for In-App Messages. This can trigger In-App message display as specified in Pushwoosh Control Panel.
 
```java
public static void postEvent(Activity activity, 
							 String event, 
							 Map<String, Object> attributes, 
							 PostEventCallback callback)
```

* **activity** - activity context
* **event** - event name
* **attributes** - additional event attributes
* **callback** - postEvent completion handler

Example:

```java 
Map<String, Object> attributes = new HashMap<>();
attributes.put("buttonNumber", 4);
attributes.put("buttonLabel", "Banner");
InAppFacade.postEvent(MainActivity.this, "buttonPressed", "buttonNumber", new InAppFacade.PostEventCallback() {
	@Override
	public void onSuccess() {
		// Handle successful postEvent
	}

	@Override
	public void onError(String s) {
		// Handle error
	}
});
```

---
### addJavascriptInterface

Adds Java object that will accessible from In-App html page JavaScript. 
Methods accessible from JavaScript should be annotated with ```@JavascriptInterface```.

```java
public static void addJavascriptInterface(Object object, String name)
```

* **object** - Java object
* **name** - name of JavaScript object


Example:<br>
**LoggerJS.java**
```java
public class LoggerJS {
	@JavascriptInterface
    public void log(String message) {
    	android.util.Log.i("LoggerJS", message);
	}
}
```

**SomeClass.java**
```java
...
InAppFacade.addJavascriptInterface(new LoggerJS(), "nativeLogger");
```

**index.js**
```js
nativeLogger.log("Some string");
```

---
### removeJavascriptInterface

Removes JavaScript interface previously registered with [addJavascriptInterface](#addjavascriptinterface).

```java
public static void removeJavascriptInterface(String name)
```

* **name** - name of JavaScript object
