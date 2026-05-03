package com.mindful.unlock.reminder.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * A minimal foreground service whose only job is to keep the app process resident on
 * Android 14+ devices that use the Cached App Freezer.
 *
 * When the Cached App Freezer suspends a process, manifest-registered BroadcastReceivers
 * (including UnlockReceiver) stop receiving ACTION_USER_PRESENT. Running a foreground
 * service prevents the OS from freezing this process, which restores reliable broadcast
 * delivery without requiring any dynamic receiver registration.
 *
 * The service does NOT register UnlockReceiver — the existing <receiver> entry in
 * AndroidManifest.xml remains the broadcast handler.
 */
class UnlockMonitorService : Service() {

    companion object {
        private const val MONITOR_CHANNEL_ID = "unlock_monitor_channel"
        private const val MONITOR_CHANNEL_NAME = "Unlock Monitor"
        private const val MONITOR_NOTIFICATION_ID = 1002
    }

    override fun onCreate() {
        super.onCreate()
        createMonitorChannel()
        startForeground(MONITOR_NOTIFICATION_ID, buildForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY so Android recreates the service if it is killed,
        // keeping the process resident as long as the reminder is enabled.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
