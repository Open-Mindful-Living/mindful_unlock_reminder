package com.mindful.unlock.reminder.data

data class UserPreferences(
    val isReminderEnabled: Boolean = false,
    val reminderMessage: String = "Take a deep breath. Choose your next action mindfully.",
    val reminderFrequency: String = "every_unlock",
    val lastReminderShownAtMillis: Long = 0L
)
