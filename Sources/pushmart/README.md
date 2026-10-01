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
`:pushwoosh-inapp-ui`); no Maven
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

The manifest sets `com.pushwoosh.appid` to the demo project `5DF3B-09018` (the one `demoapp` and
`tools/send-push.bash` use), so the SDK is usable from the first process start. On the first launch
the onboarding still asks for an access code in the `XXXXX-XXXXX` format and applies it with
`Pushwoosh.getInstance().setAppId(code)`, which overrides the manifest default; the SDK persists
the code, so a cold start does not need to apply it again. The "Use the Pushwoosh demo project"
link fills in `5DF3B-09018`.

Profile > Switch account unregisters the device, applies the new code and registers again.
Profile > Sign out unregisters the device and returns to onboarding.

## What each screen sends

| Screen / action | SDK call |
|---|---|
| Onboarding, email entered | `setEmail(email)`, `setUserId(email)` |
| Product detail opened | `postEvent("product-opened", {productId})` |
| Add to bag | `postEvent("add-to-cart", {productId, price (int), option})`, `PushwooshBadge.setBadgeNumber(count)` |
| Wishlist heart | list tag `wishlist` append/remove, `postEvent("wishlist_add", {productId, price})` on add |
| Price alert switch | list tag `price_alerts` append/remove, `postEvent("price_alert_on", {productId, price})` on enable |
| Checkout | `sendInappPurchase(sku, unitPrice, "USD")` once per unit, `orders_placed` increment, `purchased_products` append, `postEvent("checkout", {items, total})` |
| Shopping preferences chip | `setTags({fav_<category>: true/false})` |
| Push notifications switch | `registerForPushNotifications()` / `unregisterForPushNotifications()` |
| Profile > Your details > Save changes | `setUserId(memberId)` when MEMBER ID is non-empty, `setEmail(email)` when EMAIL is non-empty — both sent on every save even if unchanged. This is the e2e entry point for "register user by id" and "inbox follows identity" (base 5, base 8). MEMBER ID is prefilled with the current SDK user id (real text, not a hint), so an automated run must clear it before typing (select all + delete), exactly like demoapp's USER field |
| Offers tab (header counter) | `PushwooshInbox.registerMessagesCountObserver`, `registerUnreadMessagesCountObserver`, one-shot `messagesCount` / `unreadMessagesCount` on resume |
| Offers tab (list) | library `InboxFragment` from `pushwoosh-inbox-ui`: `loadMessages` on open / pull-to-refresh, tap = `performAction` (read), swipe = `deleteMessage` |
| Offers > ··· > Mark all as read | `PushwooshInbox.loadMessages`, then `readMessages(<unread codes>)` |
| Offers > ··· > Clear read | `PushwooshInbox.loadMessages`, then `deleteMessages(<read codes>)` |
| Offers > ··· > Delete all | `PushwooshInbox.loadMessages`, then `deleteMessages(<all codes>)` |
| Profile > Playground > Post event | `postEvent(name, {key: value})`, or an empty `TagsBundle` when ATTRIBUTE KEY is blank |
| Profile > Playground > Set tag | `setTags({key: value})` (string tag) |
| Profile > In-app showcase > any layout button | `PushwooshInAppUi.present(config)` with that preset's local config (`inAppId` is `pushmart-<type>`, e.g. `pushmart-modal`, `pushmart-sheet-floating`). Nothing leaves the device |
| Profile > Rich media > Modal display mode | `RichMediaManager.setRichMediaType(MODAL)` / `setRichMediaType(DEFAULT)`; the current value is read with `getRichMediaType()` |
| Profile > Rich media > Modal style | `RichMediaManager.getDefaultRichMediaConfig()`, edited and stored back with `setDefaultRichMediaConfig(config)`; the color scheme dropdown uses `getRichMediaColorScheme()` / `setRichMediaColorScheme(scheme)` |
| Profile > Rich media > Server triggers | `postEvent("showRichMedia")`, `postEvent("showRichMediaClient")`, `postEvent("showRichMediaServer")` (via `InAppManager`, with the callback) |

