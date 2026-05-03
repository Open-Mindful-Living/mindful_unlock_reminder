package com.mindful.unlock.reminder.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.mindful.unlock.reminder.data.UserPreferencesRepository
import com.mindful.unlock.reminder.notification.UnlockReminderNotificationManager
import com.mindful.unlock.reminder.util.ReminderFrequency
import com.mindful.unlock.reminder.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * A foreground service that keeps the app process resident on Android 14+ (Cached App Freezer)
 * AND dynamically registers an ACTION_USER_PRESENT receiver so unlock events are reliably
 * delivered even when the process would otherwise be frozen.
 *
 * Dynamic registration is required on Android 14+ because manifest-registered receivers
 * inside frozen processes do not receive system broadcasts. The foreground service prevents
 * the OS from freezing the process; the dynamic receiver handles the actual unlock event.
 *
 * UnlockReceiver (manifest-registered) is now BOOT_COMPLETED only — it restarts this
 * service after a reboot so the dynamic receiver is re-registered.
 */
class UnlockMonitorService : Service() {

    companion object {
        private const val TAG = "UnlockMonitorService"
        private const val MONITOR_CHANNEL_ID = "unlock_monitor_channel"
        private const val MONITOR_CHANNEL_NAME = "Unlock Monitor"
        private const val MONITOR_NOTIFICATION_ID = 1002
    }

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                Log.d(TAG, "ACTION_USER_PRESENT broadcast received")
                handleUnlock()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        // Ensure the reminder channel exists even if MainActivity was never opened
        // (e.g., service restarted by BOOT_COMPLETED receiver).
        UnlockReminderNotificationManager.createNotificationChannel(applicationContext)

        createMonitorChannel()

        val notification = buildForegroundNotification()
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, MONITOR_NOTIFICATION_ID, notification, fgsType)

        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(unlockReceiver, filter)
        }
        Log.d(TAG, "Dynamic ACTION_USER_PRESENT receiver registered")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        // START_STICKY so Android recreates the service if it is killed,
        // keeping the process resident as long as the reminder is enabled.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Receiver was not registered or already unregistered: ${e.message}")
        }
        job.cancel()
    }

    private fun handleUnlock() {
        serviceScope.launch {
            try {
                val repository = UserPreferencesRepository(applicationContext)
                val prefs = repository.userPreferencesFlow.first()

                Log.d(TAG, "Reminder enabled: ${prefs.isReminderEnabled}")
                if (!prefs.isReminderEnabled) return@launch

                val frequency = ReminderFrequency.fromKey(prefs.reminderFrequency)
                val shouldShow = TimeUtils.shouldShowReminder(frequency, prefs.lastReminderShownAtMillis)
                Log.d(TAG, "Frequency: $frequency, shouldShow: $shouldShow")
                if (!shouldShow) return@launch

                val posted = UnlockReminderNotificationManager.showReminderNotification(
                    applicationContext,
                    prefs.reminderMessage
                )
                if (posted) {
                    Log.d(TAG, "Reminder notification posted — updating lastReminderShownAt")
                    repository.updateLastReminderShownAt(System.currentTimeMillis())
                } else {
                    Log.d(TAG, "Reminder notification was NOT posted — lastReminderShownAt unchanged")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in handleUnlock", e)
            }
        }
    }

    private fun createMonitorChannel() {
        val channel = NotificationChannel(
            MONITOR_CHANNEL_ID,
            MONITOR_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setShowBadge(false)
            description = "Keeps unlock detection active in the background"
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification() =
        NotificationCompat.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Mindful Unlock Reminder is active")
            .setContentText("Listening for screen unlocks")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
}
