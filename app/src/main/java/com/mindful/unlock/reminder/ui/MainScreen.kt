package com.mindful.unlock.reminder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mindful.unlock.reminder.data.UserPreferences
import com.mindful.unlock.reminder.data.UserPreferencesRepository
import com.mindful.unlock.reminder.notification.UnlockReminderNotificationManager
import com.mindful.unlock.reminder.util.ReminderFrequency
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: UserPreferencesRepository,
    notificationManager: UnlockReminderNotificationManager,
    notificationPermissionGranted: androidx.compose.runtime.State<Boolean>
) {
    val prefs by repository.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val scope = rememberCoroutineScope()

    var isEnabled by remember(prefs.isReminderEnabled) { mutableStateOf(prefs.isReminderEnabled) }
    var message by remember(prefs.reminderMessage) { mutableStateOf(prefs.reminderMessage) }
    var frequency by remember(prefs.reminderFrequency) {
        mutableStateOf(ReminderFrequency.fromKey(prefs.reminderFrequency))
    }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Message Preview") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { showPreviewDialog = false }) { Text("Done") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Unlock Reminder") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            if (!notificationPermissionGranted.value) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notification permission is required to show unlock reminders. " +
                            "Please grant it in Settings.",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Enable/disable switch
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Enable unlock reminder",
                        modifier = Modifier.align(Alignment.CenterStart),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }

            // Message input
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Motivational message") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                minLines = 2
            )

            // Frequency dropdown
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = frequency.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Reminder frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    ReminderFrequency.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                frequency = option
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        repository.setReminderEnabled(isEnabled)
                        repository.setReminderMessage(message)
                        repository.setReminderFrequency(frequency.key)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            // Preview button
            Button(
                onClick = { showPreviewDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Preview message")
            }

            Text(
                text = "Android may limit unlock detection depending on battery settings and " +
                    "device manufacturer. This MVP uses a privacy-friendly local approach.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
