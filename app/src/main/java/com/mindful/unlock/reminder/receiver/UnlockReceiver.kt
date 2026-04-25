package com.mindful.unlock.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mindful.unlock.reminder.data.UserPreferencesRepository
import com.mindful.unlock.reminder.notification.UnlockReminderNotificationManager
import com.mindful.unlock.reminder.util.ReminderFrequency
import com.mindful.unlock.reminder.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives ACTION_USER_PRESENT (device unlocked) broadcasts.
 *
 * NOTE: This is an experimental, notification-first MVP approach. Broadcast-based unlock
 * detection may be restricted on some OEM devices or under aggressive battery optimization.
 * Users may need to exempt the app from battery optimization for reliable delivery.
 *
 * v1 intentionally avoids foreground services, overlays, and accessibility permissions.
 */
class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        // goAsync() extends the broadcast window beyond onReceive() so the coroutine
        // can finish its IO work before the system reclaims the process.
        // Note (MVP): a new scope is created per broadcast. Rapid repeated unlocks are
        // unlikely in practice, and the frequency cooldown in TimeUtils prevents duplicate
        // notifications even if two coroutines race. A future improvement could use a
        // shared Application-level scope with job deduplication.
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = UserPreferencesRepository(context.applicationContext)
                val prefs = repository.userPreferencesFlow.first()

                if (!prefs.isReminderEnabled) return@launch

                val frequency = ReminderFrequency.fromKey(prefs.reminderFrequency)
                if (!TimeUtils.shouldShowReminder(frequency, prefs.lastReminderShownAtMillis)) {
                    return@launch
                }

                UnlockReminderNotificationManager.showReminderNotification(
                    context.applicationContext,
                    prefs.reminderMessage
                )

                repository.updateLastReminderShownAt(System.currentTimeMillis())
            } catch (e: Exception) {
                // Gracefully handle any unexpected errors to avoid crashing on unlock
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
