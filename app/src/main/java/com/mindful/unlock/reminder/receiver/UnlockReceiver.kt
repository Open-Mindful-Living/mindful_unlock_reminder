package com.mindful.unlock.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mindful.unlock.reminder.data.UserPreferencesRepository
import com.mindful.unlock.reminder.service.UnlockMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles BOOT_COMPLETED to restart UnlockMonitorService after device reboot,
 * so the process stays resident and the dynamically registered ACTION_USER_PRESENT
 * receiver is re-registered without requiring the user to open the app.
 *
 * ACTION_USER_PRESENT handling has been moved to a dynamically registered receiver
 * inside UnlockMonitorService, which is more reliable on Android 14+ devices that
 * use the Cached App Freezer.
 */
class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            handleBoot(context)
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
}
