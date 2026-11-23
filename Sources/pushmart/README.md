# PushMart (Android)

PushMart is a showcase storefront for the Pushwoosh Android SDK. A marketer or a sales
engineer connects it to their own Pushwoosh project, walks through the shop and sees the
resulting events, tags and purchases in the Control Panel, receives pushes and lands on the
right screen from a deep link. It is the Android counterpart of the iOS `PushwooshSampleApp`
(same catalog, palette and copy).

The module lives only in this GitLab repository: it is not published to the stores and is not
part of the public sample mirror.

## Build and install

The module builds against the SDK sources in this repository (`:pushwoosh`,
`:pushwoosh-firebase`, `:pushwoosh-badge`, `:pushwoosh-inbox`, `:pushwoosh-inbox-ui`,
`:pushwoosh-inapp-ui`, `:pushwoosh-liveupdates`); no Maven
artifacts are involved.

```bash
cd Sources
./gradlew :pushmart:assembleDebug
adb install -r pushmart/build/outputs/apk/debug/pushmart-debug.apk
adb shell am start -n com.pushwoosh.pushmart/.MainActivity
```

Unit tests: `make unit-test module=pushmart` from the repository root.
Formatting: `make fmt file='pushmart/src/**/*.kt'`.

The manifest sets `com.pushwoosh.notification_icon_color` to the literal `#FF5A5F`, duplicating
the `pm_coral` color in `colors.xml`: the SDK reads this meta-data with `metaData.getInt`, which
only accepts a literal color, not a `@color/...` resource reference. If the brand color ever
changes, update both places.

## Connecting a project

PushMart starts as a guest on the demo project `5DF3B-09018` (the one `demoapp` and
`tools/send-push.bash` use). The guest code is `AppCode.DEMO` in the app, not manifest meta-data: on
every start `PushMartApp` calls `setAppId` with the stored personal code (`nuvo.appCode`) or, without
one, the guest code. Profile > Personal code switches to the shopper's own `XXXXX-XXXXX` code or back
to the guest: `unregisterForPushNotifications`, then `setAppId` with an inbox reload now and after
61 s (see Inbox), then `registerForPushNotifications`. When the notification permission was never
asked (Android 13+), the Push Primer comes first and only Turn on notifications registers; Not now
leaves the device unregistered on the new project.

## What each screen sends

