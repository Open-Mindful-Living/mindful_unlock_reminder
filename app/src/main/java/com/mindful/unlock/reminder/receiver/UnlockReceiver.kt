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
 */
class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        // Use a coroutine scope on IO to read preferences and post notification without
        // blocking the main thread. BroadcastReceiver.goAsync() is not used here because
        // DataStore reads are fast and the scope keeps work alive independently.
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
            }
        }
    }
}