`price` is sent as a number (dollars). `sendInappPurchase` has no quantity parameter, so a
purchase of two units of the same product produces two separate `PW_InAppPurchase` calls at the
unit price, not one call with `quantity: 2`.

The launcher badge is local only: `PushwooshBadge.setBadgeNumber` drives the launcher icon
through ShortcutBadger and never sends a `badge` tag to the server. ShortcutBadger only keeps the
badge in sync on launchers it has a supported receiver for (Samsung, Huawei, Xiaomi, etc.); on
the stock Pixel launcher the call is a silent no-op — no number, no dot. Any dot seen on a Pixel
comes from Android's own notification dots for unread notifications, unrelated to this call.

The push token row on Profile > Notifications is driven by the app's own `pushNotificationEnabled`
preference, not by asking the SDK: the screen shows `Not registered` whenever the toggle is off,
regardless of what the SDK still has cached. `unregisterForPushNotifications` does clear the
SDK's own persisted token (`RegistrationPrefs.clearPushRegistrationInfo`); what survives is
Firebase's own token cache, which is why re-registering afterwards yields the same FCM token
instead of a new one.

Requests and responses are visible in logcat with `com.pushwoosh.log_level = NOISE` (the inbox `[InboxRepository] updateEvent` lines the e2e scenarios assert on are `PWLog.noise`, invisible at `DEBUG`):

```bash
adb logcat -s Pushwoosh
```

## Offers (Inbox)

The Offers tab hosts the library `InboxFragment` from `pushwoosh-inbox-ui`
(`PushwooshInboxUi.createInboxFragment()`), nested in the app's own screen so the header stays
PushMart's. `InboxStyling` maps the theme colors onto `PushwooshInboxStyle` on every screen create
(the style object is global and mutable).

The header subtitle is the counter e2e scenarios read, rendered in caps:
`NO MESSAGES YET`, `3 MESSAGES · ALL READ`, `3 MESSAGES · 1 UNREAD`. It follows the SDK count
observers, so it updates when a push lands in the inbox, when a row is read or swiped away and
after a user switch. Pluralisation is fixed to "messages" on purpose — the scenarios grep for it.

The `···` button opens the actions sheet: "Mark all as read", "Clear read", "Delete all". Each
loads the current list, applies the change through `PushwooshInbox`, and reports via the result
banner; "Nothing to do" means the selection was empty. The list refreshes itself: the SDK posts
`InboxMessagesUpdatedEvent` after `readMessages`/`deleteMessages` and the library presenter
applies it. Use "Delete all" to leave the inbox clean between e2e runs.

On-device finding: "Mark all as read" calls `PushwooshInbox.readMessages`, which sets the SDK
status `READ` — the header counter drops to "all read" and `InboxMessage.isRead()` becomes true,
but the `pushwoosh-inbox-ui` rows keep their unread tint. The library view holders key the tint
on `isActionPerformed()`, which only becomes true at status `OPEN`, i.e. when a row is tapped
(`performAction`) — `markMessageOpened` would flip it but also sends a click statistic for every
message, so PushMart does not use it. The inconsistency lives in `pushwoosh-inbox-ui`, not in the
app; a tap on a row still renders it as read.

Every push sent by `tools/send-push.bash` lands in the inbox for 14 days. The `[InboxRepository]
updateEvent InboxEvent: ...` lines are `PWLog.noise` and only print while the Offers tab is open.
Switching the user on Profile > Your details forces a full inbox resync (`InboxRepository` listens
to `UserIdUpdatedEvent`); nothing in the app triggers it.

## Playground

Profile > DEVELOPER > Playground. Two cards:

- **Post event** — EVENT NAME plus an optional ATTRIBUTE KEY / ATTRIBUTE VALUE pair (sent as a
  string attribute; a blank key sends an empty `TagsBundle`). The SDK logs
  `Posted event <name>, attributes: ...`.
- **Set tag** — TAG KEY / TAG VALUE, sent as a string tag with `setTags`.