| Screen / action | SDK call |
|---|---|
| App start | `RichMediaManager.setRichMediaType(MODAL)`, `setDefaultRichMediaConfig(BOTTOM, DROP_DOWN, SLIDE_RIGHT)`, `setRichMediaColorScheme(APP)`, `setShowPushnotificationAlert(!<Show alerts in-app>)`, `InAppManager.addJavascriptInterface(PushMartJsBridge, "PushMart")` (JS: `window.PushMart.execute()` / `executeWithParam(s)`) |
| Every return to the foreground | `postEvent("appOpen")` with empty attributes (not on rotation) |
| Push with `{"changeIcon": "xmas_sale"}` / `"nil"` | No SDK call; the launcher alias switches (`LauncherXmasSale` / `LauncherDefault`) when the app is not on screen; an unknown name changes nothing |
| Home > collection chip (New in / Gift ideas / Essentials) | Own code: `postEvent("showRichMedia" / "showRichMediaClient" / "showRichMediaServer")` with empty attributes; stays on Home. Guest: a sheet with six catalog picks, no SDK call |
| Home > season card / Open the edit | No SDK call; opens the Sale screen |
| Home > bell | No SDK call; switches to the Inbox tab |
| Home > Recently viewed | No SDK call; local `pushmart.recentlyViewed` (newest 8, recorded when a product sheet opens) |
| Add to bag | `postEvent("ProductAdd", {id})` only when the product is not in the bag yet, `PushwooshBadge.setBadgeNumber(count)` |
| Shop > bag button | `postEvent("BasketOpen", {products: [ids in the bag]})` |
| Wishlist heart | list tag `wishlist` append/remove |
| Price alert switch | list tag `price_alerts` append/remove |
| Checkout | `sendInappPurchase(sku, unitPrice, "USD")` once per bag line, one `setTags` with `In-app Product` (every product id ever bought), `In-app Purchase` (lifetime total in whole dollars) and `orders_placed` +1, `postEvent("CheckoutSuccess", {products: [ids]})`, `PushwooshBadge.setBadgeNumber(0)`; the order is saved on the device (`pushmart.orders`, newest 20). Notifications allowed: PushMart's own ongoing order notification `PUSHWOOSH_ORDER` (ProgressStyle, promoted, Android 16+; NotificationCompat below); no Pushwoosh call (see Live updates) |
| Bag > Clear | `PushwooshBadge.setBadgeNumber(0)`, `postEvent("CartCleared")` with empty attributes |
| Bag > Enter code (guest) | No SDK call; opens the Personal code sheet (applying a code: see Connecting a project) |
| Orders > Buy again (card or details) | `CartStore.add` for each item still in the catalog: `postEvent("ProductAdd", {id})` for each item not in the bag yet, `PushwooshBadge.setBadgeNumber(count)` |
| Orders > View details / Start shopping | No SDK call |
| Orders > Track on Lock Screen on / off (latest order card, order details) | On: the same order notification as Checkout, no Pushwoosh call; never asked (Android 13+): the Push Primer first. Off: `PushwooshLiveUpdates.endLiveUpdate("PUSHWOOSH_ORDER")` plus the app's own cancel (see Live updates) |
| Orders > More for you > Upcoming drops > Remind me / Cancel | No Pushwoosh call (Android has no scheduled Live Update). Remind me sets an inexact alarm after the picked delay, and the alarm posts PushMart's ongoing countdown under the drop id; Cancel removes the alarm and the notification; never asked (Android 13+): the Push Primer first (see Live updates) |
| Orders > More for you > Keep your picks close | No Pushwoosh call; up to 3 ongoing notifications `picks-<id>` for the wishlist products, and a tap or View item opens `pushwoosh://product/<id>`; tapping the row again removes them; empty wishlist: a hint, nothing pinned; never asked (Android 13+): the Push Primer first (see Live updates) |
| Profile > Orders / Offers shortcut | No SDK call; switches to the Orders / Inbox tab. The Offers card shows the unread count from the same inbox count observers as the tab badge ("Picked for you" at zero) |
| Profile > Offers & order updates > Push notifications switch on | Push Primer (see below). Notifications already allowed: `registerForPushNotifications()` straight away. Never asked (Android 13+): Turn on notifications → `registerForPushNotifications()`, which shows the system dialog and registers after the answer. Denied: Turn on notifications opens the app's notification settings, no SDK call. Not now: the switch goes back off, no SDK call |
| Profile > Offers & order updates > Push notifications switch off | `unregisterForPushNotifications()` |
| Profile > Reminders > Enable notifications (shown while notifications are not allowed) | the same as the switch on |
| Profile > Reminders > Remind me | `Pushwoosh.scheduleLocalNotification` with the reminder's title and text after the picked delay; never asked (Android 13+): the Push Primer first, scheduled once push is on |
| Profile > Your details > Save | `setEmail(email)` when the email is non-empty (on every save); `registerSMSNumber(number)` / `registerWhatsappNumber(number)` for a number that is filled in and differs from the stored one. Both numbers are stored on the device (`pushmart.smsNumber`, `pushmart.whatsappNumber`); a cleared number is stored empty with no SDK call. Nothing filled in: "Nothing to save", no SDK call |
| Profile > Your details > avatar | No SDK call: the system photo picker (`PickVisualMedia`, no permission), then a 320 px JPEG in `filesDir/pushmart_avatar.jpg`; Remove photo deletes it |
| Profile > Shopping preferences (open) | `getTags` → restores the favourite categories and the size (see Shopping preferences) |
| Profile > Shopping preferences > category tile | `setTags({fav_<category>: "true"/"false"})`, string values as on iOS |
| Profile > Shopping preferences > size (XS–XL) | `setTags({size: <size>})` with the callback; a failure rolls the pick back, "Couldn't save your size" |
| Profile > Shopping preferences > Save topics | `setTags({news_topics: [ids]})` with the callback (`new_in`, `sales`, `drops`): "Saved" and the picks are stored, or "Couldn't save your topics"; then `postEvent("Subscription Segments")` with empty attributes |
| Profile > Shopping preferences > Email me offers too | `setEmailTags({email_news: on}, email)` with the email from Your details and the callback; a failure rolls the switch back, "Couldn't update email". Disabled without an email |
| Inbox tab badge, header subtitle and Profile > Offers | `PushwooshInbox.loadCachedMessages` (local, no sync) for the first value, then `registerMessagesCountObserver`, `registerUnreadMessagesCountObserver` (MainActivity for the badge, the Inbox screen for the subtitle, Profile for the Offers card) |
| Inbox tab (list) | library `InboxFragment` from `pushwoosh-inbox-ui`, styled by `InboxStyling` (`PushwooshInboxStyle`): `loadMessages` on open / pull-to-refresh, read on display, swipe = `deleteMessage` |
| Inbox > tap a card | the SDK runs `PushwooshInbox.performAction`: the `l` link opens (a PushMart deep link routes in `MainActivity`), `rm` presents the rich media; `PushwooshInboxUi.onMessageClickListener` only logs |
| Inbox > ⋯ > Refresh, empty state > Refresh offers | `PushwooshInbox.loadMessages` (throttled by the SDK); its result sets the empty state's sync-error flag |
| Inbox > ⋯ > Mark all as read | `PushwooshInbox.loadMessages`, then `readMessages(<unread codes>)` |
| Inbox > ⋯ > Clear read offers | `PushwooshInbox.loadMessages`, then `deleteMessages(<read codes>)` |
| Inbox > ⋯ > Delete all | `PushwooshInbox.loadMessages`, then `deleteMessages(<all codes>)` |
| Inbox > card button: link, mark read, dismiss | `PushwooshInboxUi.onButtonClickListener` returns `true`; the SDK opens the URL (or the message's own `l` / `rm`), marks the message read or deletes it |
| Inbox > card button: custom `tag: save` with `sku` | list tag `wishlist` append when the product is not saved yet, banner "Saved to your wishlist"; returns `false` |
| Inbox > card button: custom `tag: snooze` with `seconds` | `Pushwoosh.scheduleLocalNotification` with the message title and text after `seconds` (60 when missing), banner "Reminder set"; never asked (Android 13+): the Push Primer first, as Reminders; returns `false` |
| Inbox > card button: custom `tag: share` with `text` | the system share chooser with `text` (or the message title); returns `false` |
| Inbox > card button: custom with an unknown or missing tag, or an unknown `sku` | returns `true`, the SDK default |
| Profile > App settings > Appearance (System / Light / Dark) | No SDK call: `AppCompatDelegate.setDefaultNightMode`, stored as `pushmart.theme`; rich media and native in-apps follow it (`RichMediaColorScheme.APP`) |
| Profile > App settings > Show alerts in-app | `setShowPushnotificationAlert(false)` when on, `(true)` when off (the Android flag is inverted, see App settings); stored as `pushmart.showForegroundAlerts` |
| Profile > About PushMart | No SDK call |
| Profile > Native in-apps (debug) > any layout button | `PushwooshInAppUi.present(config)` with that preset's local config (`inAppId` is `pushmart-<type>`, e.g. `pushmart-modal`, `pushmart-sheet-floating`). Nothing leaves the device |
| Profile > Developer tools (debug) > SDK log level chip | `PWLog.updateLogLevel("NONE" / "ERROR" / "WARN" / "INFO" / "DEBUG" / "NOISE")` |
| Profile > Developer tools (debug) > Selected pair (on open) | `Pushwoosh.getApplicationCode()`, `Pushwoosh.getBaseUrl()` |
| Profile > Developer tools (debug) > Reload in-app resources | `InAppManager.reloadInApps(callback)` |
| Profile > Developer tools (debug) > SDK version | `GeneralUtils.SDK_VERSION` |

Checkout sends one `sendInappPurchase` per bag line at the unit price, as iOS `sendPurchase` does:
neither SDK takes a quantity, and the Android SDK stamps the purchase date itself.

The launcher badge is local only: `PushwooshBadge.setBadgeNumber` drives the launcher icon
through ShortcutBadger and never sends a `badge` tag to the server. ShortcutBadger only keeps the
badge in sync on launchers it has a supported receiver for (Samsung, Huawei, Xiaomi, etc.); on
the stock Pixel launcher the call is a silent no-op — no number, no dot. Any dot seen on a Pixel
comes from Android's own notification dots for unread notifications, unrelated to this call.

The switch on Profile > Offers & order updates is on when the app's own `pushNotificationEnabled`
preference is set (the last register succeeded) and the system allows notifications; it is read
again on every return to the screen. `unregisterForPushNotifications` clears the SDK's own persisted token
(`RegistrationPrefs.clearPushRegistrationInfo`); Firebase keeps its token cache, so registering
again yields the same FCM token.

Requests and responses are visible in logcat in debug builds, which set `com.pushwoosh.log_level` to `NOISE` (release builds use `ERROR`). The inbox `[InboxRepository] updateEvent` lines the e2e scenarios assert on are `PWLog.noise`, invisible at `DEBUG`:

```bash
adb logcat -s Pushwoosh
```

## Inbox

The Inbox tab hosts the library `InboxFragment` from `pushwoosh-inbox-ui`
(`PushwooshInboxUi.createInboxFragment()`) under the app's own header, as iOS hosts
`PushwooshInboxKitViewController`. `InboxStyling` maps the theme onto the global, mutable
`PushwooshInboxStyle` in `OffersFragment.onAttach`, before the feed attaches, so a theme switch
restyles the restored cards:
- `accentColor`, `backgroundColor`, `highlightColor`, `dividerColor` and the title, description
  and date colors. The read colors equal the unread ones, so a promo does not grey out the moment
  read-on-display marks it; the unread dot still shows the state.
- `imageTypeColor` / `readImageTypeColor`, `showToolbar = false`,
  `richCardsHeuristicEnabled = true`, `automaticReadOnDisplay = true`.
- The empty and error texts and images, and a medium date + short time `dateFormatter`.

The feed's `inboxRecyclerView` gets the bottom padding `pm_tab_clearance`, so the list scrolls under
the floating tab bar.

The header subtitle and the tab badge start from the cached inbox (`loadCachedMessages`: every
message, and those not read yet, the SDK counters' rules) and then follow the SDK count observers,
emitting only once both counts are known, so the badge does not blink. App start runs no inbox sync:
the first `loadMessages` comes from opening the Inbox tab. The subtitle reads "Offers and updates
from PushMart" with nothing unread, otherwise "1 new offer" / "N new offers". The badge on the Inbox
tab shows the unread count, hides at 0 and shows "9+" above 9. Its observers live in
`MainActivity`, so it updates on any tab.

The ⋯ button opens a popup styled like the iOS context menu: Refresh (`loadMessages`), Mark all as
read (`loadMessages`, then `readMessages(<unread codes>)`), Clear read offers
(`deleteMessages(<read codes>)`) and Delete all (`deleteMessages(<all codes>)`). The actions are
silent, as on iOS; the feed redraws from the SDK's `InboxMessagesUpdatedEvent`. With no messages and
a sync without error an empty state ("No offers yet", "Refresh offers" = `loadMessages`) covers the
feed. The error flag follows the feed's own load results (inbox-ui `InboxRepository.addCallback`:
`FailedLoading` sets it, `SuccessLoading` and `InboxEmpty` clear it), so pull-to-refresh updates it
too, and so do the results of the ⋯ Refresh and "Refresh offers". Use Delete all to leave the inbox
clean between runs.

With the heuristic on, inbox-ui picks the card type from the payload (`displayType`, image, title):
banner, captioned, classic, carousel or video. The app overrides two library resources by name:
- `res/layout/pw_item_inbox_captioned.xml` is iOS `PromoCaptionedCell`: a 4:3 hero with 24dp
  corners, a meta row (unread dot, date, pin), the title (22sp bold), the body and the buttons. It
  keeps the library's view ids, which `CaptionedInboxViewHolder` binds; the icon sits in a hidden
  container because the iOS cell drops it. The library has no qualified variant of this layout, so
  the app's plain `layout/` wins.
- `pw_rich_card_button_radius` = `pm_radius_pill`: capsule buttons on every card.

Tapping a card makes the SDK call `PushwooshInbox.performAction`: an `l` link opens in the browser
or, for a PushMart deep link, routes in `MainActivity`; `rm` presents the rich media.
`PushwooshInboxUi.onMessageClickListener` (`InboxButtonActions`) only logs
`PushMartInbox selected <code>`.

Card buttons go through `PushwooshInboxUi.onButtonClickListener` (`InboxButtonActions`, as iOS
`handleCustomButton`), which logs `PushMartInbox button <title> tag=<tag>`. Link, `markRead` and
`dismiss` buttons return `true` and keep the SDK default. Custom buttons:
- `{"tag":"save","sku":"<product id>"}` adds the product to the wishlist when it is not there yet
  (list tag `wishlist`) and shows "Saved to your wishlist" with the product name.
- `{"tag":"snooze","seconds":30}` schedules `Pushwoosh.scheduleLocalNotification` with the message
  title and text after `seconds` (60 when missing) and shows "Reminder set". The title goes in the
  `header` extra, because `LocalNotification.Builder` has no title setter. When the notification
  permission was never asked (Android 13+), the Push Primer shows first and the reminder is
  scheduled once push is on, as on iOS. With notifications denied the reminder does not show.
- `{"tag":"share","text":"..."}` opens the system share chooser with `text`, or the message title.
- An unknown or missing tag, or a `sku` that is not in the catalog, returns `true` (the SDK default).

The SDK reports every button tap as an open before the listener runs, so the message turns read
whatever the listener returns. A captioned promo with buttons, as custom data of an inbox push with
an image:

```json
{"displayType":"captioned","pinned":true,"buttons":[
  {"title":"Save","action":"custom","tag":"save","sku":"dress_03"},
  {"title":"Remind me","action":"custom","tag":"snooze","seconds":30},
  {"title":"Share","action":"custom","tag":"share","text":"PushMart autumn edit"}]}
```

Differences from iOS that the Android SDK imposes (documented, not worked around):
- No pinned-first order: inbox-ui sorts by date, and `pinned` only shows the pin.
- CTA buttons are coral text on a 12% coral fill: `RichCardViewHolder` sets the button colors in
  code (iOS: solid coral with white text).
- The Control Panel tab selection resets when the Inbox is reopened (`InboxFragment` design).
- Refresh is `loadMessages`, throttled by the SDK the same as its own pull-to-refresh.
- No willDisplay / shouldDelete / didDismiss / Wallet callbacks: inbox-ui only has the message and
  button click listeners.

Every push sent by `tools/send-push.bash` lands in the inbox for 14 days. The `[InboxRepository]
updateEvent InboxEvent: ...` lines are `PWLog.noise` and only print while the Inbox tab is open.
Switching the personal code makes the SDK clear its local inbox (`AppIdChangedEvent`). Right after
`setAppId` the app posts `loadMessages` to the main looper, behind the wipe, and one more 61 s
later, and after each it pushes the cached counts to the badge and the subtitle, because the count
observers stay silent when a load is throttled or the database is wiped. The SDK's 60 s sync window
lives in memory and survives the switch, and there is no public forced sync, so the new project's
inbox shows at once when nothing synced in the last minute, otherwise within about 61 s.

## Profile

The Profile tab, as iOS `ProfileTabView`: "Hi there" with the profile photo (otherwise the email's
first letter, otherwise a person icon) and the email, which opens Your details; the Orders and
Offers shortcut cards, which switch tabs; the Personal code card; then the rows, each opening its
screen:

| Section | Row | Opens |
|---|---|---|
| Account | Your details | Your details |
| Account | Shopping preferences | Shopping preferences |
| Notifications | Offers & order updates | Offers & order updates (the push switch) |
| Notifications | Reminders | Reminders |
| App | App settings | App settings |
| App | About PushMart | About PushMart |
| Developer (debug builds only) | Native in-apps | Native in-apps |
| Developer (debug builds only) | Developer tools | Developer tools |

The footer is "PushMart version" with the app's `versionName`. The header, the photo and the
personal code are read again on every return to the tab.

## Push Primer

Every push opt-in goes through `PushPermission.enable` (`PushPrimer`), as iOS `PushPermission.enable`:
the Offers & order updates switch, Reminders > Enable notifications, a reminder (Reminders > Remind
me, the Inbox snooze button) and a Personal code switch while the permission was never asked. The
Android SDK has no push primer, so the primer is PushMart's own bottom sheet (`PushPrimerSheet`)
with the iOS copy and colors: "Don't miss a thing", Turn on notifications, Not now.

- Notifications already allowed (`NotificationManagerCompat.areNotificationsEnabled()`, which also
  covers Android 12 and lower with notifications on): no sheet, outcome `SUPPRESSED`.
- Never asked (Android 13+, not granted, `pushmart.pushPermissionRequested` unset): Turn on
  notifications stores the flag and returns `ACCEPTED`; the caller registers and the SDK shows the
  system dialog itself, so the system dialog never shows without the primer.
- Denied: Turn on notifications opens the app's notification settings (iOS
  `fallbackToSettings(true)`) and returns `DECLINED`.
