package com.mozilla.buildwatch.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val username: Flow<String>
    val notificationsEnabled: Flow<Boolean>
    val watchedPushIds: Flow<Set<Int>>
    val notifiedPushIds: Flow<Set<Int>>
    suspend fun setUsername(value: String)
    suspend fun setNotificationsEnabled(value: Boolean)
    suspend fun setWatchedPushIds(ids: Set<Int>)
    suspend fun setNotifiedPushIds(ids: Set<Int>)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "buildwatch_prefs")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {

    companion object {
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_WATCHED_PUSH_IDS = stringSetPreferencesKey("watched_push_ids")
        private val KEY_NOTIFIED_PUSH_IDS = stringSetPreferencesKey("notified_push_ids")
    }

    override val username: Flow<String> = context.dataStore.data.map { it[KEY_USERNAME] ?: "" }

    override val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: false }

    override val watchedPushIds: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[KEY_WATCHED_PUSH_IDS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    override val notifiedPushIds: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFIED_PUSH_IDS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    override suspend fun setUsername(value: String) {
        context.dataStore.edit { it[KEY_USERNAME] = value }
    }

    override suspend fun setNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = value }
    }

    override suspend fun setWatchedPushIds(ids: Set<Int>) {
        context.dataStore.edit { it[KEY_WATCHED_PUSH_IDS] = ids.map { id -> id.toString() }.toSet() }
    }

    override suspend fun setNotifiedPushIds(ids: Set<Int>) {
        context.dataStore.edit { it[KEY_NOTIFIED_PUSH_IDS] = ids.map { id -> id.toString() }.toSet() }
    }
}