Fields are empty on open (no prefilled text), and the event name is required.

## In-app showcase

Profile > DEVELOPER > In-app showcase. Nine buttons, one per native in-app layout the Android SDK
renders: Modal, Modal floating, Sheet, Sheet floating, Carousel, Stories, Banner, Fullscreen,
Video. Each button calls `PushwooshInAppUi.present(config)` with a config `InAppPresets` builds in
memory — no campaign, no Control Panel, no network. The images are module drawables addressed as
`android.resource://com.pushwoosh.pushmart/drawable/<name>`; only the Video preset fetches its clip
over https, so it is the one button that needs a connection.

The floating variants differ from their blocking siblings by `dimBackground: false` (Modal
floating) and by having no close button (Sheet floating, where swipe-down is the dismiss path).
The color scheme set on the Rich media screen applies here as well: the Modal preset is the only
one carrying a `dark` variant.

Delegate callbacks are logged under the `PushMartInApp` tag — `shouldDisplay`, `willPresent`,
`didPresent`, `didClose`, `clickedAction`. This is how an e2e run tells "rendered" from "the
parser dropped the config":

```bash
adb logcat -s Pushwoosh | grep PushMartInApp
```

`InAppPresetsTest` feeds every preset through `InAppConfigParser` in both color schemes and checks
each local drawable resolves, so a broken preset fails the unit tests rather than the demo.

## Rich media

Profile > DEVELOPER > Rich media. HTML rich media coming from campaigns, in three cards.

**Modal display mode** — `RichMediaManager.setRichMediaType`. Off is `DEFAULT`, the legacy
full-screen activity over the app; on is `MODAL`, a window over the current screen. The setting
applies to the next rich media and survives restarts. Native in-app layouts are a separate
pipeline and ignore it.

**Modal style** — position, present and dismiss animation, window width, swipe-to-dismiss
gestures and animation duration, all written into the config from
`RichMediaManager.getDefaultRichMediaConfig()` and stored back with `setDefaultRichMediaConfig`.
That setter overwrites every field, so each control edits the stored config instead of building a
fresh one. The color scheme dropdown goes through `setRichMediaColorScheme` and is the one control
here that also affects native in-app.

**Server triggers** — three buttons posting `showRichMedia`, `showRichMediaClient` and
`showRichMediaServer`. Content only appears if the connected Control Panel project has a campaign
for that event; the demo project `5DF3B-09018` has none, so the honest answer there is "No content
returned". The `postEvent` callback only says the request went through, so "Content shown" is
reported from the rich media delegate and never from the callback. The result banner fades after a
couple of seconds; the line under the buttons keeps the last outcome:

| Outcome | Means |
|---|---|
| `Sending <event>…` | the button was tapped, the request is in flight |
| `Event sent — Waiting for content…` | the SDK accepted the event, a campaign may still answer |
| `Event failed — <error>` | `postEvent` itself failed (no network, bad app code) |
| `No content returned — <event> was sent but nothing came back. Configure a campaign for this event in Control Panel.` | the event went through and no campaign answered within 5 s |
| `No response — <event> got no answer in 5 s. Check the app code and network.` | the SDK never called back at all |
| `Content shown — <code>` | the rich media delegate reported a presented page |
| `Content failed — <error>` | the delegate reported an error for the page |

Delegate callbacks are logged under the `PushMartRichMedia` tag — `shouldPresent`, `onPresent`,
`onError`, `onClose`:

```bash
adb logcat -s Pushwoosh | grep PushMartRichMedia
```

In `DEFAULT` mode the page opens in its own activity, so `onPresent` arrives while the Rich media
screen is stopped. The screen therefore listens through a plain main-thread slot
(`RichMediaSignals`), not a lifecycle-gated observer: a signal reaches whoever is listening right
now, and a signal nobody listens for is dropped rather than replayed on the next visit.

## Launch arguments (e2e)

`MainActivity` reads two intent extras on a cold start, before choosing the start destination:

