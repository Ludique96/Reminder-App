# Arrival Reminders

A small native Android app (Kotlin + Jetpack Compose). Keep a to-do list and attach items to a
place. When you arrive at that place, you get a notification listing those items.

Two kinds of places:

- **Wi-Fi network**: triggers when you connect to a saved network (e.g. your home Wi-Fi). Cheapest on battery.
- **Location**: triggers when you enter a circle (100 to 1000 m radius) around a saved spot, using Android geofencing.

Everything is stored on the phone in a JSON file. There is no server and no account.

## Build and run

1. Install [Android Studio](https://developer.android.com/studio).
2. Open this folder (`ArrivalReminders`) as a project. Let Gradle sync; it downloads Gradle 8.9 and the Android SDK 35 components it needs.
3. Plug in your phone (USB debugging on) and press Run. Needs Android 10 (API 29) or newer with Google Play Services.

## Building without Android Studio (GitHub Actions)

Pushing to the `main` branch of a GitHub repo runs `.github/workflows/build-apk.yml`, which builds
the app and publishes `app-debug.apk` as a release named "Build N".

1. On your phone, open the repo's **Releases** page in the browser and download `app-debug.apk` from the newest build.
2. Open the downloaded file and install it. The first time, Android asks you to allow installs from your browser.
3. Later builds install over the old one and keep your data, because every build uses the same signing key (`app/debug.keystore`).

Keep the repo **private**: the signing key is committed to it. It's a debug key for personal use, not a Play Store key.

## First-run setup on the phone

1. Open the app and tap **Grant permissions**. Allow location, then choose **Allow all the time** when asked (Android asks in two steps). Allow notifications.
2. In phone Settings > Apps > Arrival Reminders > Battery, choose **Unrestricted** so Android doesn't suppress background triggers.
3. **Places** tab: name a place, then either tap **Use current** to fill in the Wi-Fi name and save it, or stand at the spot and tap **Save my current location**.
4. **To-do** tab: type an item, pick the place under "When I arrive at", tap **Add**.

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
| `Receivers.kt` | Handles geofence, Wi-Fi and boot broadcasts |
| `Notifier.kt` | Builds and posts the arrival notification |
| `Monitoring.kt` | Permission checks and "re-register everything" helper |
| `MainActivity.kt` | The Compose UI and the permission flow |

## Known limits

- Reading the current Wi-Fi name requires location permission and Location turned on in system settings (an Android rule).
- Geofences are approximate, and Android can delay them, especially in power-saving modes.
- Triggers are re-registered at boot, after an app update, and whenever you open the app.
- Not yet built or run on a device. Expect to fix a small compile issue or two on first sync.
