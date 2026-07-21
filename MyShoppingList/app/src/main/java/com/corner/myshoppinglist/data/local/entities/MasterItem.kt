package com.corner.myshoppinglist.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_items")
data class MasterItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val averagePrice: Double = 0.0,
    val lastPrice: Double = 0.0,
    val lowestPrice: Double = 0.0,
    val highestPrice: Double = 0.0,
    val purchaseCount: Int = 0,
    val lastPurchaseDate: Long = 0
)
