package com.corner.myshoppinglist

import android.app.Application
import com.corner.myshoppinglist.data.local.AppDatabase
import com.corner.myshoppinglist.data.repository.SettingsRepository
import com.corner.myshoppinglist.data.repository.ShoppingRepository

class ShoppingApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        ShoppingRepository(
            database.shoppingListDao(),
            database.shoppingItemDao(),
            database.masterItemDao(),
            database.photoDao(),
            database.statsDao(),
            database.storeDao()
        )
    }
    val settingsRepository by lazy {
        SettingsRepository(database.settingsDao())
    }
}
