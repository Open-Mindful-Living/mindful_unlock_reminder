# Unlock Reminder

A minimal Android app that shows a motivational reminder notification when you unlock your phone.

## Features (MVP v1)

- Configure a personal motivational message
- Toggle unlock reminders on/off
- Choose reminder frequency: every unlock, once every 30 minutes, or once per day
- Notification-first approach — no overlays, no accessibility permissions
- Preview your message in-app before enabling

## Requirements

- Android 8.0+ (API 26+)
- Notification permission (required on Android 13+)

## How to Build

1. Open the project in Android Studio Hedgehog or later.
2. Let Gradle sync finish.
3. Build → Make Project (`Ctrl+F9` / `Cmd+F9`).
4. Run on a physical device or emulator (API 26+).

## Manual Testing Guide

1. **Install the app** on a device or emulator.
2. **Open the app** — the main settings screen appears.
3. **Grant notification permission** when the system dialog appears (Android 13+ only).
4. **Enable unlock reminder** using the toggle switch.
5. **Set a custom motivational message** in the text field.
6. Choose a **reminder frequency** from the dropdown.
7. Tap **Save** to persist your settings.
8. Tap **Preview message** to verify the dialog appears with your message.
9. **Lock the phone** (power button or `adb shell input keyevent 26`).
10. **Unlock the phone** (swipe up / PIN / fingerprint).
11. **Verify a notification appears** in the notification tray with the title "A mindful reminder" and your configured message.
12. **Tap the notification** — the motivational card screen should open showing your message.
13. Tap **Done** to dismiss, or **Edit message** to return to the settings screen.

### Testing with ADB

```bash
# Lock the screen
adb shell input keyevent 26

# Simulate USER_PRESENT broadcast (unlock)
adb shell am broadcast -a android.intent.action.USER_PRESENT
```

> **Note:** The ADB broadcast method only works if the app is currently running or recently backgrounded. On some OEM devices you may need to lock/unlock physically for reliable delivery.

## Why a Persistent Notification?

On Android 14+ (verified on Pixel), the operating system uses the **Cached App Freezer** to suspend app processes in the background. When a process is frozen, its manifest-registered `BroadcastReceiver`s stop receiving system broadcasts — including `ACTION_USER_PRESENT` — even though other apps receive the same broadcast at the same moment.

Running a **foreground service** keeps the app's process resident and prevents the OS from freezing it. The service itself **dynamically registers** an `ACTION_USER_PRESENT` receiver in `onCreate()`, so unlock events are reliably delivered directly to the live service rather than relying on a manifest-registered receiver inside a potentially frozen process.

`UnlockReceiver` (declared in the manifest) is now **BOOT_COMPLETED only** — its sole job is to restart `UnlockMonitorService` after a device reboot so the dynamic receiver is re-registered without requiring the user to open the app.

The persistent notification is posted on a **silent, badge-free channel** (`IMPORTANCE_MIN`) so it sits quietly in the "Silent" section of the notification shade and does not play a sound, vibrate, or show a badge. You can disable or hide it further via long-press → notification settings on that specific notification without affecting reminder delivery (though on some OEMs, stopping the foreground service may re-enable the freezer).



```
app/src/main/java/com/mindful/unlock/reminder/
├── data/
│   ├── UserPreferences.kt              # Data model (DataStore-backed settings)
│   └── UserPreferencesRepository.kt    # DataStore read/write operations
├── notification/
│   └── UnlockReminderNotificationManager.kt  # Channel creation & notification posting
├── receiver/
│   └── UnlockReceiver.kt              # BOOT_COMPLETED only — restarts the monitor service after reboot
├── service/
│   └── UnlockMonitorService.kt        # Keeps process alive AND dynamically listens for ACTION_USER_PRESENT
├── ui/
│   ├── MainActivity.kt                # Entry point, permission handling
│   ├── MainScreen.kt                  # Compose settings screen
│   └── MessageDialogActivity.kt       # Motivational card screen (opened from notification)
└── util/
    ├── ReminderFrequency.kt           # Enum: every_unlock / 30min / daily
    └── TimeUtils.kt                   # Cooldown / frequency check logic
```

## Troubleshooting

### Reminder notification not appearing after upgrade

If you previously installed an older build of this app, **uninstall and reinstall** to reset the notification channel. Android does not allow apps to raise channel importance (`IMPORTANCE_HIGH`) after the channel has already been created at a lower importance level. A fresh install creates the channel correctly.

### Reminder notification not appearing at all

- **Check your lockscreen security type.** `ACTION_USER_PRESENT` only fires on **secured** lockscreens (PIN / pattern / password / biometric). If your device lockscreen is set to "Swipe" or "None", the broadcast is never sent and no reminder will appear. Set a PIN or biometric lock to enable unlock detection.
- **Check notification permission.** On Android 13+, the app requires the `POST_NOTIFICATIONS` runtime permission. Make sure it is granted in Settings → Apps → Unlock Reminder → Notifications.
- **Check battery optimization.** On some OEM devices (MIUI, EMUI, One UI), aggressive battery optimization may stop the foreground service. Exempt the app from battery optimization in Settings → Battery.



Settings are stored in [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore):

| Key | Type | Default |
|-----|------|---------|
| `IS_REMINDER_ENABLED` | Boolean | `false` |
| `REMINDER_MESSAGE` | String | `"Take a deep breath. Choose your next action mindfully."` |
| `REMINDER_FREQUENCY` | String | `"every_unlock"` |
| `LAST_REMINDER_SHOWN_AT_MILLIS` | Long | `0` |

## Known Limitations

- **Unlock detection on OEM devices** — Aggressive battery optimization (e.g., MIUI, EMUI, One UI) may still delay or block `ACTION_USER_PRESENT` on some manufacturer ROMs. Users may need to exempt the app from battery optimization on those devices.
- **Force-stopped apps** will not receive broadcasts on Android 3.1+ until the user manually launches the app again. The foreground service also cannot be restarted automatically after a force-stop.
- **Android 14+ Cached App Freezer** — The foreground service (`UnlockMonitorService`) resolves this issue on stock Android 14+ devices (e.g., Pixel). The service dynamically registers `ACTION_USER_PRESENT` so the broadcast is reliably received while the reminder toggle is enabled.
- **Android 8.0+ implicit broadcast restrictions** — `ACTION_USER_PRESENT` is on the [explicit broadcast exception list](https://developer.android.com/guide/components/broadcast-exceptions) but is now handled via dynamic registration inside the foreground service for maximum reliability.

## Permissions Used

| Permission | Purpose |
|-----------|---------|
| `POST_NOTIFICATIONS` | Show the reminder notification (runtime request on Android 13+) |
| `FOREGROUND_SERVICE` | Required to run `UnlockMonitorService` as a foreground service |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required on Android 14+ for foreground services with type `specialUse` |
| `RECEIVE_BOOT_COMPLETED` | Restart the monitor service automatically after device reboot |
| `VIBRATE` | Allow the reminder notification channel to vibrate on delivery |

No overlays, no accessibility services, no device admin, no internet access.
