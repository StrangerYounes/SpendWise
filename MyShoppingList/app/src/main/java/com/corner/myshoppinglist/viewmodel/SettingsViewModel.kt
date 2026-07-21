package com.corner.myshoppinglist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.corner.myshoppinglist.data.local.entities.AppSettings
import com.corner.myshoppinglist.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings?> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    fun toggleDarkTheme(dark: Boolean?) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettings()
            repository.updateSettings(current.copy(darkTheme = dark))
        }
    }

    fun setCurrency(symbol: String) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettings()
            repository.updateSettings(current.copy(currencySymbol = symbol))
        }
    }
}

class SettingsViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
