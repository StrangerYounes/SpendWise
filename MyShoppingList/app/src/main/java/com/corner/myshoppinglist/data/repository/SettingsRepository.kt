package com.corner.myshoppinglist.data.repository

import com.corner.myshoppinglist.data.local.dao.SettingsDao
import com.corner.myshoppinglist.data.local.entities.AppSettings
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val settingsDao: SettingsDao) {
    val settings: Flow<AppSettings?> = settingsDao.getSettings()

    suspend fun updateSettings(settings: AppSettings) {
        settingsDao.updateSettings(settings)
    }
}