- Not now, back, a swipe down, or a sheet that cannot show (no activity, state saved, one already
  up): `DECLINED`, no registration.

The primer is never shown at launch.

## Reminders

Profile > Reminders, as iOS `NotificationsView`: a preview of the local notification, three
reminders (My cart, Wishlist, New in, with the iOS titles and texts), hours / minutes / seconds
pickers (default 10 s) and Remind me, which schedules `Pushwoosh.scheduleLocalNotification` and shows
"Reminder set" with the delay. Remind me is disabled at a zero delay. The Enable notifications card
shows while the system does not allow notifications.

## Live updates

PushMart's counterpart of the iOS Live Activities is its own ongoing notification on the
`pushmart_live` channel ("Live updates": default importance, no sound, no vibration), posted with
tag `PUSHWOOSH_ORDER` and id `"PUSHWOOSH_ORDER".hashCode()`, the key the SDK renderer gives a Live
Update. It shows "Order created" / "We received your order." with the four-phase order bar: a
promoted `Notification.ProgressStyle` on Android 16+, a `NotificationCompat` progress bar below.
Tapping it opens the app.

Live updates are available while notifications are allowed or were never asked (Android 13+) and
the `pushmart_live` channel is not blocked, as iOS `areActivitiesEnabled`. That gates both Track on
Lock Screen switches (the latest order card in Orders, the Status section of the order details),
the Keep your picks close row, the countdown copy of Upcoming drops and the Live copy in About.
Checkout starts tracking only when notifications are already allowed: there is no prompt at
checkout, and "Order placed!" uses the Lock Screen copy only when the notification went up. A
switch, Remind me or Keep your picks close used while never asked goes through the Push Primer and
the system dialog first; a declined prompt turns the switch back off and sets or pins nothing. The
tracked order (`pushmart.trackedOrder`) is read against the notifications on screen, so a
notification dismissed or ended elsewhere turns the switch off on the next visit.

