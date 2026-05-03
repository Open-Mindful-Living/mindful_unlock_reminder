package com.mindful.unlock.reminder.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mindful.unlock.reminder.ui.MessageDialogActivity

object UnlockReminderNotificationManager {

    private const val TAG = "UnlockReminderNotif"

    // Note: Changing channel importance after install requires uninstall + reinstall.
    // Android does not allow apps to raise channel importance after initial creation.
    private const val CHANNEL_ID = "unlock_reminder_channel"
    private const val CHANNEL_NAME = "Unlock Reminders"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows mindful reminders on screen unlock"
            enableLights(true)
            lightColor = Color.BLUE
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Posts the reminder notification. Returns true if the notification was successfully
     * posted, false if the permission was missing or an error occurred.
     */
    fun showReminderNotification(context: Context, message: String): Boolean {
        // Always ensure the channel exists before posting (idempotent call).
        createNotificationChannel(context)

        // On API 33+ check POST_NOTIFICATIONS permission before posting.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.d(TAG, "POST_NOTIFICATIONS permission not granted — skipping notification")
                return false
            }
        }

        val intent = Intent(context, MessageDialogActivity::class.java).apply {
            putExtra("message", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("A mindful reminder")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Reminder notification posted successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post reminder notification: ${e.message}")
            false
        }
    }
}
