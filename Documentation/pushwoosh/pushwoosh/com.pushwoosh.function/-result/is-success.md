//[pushwoosh](../../../index.md)/[com.pushwoosh.function](../index.md)/[Result](index.md)/[isSuccess](is-success.md)

# isSuccess

[main]\
open fun [isSuccess](is-success.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks whether the asynchronous operation completed successfully. 

 This method determines success by checking if the exception field is null. A null exception indicates that the operation completed without errors. 

**Usage pattern:** Always call this method first before accessing getData or getException to determine which accessor to use.  Example: 

```kotlin

	  if (result.isSuccess()) {
	      // Operation succeeded - safe to get data
	      String token = result.getData().getToken();
	  } else {
	      // Operation failed - safe to get exception
	      Exception error = result.getException();
	      Log.e("App", "Error: " + error.getMessage());
	  }
	
```

#### Return

true if the operation completed successfully (exception is null), false otherwise