The manifest registers PushMart's own style provider:

```xml
<meta-data
    android:name="com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER"
    android:value="com.pushwoosh.pushmart.live.OrderLiveUpdateStyleProvider" />
```

`OrderLiveUpdateStyleProvider` draws a server Live Update with the same order bar. It reads the
phase from the push **title** with the iOS widget's keyword rule (the title takes PushOn's
`deliveryStatus`) and ignores the payload's `segments` and `progress`. Example titles:
`ORDER CREATED` (Created), `PACKED` (Packed), `ON THE WAY` (On the way), `DELIVERED` (Delivered); an
unknown title or none gives Packed. The SDK draws the title and the body as sent.

A server Live Update `start` / `update` / `end` with `id: PUSHWOOSH_ORDER` replaces or dismisses
the app's order notification in place. This works on Android 16+ only: the SDK drops live-update
pushes below that. `PushwooshLiveUpdates.getActiveIds()` lists only the Live Updates the SDK
rendered, not the app's own notifications.

Orders > More for you, as iOS: Upcoming drops, then Keep your picks close.

Upcoming drops lists four drops. Remind me opens Remind me in: hours / minutes / seconds wheels
(default 5 minutes, shared with Reminders) and the time the reminder lands. It sets an inexact
`AlarmManager.setAndAllowWhileIdle` alarm (no exact-alarm permission, so in Doze it can come late)
for that time; the alarm posts "<name> drop" / "<category> · <tagline> goes live at <time>", a
countdown to the drop 10 minutes later, under the drop id (promoted on Android 16+, no
ProgressStyle; `NotificationCompat` below). The countdown times out when the drop goes live, and a
tap opens the app. A set reminder (`pushmart.drops`, `<id>|<start millis>`) shows "Reminder set for
<time>" with Cancel until Cancel or until the drop goes live; Cancel removes the alarm, the
notification and the entry. Android shows the countdown at every API level, so the screen always
uses the countdown copy while Live updates are available and the reminder copy otherwise.

