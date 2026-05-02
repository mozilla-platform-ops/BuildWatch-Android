package com.mozilla.buildwatch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mozilla.buildwatch.repository.DataStoreSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DataStoreSettingsRepository(application)

    val username = repo.username.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        "",
    )

    val notificationsEnabled = repo.notificationsEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
    )

    fun setUsername(handle: String) {
        viewModelScope.launch {
            val trimmed = handle.trim().substringBefore("@")
            repo.setUsername(if (trimmed.isEmpty()) "" else "$trimmed@mozilla.com")
        }
    }

    fun setNotificationsEnabled(value: Boolean) {
        viewModelScope.launch { repo.setNotificationsEnabled(value) }
    }
}
