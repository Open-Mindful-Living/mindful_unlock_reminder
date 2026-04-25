package com.mindful.unlock.reminder.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val IS_REMINDER_ENABLED = booleanPreferencesKey("IS_REMINDER_ENABLED")
        private val REMINDER_MESSAGE = stringPreferencesKey("REMINDER_MESSAGE")
        private val REMINDER_FREQUENCY = stringPreferencesKey("REMINDER_FREQUENCY")
        private val LAST_REMINDER_SHOWN_AT_MILLIS = longPreferencesKey("LAST_REMINDER_SHOWN_AT_MILLIS")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            isReminderEnabled = prefs[IS_REMINDER_ENABLED] ?: false,
            reminderMessage = prefs[REMINDER_MESSAGE]
                ?: "Take a deep breath. Choose your next action mindfully.",
            reminderFrequency = prefs[REMINDER_FREQUENCY] ?: "every_unlock",
            lastReminderShownAtMillis = prefs[LAST_REMINDER_SHOWN_AT_MILLIS] ?: 0L
        )
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setReminderMessage(message: String) {
        context.dataStore.edit { prefs ->
            prefs[REMINDER_MESSAGE] = message
        }
    }

    suspend fun setReminderFrequency(frequency: String) {
        context.dataStore.edit { prefs ->
            prefs[REMINDER_FREQUENCY] = frequency
        }
    }

    suspend fun updateLastReminderShownAt(millis: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_REMINDER_SHOWN_AT_MILLIS] = millis
        }
    }
}