Keep your picks close posts the first 3 wishlist products as separate ongoing notifications, tag
`picks-<id>` (the iOS activity id): the product name, "In your wishlist · <price>", the product
photo and a View item action. A tap or View item opens `pushwoosh://product/<id>`. They are plain
ongoing notifications, never promoted, and no group summary. While any of them is on screen the row
reads "Your picks are on your Lock Screen", and a tap removes them all. An empty wishlist shows
"Your wishlist is empty" and pins nothing.

## Your details

Profile > Your details, as iOS `UserView`: the profile photo, the email and the SMS and WhatsApp
numbers for order updates, with one Save. The avatar shows the photo, otherwise the email's first
letter, otherwise a person icon. Tapping it opens the system photo picker, or with a photo the
"Profile photo" choice of Choose from library / Remove photo. The photo is centre-cropped to a
320 px square JPEG on a background thread and stays on the device ("Photo updated", or "Couldn't use
this photo"). The subtitle is the stored email.

## Shopping preferences

Profile > Shopping preferences, as iOS `TagsView`: category tiles with the photo of each category's
first product (a coral check when followed, a plus otherwise), five size capsules, news topic chips
saved with Save topics, and the Email me offers too switch. On open `getTags` merges the device's
tags into the local picks: a `fav_<category>` tag of `true`, `"true"`, `"1"` or `1` turns the
category on, `false`, `"false"`, `"0"` or `0` turns it off, any other value or a missing tag leaves
it as it is; `size` applies only when it is one of XS–XL. On a network error the SDK answers with its
cached tags. Picks are stored under `pushmart.favCategories`, `pushmart.prefs.size`,
`pushmart.prefs.newsTopics` and `pushmart.prefs.emailNews`.

## App settings

Profile > App settings, as iOS `SettingsView`. Appearance is a System / Light / Dark segmented
control stored as `pushmart.theme`; picking one recreates the activity in the new mode and the
screen comes back. At launch `PushMartApp` sets `RichMediaManager.setRichMediaColorScheme(APP)`,
so rich media and native in-apps follow the app's theme rather than a scheme stored by an earlier
build.

Show alerts in-app decides whether a push arriving while PushMart is open shows as a notification
("Alerts on" / "Alerts off"). The Android SDK has no getter for it, so the app stores the switch as
`pushmart.showForegroundAlerts` (default on) and applies it again at every launch. The Android flag
is the inverse of iOS: `Pushwoosh.setShowPushnotificationAlert(true)` *suppresses* foreground
notifications, so the switch on calls `(false)` and off calls `(true)`.

## About PushMart

Profile > About PushMart, as iOS `MonetizationView`: a photo mosaic with the wordmark, "Shop what's
next.", How PushMart works (Price alerts, Deals tuned to you) and the About paragraph. While live
updates are available (see Live updates) the subtitle and a first Live order tracking row use the
iOS Live Activities copy. No SDK calls.

## Developer (debug builds only)

The Developer section of Profile is shown only when `BuildConfig.DEBUG`, as iOS wraps it in
`#if DEBUG`. It has two screens.

**Native in-apps** — nine buttons, one per native in-app layout the Android SDK renders: Modal,
Modal floating, Sheet, Sheet floating, Carousel, Stories, Banner, Fullscreen, Video. Each button
calls `PushwooshInAppUi.present(config)` over the screen with a config `InAppPresets` builds in
memory — no campaign, no Control Panel, no network. iOS opens a themed showroom page per template
first; Android presents straight away, and has no PiP, Scratch Card or Spin Wheel layouts. The
images are catalog and drop photos addressed as
`android.resource://com.pushwoosh.pushmart/drawable/<name>`; only the Video preset fetches its clip
over https, so it is the one button that needs a connection. Product buttons link to catalog
products as the iOS configs do (`pushwoosh://product/outwear_02`, `dress_04`, `jeans_01`), the
others to `pushwoosh://sale`.

The floating variants differ from their blocking siblings by `dimBackground: false` (Modal
floating) and by having no close button (Sheet floating, where swipe-down is the dismiss path).
The Modal preset is the only one carrying a `dark` variant, picked when the app is in dark mode.

Delegate callbacks are logged under the `PushMartInApp` tag — `shouldDisplay`, `willPresent`,
`didPresent`, `didClose`, `clickedAction`. This is how an e2e run tells "rendered" from "the
parser dropped the config":

```bash
adb logcat -s Pushwoosh | grep PushMartInApp
```

`InAppPresetsTest` feeds every preset through `InAppConfigParser` in both color schemes and checks
each local drawable resolves and each product link is a catalog product, so a broken preset fails
the unit tests rather than the demo.

**Developer tools**, as iOS `DeveloperView`:

- SDK log level: None / Error / Warn / Info / Debug / Verbose chips call `PWLog.updateLogLevel`
  ("Log level: <name>"). The SDK has no getter, so the screen remembers the pick for the process;
  it starts at Verbose because debug builds set `NOISE` in the manifest.
- Selected pair: the application code and base URL the SDK uses now, read on every open.
- Diagnostics: Reload in-app resources (`InAppManager.reloadInApps`, "In-app resources reloaded"
  or "Reload failed" with the error) and the SDK version.

Rich media delegate callbacks are logged under the `PushMartRichMedia` tag — `shouldPresent`,
`onPresent`, `onError`, `onClose`:

```bash
adb logcat -s Pushwoosh | grep PushMartRichMedia
```

## Launch arguments (e2e)

Debug builds only: on a cold start `MainActivity` hands the intent extras to `DebugLaunch`
(`src/debug`) before choosing the start destination. The release `DebugLaunch` is empty.

| Extra | Type | Effect |
|---|---|---|
| `pw_skip_onboarding` | boolean (`--ez`; the string `"true"` via `--es` also works) | When the app is not onboarded yet: marks onboarding done, applies the code and opens Home. Ignored once onboarded. |
| `pw_app_code` | string | Personal code for the skip, `XXXXX-XXXXX` (masked leniently, so `abcde12345` works). Invalid, absent or `5DF3B-09018` → guest. Does nothing without the skip flag. |

```bash
adb shell am start -n com.pushwoosh.pushmart/.MainActivity --ez pw_skip_onboarding true --es pw_app_code 5DF3B-09018
./tools/pushmart-install.bash skip-onboarding   # build, install and launch with the flag
adb shell pm clear com.pushwoosh.pushmart        # back to a fresh, not-onboarded install
```

The extras are read on a cold start only: force-stop the app (or reinstall it) before launching
with them, otherwise a launch against the already-running process silently ignores the flag.

Push registration is still a user action (Profile > Offers & order updates), the skip does not
register the device.

## Deep links and push routing

`MainActivity` handles the `pushwoosh://` scheme and PushOn's `pushon-add-product://` and
`pushon-open-tab://` links:

| Link | Opens |
|---|---|
| `pushwoosh://sale` | Sale promo screen |
| `pushwoosh://product/<id>` | Product detail (any catalog id, e.g. `outwear_01`, `dress_04`; see `catalog/Catalog.kt`) |
| `pushwoosh://product1`, `pushwoosh://product2` | `outwear_01`, `outwear_02` |
| `pushon-add-product://<id>` | Adds the product to the bag and opens the bag (an unknown id does nothing) |
| `pushon-open-tab://cart` | The bag |

A push with custom data (the `data` object of the Messaging API) is routed by
`PushMartNotificationServiceExtension`, in this priority:

| Custom data | Opens |
|---|---|
| `{"custom": {"voucher": {"discount": N}}}` | Voucher screen with `-N%` (PushOn) |
| `{"custom": {"specialOffer": {"product_id": "<id>"}}}` | Product detail (PushOn; an unknown id opens nothing) |
| `{"custom": {"items_in_basket": …}}` | The bag (PushOn, any value) |
| `{"voucher": "<code>"}` | Voucher screen with the code |
| `{"product_id": "<id>"}` | Product detail |
| `{"sale": true}` | Sale promo screen |

A push with a link in the `l` field is opened by the SDK through `ACTION_VIEW`, so these links
work from pushes too. Routes are ignored until onboarding is complete.
An open sheet, Sale or Voucher closes before the route's screen opens. Opening the bag from a
route posts no `BasketOpen`.

Try it from a shell:

```bash
adb shell am start -a android.intent.action.VIEW -d "pushwoosh://product/dress_04"
adb shell am start -a android.intent.action.VIEW -d "pushon-add-product://dress_04"
adb shell am start -a android.intent.action.VIEW -d "pushon-open-tab://cart"
./tools/send-push.bash -t "Drop" -m "Plait Halter Sundress is back" -l "pushwoosh://product/dress_04"
```

`tools/send-push.bash` calls the Messaging API v2 `notify` endpoint without a device filter, so
it broadcasts to every device registered under app `5DF3B-09018`, not just yours. To target only
your own device, call the v1 `createMessage` endpoint with `"devices": ["<push token>"]`. A debug
build logs the token: run `adb logcat -s Pushwoosh` and look for the
`Registered for push notifications` line.

```bash
curl -s -X POST https://api.pushwoosh.com/json/1.3/createMessage \
  -H "Content-Type: application/json" \
  -d '{
    "request": {
      "application": "5DF3B-09018",
      "auth": "<API token from tools/send-push.bash>",
      "notifications": [{
        "send_date": "now",
        "content": "You have a voucher",
        "data": {"voucher": "SAVE20"},
        "devices": ["<push token>"]
      }]
    }
  }'
```

To test cold-start push routing, kill the app instead of force-stopping it — a force-stopped app
is put in Android's "stopped" state, and FCM will not attempt delivery to it:

```bash
adb shell am kill com.pushwoosh.pushmart
```

## Edge-to-edge

`MainActivity` calls `enableEdgeToEdge()` and applies one insets rule in its root listener: the
content pads only what nobody below it takes.

- Top: the status bar height, except on full-bleed screens (`FULL_BLEED_DESTINATIONS`: Sale and
  Voucher), which pad themselves.
- Bottom on tab screens (`TAB_DESTINATIONS`): nothing. `BottomNavigationView` pads itself by the
  navigation bar inset, so its background reaches the screen edge.
- Bottom on every other screen: the navigation bar height.
- Keyboard: on every screen the bottom padding becomes the keyboard height (`adjustResize` stops
  resizing the window once edge-to-edge is on), and the children receive the insets minus what the
  content took, so the tab bar sits on the keyboard without an inset band inside.

The rule reads the current destination, so the destination listener re-requests insets on every
navigation. Fragments do not handle insets, except the promo screens (`SaleFragment`,
`VoucherFragment`, both on `FullBleedFragment`): they pad their own root by the system bars, keep
the gradient under them, switch the status bar icons to light and drop the 3-button navigation
contrast scrim while shown. Bottom sheets are separate windows and are
handled by Material 3 (`enableEdgeToEdge` in `ThemeOverlay.Material3.BottomSheetDialog`).

## Test device link

The SDK ships `com.pushwoosh.DeepLinkActivity`, which registers a test device when it receives
`pushwoosh-<appcode>://createTestDevice`. PushMart declares it only in the debug manifest
(`src/debug/AndroidManifest.xml`) and only for the guest project (`pushwoosh-5DF3B-09018` and
`pushwoosh-5df3b-09018`), because the scheme is static and cannot follow a personal code applied
at runtime. Android intent-filters cannot be built at runtime, so using this link with another app
code means editing the debug manifest by hand. Release builds do not have the link.

```bash
adb shell am start -a android.intent.action.VIEW -d "pushwoosh-5DF3B-09018://createTestDevice"
```

Calling it a second time for a device that is already registered as a test device returns HTTP
210, and the app shows a "Test device registration has failed" toast.

## Known backend quirks

One SDK call surfaced backend validation that is worth knowing before filing a bug from a toast:

- `registerEmail` rejects addresses on the `@example.com` domain.

## Firebase configuration (temporary workaround)

`google-services.json` is a copy of `demoapp/google-services.json` with `package_name` replaced
by `com.pushwoosh.pushmart`. There is no real Firebase Android app registered under this package
today, yet a real FCM token is issued with this file as of this writing. Treat this as a
stopgap, not a guarantee: replace the file once a proper Firebase Android app exists for
`com.pushwoosh.pushmart`.
