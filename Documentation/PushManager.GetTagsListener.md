# Interface PushManager.GetTagsListener #

Package `com.arellomobile.android.push`

Listener for `getTags` method.

---
### onTagsReceived

Called when tags received.

```java
void onTagsReceived(java.util.Map<java.lang.String,java.lang.Object> tags)
```
* **tags** - received tags map

---
### onError

Called when request failed.

```java
void onError(java.lang.Exception e)
```
* **e** - error exception
