package com.corner.myshoppinglist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.corner.myshoppinglist.data.local.entities.AppSettings
import com.corner.myshoppinglist.data.repository.BackupRepository
import com.corner.myshoppinglist.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {
    val settings: StateFlow<AppSettings?> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus = _backupStatus.asStateFlow()

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

    fun exportData(itemsOnly: Boolean, onDataReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonData = backupRepository.exportData(itemsOnly)
                onDataReady(jsonData)
            } catch (e: Exception) {
                _backupStatus.value = BackupStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun restoreData(jsonData: String) {
        viewModelScope.launch {
            _backupStatus.value = BackupStatus.Loading
            try {
                backupRepository.restoreData(jsonData)
                _backupStatus.value = BackupStatus.Success("Data restored successfully")
            } catch (e: Exception) {
                _backupStatus.value = BackupStatus.Error(e.message ?: "Failed to restore data")
            }
        }
    }

    fun resetBackupStatus() {
        _backupStatus.value = BackupStatus.Idle
    }
}

sealed class BackupStatus {
    object Idle : BackupStatus()
    object Loading : BackupStatus()
    data class Success(val message: String) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}

class SettingsViewModelFactory(
    private val repository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, backupRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