| Extra | Type | Effect |
|---|---|---|
| `pw_skip_onboarding` | boolean (`--ez`; the string `"true"` via `--es` also works) | When the app is not onboarded yet: connects with the app code, continues as guest and opens Home. Ignored once onboarded — it never re-connects. |
| `pw_app_code` | string | App code for the skip, `XXXXX-XXXXX` (masked leniently, so `5df3b09018` works). Invalid or absent → `5DF3B-09018`. Does nothing without the skip flag. |

```bash
adb shell am start -n com.pushwoosh.pushmart/.MainActivity --ez pw_skip_onboarding true --es pw_app_code 5DF3B-09018
./tools/pushmart-install.bash skip-onboarding   # build, install and launch with the flag
adb shell pm clear com.pushwoosh.pushmart        # back to a fresh, not-onboarded install
```

The extras are read on a cold start only: force-stop the app (or reinstall it) before launching
with them, otherwise a launch against the already-running process silently ignores the flag.

The extras are not gated by the build type: they are invisible to a demo audience and the
release build type shares the debug signing anyway. Push registration is still a user action
(Profile > Offers & order updates), the skip does not register the device.

## Deep links and push routing

`MainActivity` handles the `pushwoosh://` scheme:

| Link | Opens |
|---|---|
| `pushwoosh://sale` | Sale promo screen |
| `pushwoosh://product/<id>` | Product detail (`aeroknit`, `pulsebuds`, `chrono`, `voyager`, `beacon`, `glide`, `orbit`, `lumen`, `trail`, `solo`) |
| `pushwoosh://product1`, `pushwoosh://product2` | `aeroknit`, `chrono` |

A push with custom data (the `data` object of the Messaging API) is routed by
`PushMartNotificationServiceExtension`, in this priority:

| Custom data | Opens |
|---|---|
| `{"voucher": "<code>"}` | Voucher screen with the code |
| `{"product_id": "<id>"}` | Product detail |
| `{"sale": true}` | Sale promo screen |

A push with a link in the `l` field is opened by the SDK through `ACTION_VIEW`, so
`pushwoosh://...` links work from pushes too. Routes are ignored until onboarding is complete.

Try it from a shell:

```bash
adb shell am start -a android.intent.action.VIEW -d "pushwoosh://product/chrono"
./tools/send-push.bash -t "Drop" -m "Chrono S3 is back" -l "pushwoosh://product/chrono"
```

`tools/send-push.bash` calls the Messaging API v2 `notify` endpoint without a device filter, so
it broadcasts to every device registered under app `5DF3B-09018`, not just yours. To target only
your own device, call the v1 `createMessage` endpoint with `"devices": ["<push token>"]`, using
the token shown on Profile > Notifications while the push toggle is on:

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
`pushwoosh-<appcode>://createTestDevice`. PushMart declares it in the manifest for the demo
project only (`pushwoosh-5DF3B-09018` and `pushwoosh-5df3b-09018`), because the scheme is
static and cannot follow the app code entered at runtime. Android intent-filters cannot be
built at runtime, so using this link with another app code means editing the manifest by hand.

```bash
adb shell am start -a android.intent.action.VIEW -d "pushwoosh-5DF3B-09018://createTestDevice"
```

Calling it a second time for a device that is already registered as a test device returns HTTP
210, and the app shows a "Test device registration has failed" toast.

## Known backend quirks

A couple of SDK calls surfaced backend validation that is worth knowing before filing a bug from
a toast:

- `setUserId` with an email-shaped value (e.g. entering an email address on Your details > Member
  ID) returns 210 and the app shows a fail toast.
- `registerEmail` rejects addresses on the `@example.com` domain.

## Firebase configuration (temporary workaround)

`google-services.json` is a copy of `demoapp/google-services.json` with `package_name` replaced
by `com.pushwoosh.pushmart`. There is no real Firebase Android app registered under this package
today, yet a real FCM token is issued with this file as of this writing. Treat this as a
stopgap, not a guarantee: replace the file once a proper Firebase Android app exists for
`com.pushwoosh.pushmart`.
