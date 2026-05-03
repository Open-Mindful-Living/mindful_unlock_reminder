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

Running a **foreground service** keeps the app's process resident and prevents the OS from freezing it, which restores reliable broadcast delivery. The service itself does nothing except keep the process alive; `UnlockReceiver` (declared in the manifest) remains the actual broadcast handler.

The persistent notification is posted on a **silent, badge-free channel** (`IMPORTANCE_MIN`) so it sits quietly in the "Silent" section of the notification shade and does not play a sound, vibrate, or show a badge. You can disable or hide it further via long-press → notification settings on that specific notification without affecting reminder delivery (though on some OEMs, stopping the foreground service may re-enable the freezer).



```
app/src/main/java/com/mindful/unlock/reminder/
├── data/
│   ├── UserPreferences.kt              # Data model (DataStore-backed settings)
│   └── UserPreferencesRepository.kt    # DataStore read/write operations
├── notification/
│   └── UnlockReminderNotificationManager.kt  # Channel creation & notification posting
├── receiver/
│   └── UnlockReceiver.kt              # Listens for ACTION_USER_PRESENT and BOOT_COMPLETED
├── service/
│   └── UnlockMonitorService.kt        # Foreground service — keeps process alive on Android 14+
├── ui/
│   ├── MainActivity.kt                # Entry point, permission handling
│   ├── MainScreen.kt                  # Compose settings screen
│   └── MessageDialogActivity.kt       # Motivational card screen (opened from notification)
└── util/
    ├── ReminderFrequency.kt           # Enum: every_unlock / 30min / daily
    └── TimeUtils.kt                   # Cooldown / frequency check logic
```

## Data Model

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
- **Android 14+ Cached App Freezer** — The foreground service (`UnlockMonitorService`) resolves this issue on stock Android 14+ devices (e.g., Pixel). The `ACTION_USER_PRESENT` broadcast is now reliably received while the reminder toggle is enabled.
- **Android 8.0+ implicit broadcast restrictions** — `ACTION_USER_PRESENT` is on the [explicit broadcast exception list](https://developer.android.com/guide/components/broadcast-exceptions) and is still delivered to manifest-registered receivers.

## Permissions Used

| Permission | Purpose |
|-----------|---------|
| `POST_NOTIFICATIONS` | Show the reminder notification (runtime request on Android 13+) |
| `FOREGROUND_SERVICE` | Required to run `UnlockMonitorService` as a foreground service |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required on Android 14+ for foreground services with type `specialUse` |
| `RECEIVE_BOOT_COMPLETED` | Restart the monitor service automatically after device reboot |

No overlays, no accessibility services, no device admin, no internet access.
