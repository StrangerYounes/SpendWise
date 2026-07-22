package com.corner.myshoppinglist.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "store_item_prices",
    primaryKeys = ["masterItemId", "storeId"],
    foreignKeys = [
        ForeignKey(
            entity = MasterItem::class,
            parentColumns = ["id"],
            childColumns = ["masterItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Store::class,
            parentColumns = ["id"],
            childColumns = ["storeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["masterItemId"]),
        Index(value = ["storeId"])
    ]
)
data class StoreItemPrice(
    val masterItemId: Long,
    val storeId: Long,
    val lastPrice: Double,
    val lowestPrice: Double = lastPrice,
    val highestPrice: Double = lastPrice,
    val lastUpdated: Long = System.currentTimeMillis()
)
