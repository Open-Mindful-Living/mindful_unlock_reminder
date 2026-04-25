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

## Architecture

```
app/src/main/java/com/mindful/unlock/reminder/
├── data/
│   ├── UserPreferences.kt              # Data model (DataStore-backed settings)
│   └── UserPreferencesRepository.kt    # DataStore read/write operations
├── notification/
│   └── UnlockReminderNotificationManager.kt  # Channel creation & notification posting
├── receiver/
│   └── UnlockReceiver.kt              # Listens for ACTION_USER_PRESENT
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

- **Unlock detection reliability varies** by Android version and device manufacturer. Aggressive battery optimization (e.g., MIUI, EMUI, One UI) may delay or block the `ACTION_USER_PRESENT` broadcast.
- **Force-stopped apps** will not receive broadcasts on Android 3.1+ until the user manually launches the app again.
- **No background service** — v1 intentionally avoids foreground services, overlays, and accessibility permissions to stay privacy-friendly and simple.
- **Android 8.0+ implicit broadcast restrictions** — `ACTION_USER_PRESENT` is on the [explicit broadcast exception list](https://developer.android.com/guide/components/broadcast-exceptions) and is still delivered to manifest-registered receivers.
- Future versions may explore an optional foreground monitoring service, but v1 stays minimal.

## Permissions Used

| Permission | Purpose |
|-----------|---------|
| `POST_NOTIFICATIONS` | Show the reminder notification (runtime request on Android 13+) |

No overlays, no accessibility services, no device admin, no internet access.
