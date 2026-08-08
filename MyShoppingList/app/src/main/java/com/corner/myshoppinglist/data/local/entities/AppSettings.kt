package com.corner.myshoppinglist.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 0,
    val darkTheme: Boolean? = null, // null means follow system
    val currencySymbol: String = "$",
    val lastConversionRate: Double = 1.0
)
