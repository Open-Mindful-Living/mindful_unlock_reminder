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
 * Receives BOOT_COMPLETED broadcast to restart the monitoring service.
 *
 * Screen unlock (ACTION_USER_PRESENT) is handled dynamically by UnlockMonitorService
 * while it is running, which is more reliable on modern Android versions.
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
