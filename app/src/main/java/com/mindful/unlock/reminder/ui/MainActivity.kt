package com.mindful.unlock.reminder.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.mindful.unlock.reminder.data.UserPreferencesRepository
import com.mindful.unlock.reminder.notification.UnlockReminderNotificationManager

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
        val notificationManager = UnlockReminderNotificationManager
        notificationManager.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MainScreen(
                repository = repository,
                notificationManager = notificationManager,
                notificationPermissionGranted = notificationPermissionGranted
            )
        }
    }
}
