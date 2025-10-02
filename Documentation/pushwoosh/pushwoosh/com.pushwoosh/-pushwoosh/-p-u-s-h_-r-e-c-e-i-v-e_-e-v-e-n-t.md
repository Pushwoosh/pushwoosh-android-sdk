//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[PUSH_RECEIVE_EVENT](-p-u-s-h_-r-e-c-e-i-v-e_-e-v-e-n-t.md)

# PUSH_RECEIVE_EVENT

[main]\
val [PUSH_RECEIVE_EVENT](-p-u-s-h_-r-e-c-e-i-v-e_-e-v-e-n-t.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) = &quot;PUSH_RECEIVE_EVENT&quot;

Intent extra key for push notification payload. Is added to intent that starts Activity when push notification is clicked.  Example: 

```kotlin

  
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      if (getIntent().hasExtra(Pushwoosh.PUSH_RECEIVE_EVENT)) {
          // Activity was started in response to push notification
          showMessage("Push message is " + getIntent().getExtras().getString(Pushwoosh.PUSH_RECEIVE_EVENT));
      }
  }

```
