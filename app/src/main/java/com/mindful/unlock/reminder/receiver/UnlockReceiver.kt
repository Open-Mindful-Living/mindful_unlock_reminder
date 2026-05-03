package com.mindful.unlock.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mindful.unlock.reminder.data.UserPreferencesRepository
import com.mindful.unlock.reminder.notification.UnlockReminderNotificationManager
import com.mindful.unlock.reminder.service.UnlockMonitorService
import com.mindful.unlock.reminder.util.ReminderFrequency
import com.mindful.unlock.reminder.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives ACTION_USER_PRESENT (device unlocked) and BOOT_COMPLETED broadcasts.
 *
 * On ACTION_USER_PRESENT: checks reminder settings and posts the motivational notification
 * if the reminder is enabled and the cooldown has passed.
 *
 * On BOOT_COMPLETED: restarts UnlockMonitorService if the reminder was enabled before reboot,
 * so the process stays resident without requiring the user to re-open the app.
 */
class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> handleBoot(context)
            Intent.ACTION_USER_PRESENT -> handleUnlock(context)
        }
    }

    private fun handleBoot(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = UserPreferencesRepository(context.applicationContext)
                    .userPreferencesFlow.first()
                if (prefs.isReminderEnabled) {
                    ContextCompat.startForegroundService(
                        context.applicationContext,
                        Intent(context.applicationContext, UnlockMonitorService::class.java)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleUnlock(context: Context) {
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
