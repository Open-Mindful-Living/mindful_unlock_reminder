package com.mindful.unlock.reminder.util

object TimeUtils {
    fun shouldShowReminder(frequency: ReminderFrequency, lastShownAtMillis: Long): Boolean {
        val now = System.currentTimeMillis()
        return when (frequency) {
            ReminderFrequency.EVERY_UNLOCK -> true
            ReminderFrequency.ONCE_EVERY_30_MIN -> (now - lastShownAtMillis) >= 30 * 60 * 1000L
            ReminderFrequency.ONCE_PER_DAY -> (now - lastShownAtMillis) >= 24 * 60 * 60 * 1000L
        }
    }
}
