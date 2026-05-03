package com.mindful.unlock.reminder.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mindful.unlock.reminder.data.UserPreferencesRepository
import com.mindful.unlock.reminder.notification.UnlockReminderNotificationManager
import com.mindful.unlock.reminder.util.ReminderFrequency
import com.mindful.unlock.reminder.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * A foreground service that keeps the app process resident and reliably listens
 * for screen unlocks via a dynamically registered BroadcastReceiver.
 */
class UnlockMonitorService : Service() {

    private lateinit var repository: UserPreferencesRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("UnlockMonitorService", "Received broadcast: ${intent.action}")
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                handleUnlock()
            }
        }
    }

    companion object {
        private const val TAG = "UnlockMonitorService"
        private const val MONITOR_CHANNEL_ID = "unlock_monitor_channel"
        private const val MONITOR_CHANNEL_NAME = "Unlock Monitor Status"
        private const val MONITOR_NOTIFICATION_ID = 1002
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        repository = UserPreferencesRepository(applicationContext)
        
        createMonitorChannel()
        startForeground(MONITOR_NOTIFICATION_ID, buildForegroundNotification())

        // Register for unlock events dynamically. 
        // For system broadcasts like ACTION_USER_PRESENT, we use RECEIVER_EXPORTED.
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(unlockReceiver, filter)
        }
    }

    private fun handleUnlock() {
        Log.d(TAG, "Handling unlock event...")
        serviceScope.launch {
            try {
                val prefs = repository.userPreferencesFlow.first()
                Log.d(TAG, "Reminder enabled: ${prefs.isReminderEnabled}")
                
                if (!prefs.isReminderEnabled) return@launch

                val frequency = ReminderFrequency.fromKey(prefs.reminderFrequency)
                val shouldShow = TimeUtils.shouldShowReminder(frequency, prefs.lastReminderShownAtMillis)
                Log.d(TAG, "Should show reminder (frequency=$frequency): $shouldShow")

                if (!shouldShow) return@launch

                Log.d(TAG, "Showing notification...")
                UnlockReminderNotificationManager.showReminderNotification(
                    applicationContext,
                    prefs.reminderMessage
                )

                repository.updateLastReminderShownAt(System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e(TAG, "Error handling unlock", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createMonitorChannel() {
        val channel = NotificationChannel(
            MONITOR_CHANNEL_ID,
            MONITOR_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setShowBadge(false)
            description = "Keeps unlock detection active"
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification() =
        NotificationCompat.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Unlock Reminder is active")
            .setContentText("Monitoring for mindful moments...")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
}
