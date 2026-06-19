# Module pushwoosh-liveupdates

Adds Live Update support to the Pushwoosh SDK. A Live Update is an ongoing, progress-style
notification (Android 16 / API 36+) that the SDK posts, refreshes, and dismisses automatically from
server-sent push messages — think order tracking, delivery progress, or a live sports score. The
Pushwoosh backend is the source of truth and drives every start / update / end transition; there is
no app-side API to create one. Add this dependency when your campaigns send Live Update pushes and
you want them rendered as native progress notifications.

Only two things are under app control; the SDK handles the rest:
- **Manage what is on screen** via [PushwooshLiveUpdates] — dismiss a specific Live Update locally,
  before the server's terminal `end` push, or query which ones are currently shown.
- **Customize the progress bar** by implementing [LiveUpdateProgressStyleProvider]. Everything else
  (channel registration, the ongoing flag, large-icon download, action mapping, header time) stays
  SDK-owned.

**Availability:** Live Updates require Android 16 (API 36) or newer. On older devices live-update
pushes are suppressed — they are not shown at all (neither as a Live Update nor as a regular
notification) — and every [PushwooshLiveUpdates] method is a safe no-op.

## Sending a Live Update from the backend

A Live Update is created and driven entirely from the server through the Pushwoosh Messaging API —
the app cannot start or update one. Every push carries an `op` (`OPERATION_START` /
`OPERATION_UPDATE` / `OPERATION_END`) and a stable `id`; reusing the same `id` refreshes the
notification in place, and `OPERATION_END` dismisses it. These fields travel inside a structured
`live_update` object under the `android` platform block, alongside `title` and `body`.

Start one — `POST https://api.pushwoosh.com/messaging/v2/notify` with header
`Authorization: Token YOUR-API-TOKEN`:

```json
{
  "segment": {
    "application": "YOUR-APP-CODE",
    "platforms": ["ANDROID"],
    "expression": "A(\"YOUR-APP-CODE\")",
    "schedule": { "at": "2026-06-02T12:00:00Z" },
    "payload": { "content": { "localized_content": { "default": { "android": {
      "title": "Pizza Margherita — order #4521",
      "body": "Courier on the way • ETA 12 min",
      "live_update": {
        "op": "OPERATION_START",
        "id": "pizza_4521",
        "progress": 4,
        "segments": [{"color": "#705289", "length": 8}]
      }
    }}}}}
  }
}
```

To advance it, resend with `"op": "OPERATION_UPDATE"`, the same `id` and a new `progress`; to
dismiss it, send `"op": "OPERATION_END"` with that `id`.

### Live Update fields (`android.live_update`)

| Field | Meaning |
|---|---|
| `op` | Lifecycle operation: `OPERATION_START`, `OPERATION_UPDATE`, or `OPERATION_END` (required) |
| `id` | Stable activity id; reuse across pushes to update in place (required) |
| `progress` | Progress value, measured against the sum of segment lengths |
| `progress_indeterminate` | `true` for an animated, value-less bar |
| `progress_bar` | `false` to hide the progress bar entirely; the notification still posts ongoing and promoted (default `true`) |
| `segments` | JSON array of `{"color":"#RRGGBB","length":N}` progress segments |
| `extras` | Arbitrary JSON object, surfaced to your provider via `LiveUpdateState.getExtras()` |
| `when` | Header time anchor, epoch ms |
| `chronometer` | `true` to tick the header time as a live counter from `when` |
| `chronometer_count_down` | with chronometer: `true` counts down instead of up |
| `show_when` | `false` to hide the header time column |

Title, body and large icon use the standard Pushwoosh push fields (`android.title`,
`android.body`, `ci`) — they are not Live-Update-specific. Action buttons are covered below.

### Action buttons (`pw_actions`)

Live Updates render action buttons from the same `pw_actions` field as regular Pushwoosh pushes —
the SDK reuses core's parser, so the format is identical and not Live-Update-specific. The value is
a JSON array (sent as a string inside `android.root_params`); each element describes one button:

| Field | Required | Meaning |
|---|---|---|
| `type` | yes | Button kind: `ACTIVITY`, `BROADCAST`, or `SERVICE` — which Android component the tap targets |
| `title` | yes | Button label; a button without a title is skipped |
| `url` | no | URI handed to the component; for an `ACTIVITY` with no explicit `action` the SDK opens it via `ACTION_VIEW` |
| `action` | no | Explicit intent action string |
| `class` | no | Fully-qualified component class name to start directly |
| `extras` | no | JSON object whose values are forwarded to the component as string intent extras |

The three `type` values map to the matching `PendingIntent`:

- **`ACTIVITY`** — launches an Activity, e.g. a deep link or an in-app screen. With only a `url` and
  no `action`, the SDK opens it as `ACTION_VIEW`.
- **`BROADCAST`** — sends a broadcast to your `BroadcastReceiver`; use it to handle the tap in the
  background without bringing UI forward.
- **`SERVICE`** — starts a Service.

Example — a `start` push with two buttons (`pw_actions` sits in `root_params`, alongside the
`live_update` object):

```json
"android": {
  "live_update": { "op": "OPERATION_START", "id": "pizza_4521", "progress": 4 },
  "root_params": {
    "pw_actions": "[{\"type\":\"ACTIVITY\",\"title\":\"Track order\",\"url\":\"myapp://orders/4521\"},{\"type\":\"BROADCAST\",\"title\":\"Cancel\",\"extras\":{\"orderId\":\"4521\"}}]"
  }
}
```

Each render rebuilds the notification from the current push, so an `update` keeps its buttons only
if it resends `pw_actions` — omit the field and the buttons disappear on the next update.

# Package com.pushwoosh.liveupdates

The primary entry point is [PushwooshLiveUpdates]. Use `endLiveUpdate` to dismiss one Live Update by
its `activityId` when the app knows the activity has finished before the server's `end` push,
`endAllLiveUpdates` to clear everything currently shown (for example on logout), and `getActiveIds`
to reconcile app state with what is on screen. All methods are safe to call from any thread.

To customize how a Live Update looks, implement [LiveUpdateProgressStyleProvider] and register it in
`AndroidManifest.xml` via the `com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER` meta-data key. Its single
`createStyle` method is invoked once per `start` / `update` render, on a worker thread, and must be
stateless — derive the returned `Notification.ProgressStyle` only from the supplied state. If it
throws, the SDK falls back to the default style and the notification still posts.

[LiveUpdateState] is the immutable snapshot handed to `createStyle`: it carries the Live Update's
identity and lifecycle ([LiveUpdateOperation]), its title and subtitle, progress value with its
[LiveUpdateSegment] breakdown, large-icon URL, notification actions, and an arbitrary JSON `extras`
object — the primary channel for custom business data. [LiveUpdateOperation] distinguishes the
`START`, `UPDATE`, and `END` phases of the lifecycle. [LiveUpdateSegment] describes one colored
phase of a multi-segment progress bar.
