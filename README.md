# Arrival Reminders

A small native Android app (Kotlin + Jetpack Compose). Keep a to-do list and attach items to a
place. When you arrive at that place, you get a notification listing those items.

Two kinds of places:

- **Wi-Fi network**: triggers when you connect to a saved network (e.g. your home Wi-Fi). Cheapest on battery.
- **Location**: triggers when you enter a circle (100 to 1000 m radius) around a saved spot, using Android geofencing. Pick the spot by searching an address or using your current location.

Everything is stored on the phone in a JSON file. There is no server and no account.

## Using the app

- **To-do tab**: reminders grouped by place. Tap **Add item**, type the task, pick a place, and optionally turn on **Repeat every time**. Tap an item to edit or delete it.
- **Places tab**: tap **Add place** to save a Wi-Fi network or a location. Tap a place to edit or delete it.
- **Repeat off** (default): the reminder comes back on every arrival until you tick it off.
- **Repeat on**: ticking it off only covers this visit. It returns on your next arrival.
- **Notification button**: **Done** (or **Mark all done**) ticks off everything listed for that place without opening the app.
- The app follows your phone's light/dark setting.

## Building without Android Studio (GitHub Actions)

Pushing to the `main` branch of a GitHub repo runs `.github/workflows/build-apk.yml`, which builds
the app and publishes `app-debug.apk` as a release named "Build N".

1. On your phone, open the repo's **Releases** page in the browser and download `app-debug.apk` from the newest build.
2. Open the downloaded file and install it. The first time, Android asks you to allow installs from your browser.
3. Later builds install over the old one and keep your data, because every build uses the same signing key (`app/debug.keystore`).

Keep the repo **private**: the signing key is committed to it. It's a debug key for personal use, not a Play Store key.

## Building with Android Studio

1. Install [Android Studio](https://developer.android.com/studio).
2. Open this folder as a project. Let Gradle sync; it downloads Gradle 8.9 and the Android SDK 35 components it needs.
3. Plug in your phone (USB debugging on) and press Run. Needs Android 10 (API 29) or newer with Google Play Services.

## First-run setup on the phone

1. Open the app. A card at the top lists what's missing. Tap **Grant permissions** and allow location and notifications.
2. Android won't offer **Allow all the time** in that pop-up. Tap **Open app settings**, then Permissions > Location > **Allow all the time** (and turn on **Use precise location**).
3. In the same settings page, set Battery to **Unrestricted** so Android doesn't suppress background triggers.
4. Add a place on the **Places** tab, then add items on the **To-do** tab.

## Testing

- Wi-Fi: disconnect from the network and reconnect. A notification should appear within a few seconds.
- Location: leave the circle, then re-enter it. The first geofence event can take a minute or two. Saving a place while you're already inside it does not trigger a notification.
- After a notification, the same place won't notify again for 10 minutes (prevents repeats from Wi-Fi flapping or edge-of-circle jitter). Change `COOLDOWN_MS` in `Notifier.kt` if you want a different gap.

## How it works

| File | Role |
| --- | --- |
| `Models.kt` | `Place` and `TodoItem` data classes |
| `Storage.kt` | Reads and writes the JSON file |
| `GeofenceManager.kt` | Registers geofences with Play Services |
| `WifiMonitor.kt` | Registers a Wi-Fi network callback and reads the current SSID |
| `Receivers.kt` | Handles geofence, Wi-Fi, boot, and notification "Done" broadcasts |
| `Notifier.kt` | Builds and posts the arrival notification |
| `Monitoring.kt` | Permission checks and "re-register everything" helper |
| `MainActivity.kt` | App shell: navigation, permission card, add/edit dialogs wiring |
| `ItemsScreen.kt` | Grouped to-do list and the add/edit item dialog |
| `PlacesScreen.kt` | Places list, add/edit place dialog, address search |
| `Theme.kt` | Light and dark colours |

## Known limits

- Reading the current Wi-Fi name requires location permission and Location turned on in system settings (an Android rule).
- Address search uses the phone's built-in geocoder, which needs a network connection and Google services. If it's unavailable, use your current location instead.
- There is no map pin-drop yet; that would need a Google Maps API key.
- Geofences are approximate, and Android can delay them, especially in power-saving modes.
- Triggers are re-registered at boot, after an app update, and whenever you open the app.
- There is no "leaving" trigger; reminders fire on arrival only.
