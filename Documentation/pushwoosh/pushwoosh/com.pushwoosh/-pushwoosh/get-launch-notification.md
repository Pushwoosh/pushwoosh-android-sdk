//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[getLaunchNotification](get-launch-notification.md)

# getLaunchNotification

[main]\
open fun [getLaunchNotification](get-launch-notification.md)(): [PushMessage](../../com.pushwoosh.notification/-push-message/index.md)

Returns the push notification that launched the application. 

 This method returns the push message data if the app was started by tapping a push notification. Returns null if the app was launched normally or if [clearLaunchNotification](clear-launch-notification.md) was called.  Example: 

```kotlin

  
  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      // Check if app was launched from a push notification
      PushMessage launchNotification = Pushwoosh.getInstance().getLaunchNotification();
      if (launchNotification != null) {
          // Extract custom data for deep linking
          String screen = launchNotification.getCustomData().getString("screen");
          String productId = launchNotification.getCustomData().getString("product_id");
          String articleId = launchNotification.getCustomData().getString("article_id");

          Log.d("App", "Launched from push: " + launchNotification.getMessage());

          // Navigate to specific screen based on push data
          if ("product_details".equals(screen) && productId != null) {
              // Open product details screen
              Intent intent = new Intent(this, ProductDetailsActivity.class);
              intent.putExtra("product_id", productId);
              startActivity(intent);
          } else if ("article".equals(screen) && articleId != null) {
              // Open article screen
              Intent intent = new Intent(this, ArticleActivity.class);
              intent.putExtra("article_id", articleId);
              startActivity(intent);
          } else if ("promotions".equals(screen)) {
              // Open promotions screen
              startActivity(new Intent(this, PromotionsActivity.class));
          }

          // Clear to prevent reprocessing
          Pushwoosh.getInstance().clearLaunchNotification();
      }
  }

```

#### Return

Launch notification data or null
