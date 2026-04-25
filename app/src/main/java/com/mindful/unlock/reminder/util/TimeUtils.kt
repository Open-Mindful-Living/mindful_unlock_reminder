package com.mindful.unlock.reminder.util

object TimeUtils {

    private const val THIRTY_MINUTES_MILLIS = 30 * 60 * 1_000L
    private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1_000L

    fun shouldShowReminder(frequency: ReminderFrequency, lastShownAtMillis: Long): Boolean {
        val now = System.currentTimeMillis()
        return when (frequency) {
            ReminderFrequency.EVERY_UNLOCK -> true
            ReminderFrequency.ONCE_EVERY_30_MIN -> (now - lastShownAtMillis) >= THIRTY_MINUTES_MILLIS
            ReminderFrequency.ONCE_PER_DAY -> (now - lastShownAtMillis) >= ONE_DAY_MILLIS
        }
    }
}
