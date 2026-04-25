package com.mindful.unlock.reminder.util

enum class ReminderFrequency(val key: String, val displayName: String) {
    EVERY_UNLOCK("every_unlock", "Every unlock"),
    ONCE_EVERY_30_MIN("once_every_30_min", "Once every 30 minutes"),
    ONCE_PER_DAY("once_per_day", "Once per day");

    companion object {
        fun fromKey(key: String): ReminderFrequency =
            entries.find { it.key == key } ?: EVERY_UNLOCK
    }
}
