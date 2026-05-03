package com.mindful.unlock.reminder.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.mindful.unlock.reminder.data.UserPreferencesRepository
import com.mindful.unlock.reminder.notification.UnlockReminderNotificationManager
import com.mindful.unlock.reminder.service.UnlockMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionGranted = mutableStateOf(true)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted.value = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = UserPreferencesRepository(applicationContext)
        UnlockReminderNotificationManager.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            notificationPermissionGranted.value = alreadyGranted
            if (!alreadyGranted) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Re-arm the foreground monitor service if the reminder was already enabled
        // (handles reinstalls and reboots where the user does not change settings).
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = repository.userPreferencesFlow.first()
            if (prefs.isReminderEnabled) {
                ContextCompat.startForegroundService(
                    applicationContext,
                    Intent(applicationContext, UnlockMonitorService::class.java)
                )
            }
        }

        setContent {
            MaterialTheme {
                MainScreen(
                    repository = repository,
                    notificationPermissionGranted = notificationPermissionGranted
                )
            }
        }
    }
}
