package com.corner.myshoppinglist.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val masterItems: List<MasterItemBackup>? = null,
    val shoppingLists: List<ShoppingListBackup>? = null,
    val shoppingItems: List<ShoppingItemBackup>? = null,
    val categories: List<CategoryBackup>? = null,
    val stores: List<StoreBackup>? = null,
    val storeItemPrices: List<StoreItemPriceBackup>? = null,
    val settings: AppSettingsBackup? = null
)

@Serializable
data class MasterItemBackup(
    val name: String,
    val averagePrice: Double,
    val lastPrice: Double,
    val lowestPrice: Double,
    val highestPrice: Double,
    val purchaseCount: Int,
    val lastPurchaseDate: Long
)

@Serializable
data class ShoppingListBackup(
    val id: Long, // Used for mapping shopping items
    val name: String,
    val createdDate: Long,
    val completedDate: Long?,
    val isCompleted: Boolean,
    val storeName: String?,
    val categoryName: String?
)

@Serializable
data class ShoppingItemBackup(
    val listId: Long,
    val itemName: String,
    val estimatedPrice: Double?,
    val actualPrice: Double?,
    val quantity: Double,
    val unit: String?,
    val purchased: Boolean,
    val orderIndex: Int,
    val notes: String?,
    val storeName: String?
)

@Serializable
data class CategoryBackup(
    val name: String,
    val color: Int
)

@Serializable
data class StoreBackup(
    val name: String
)

@Serializable
data class StoreItemPriceBackup(
    val itemName: String,
    val storeName: String,
    val lastPrice: Double,
    val lowestPrice: Double,
    val highestPrice: Double,
    val lastUpdated: Long
)

@Serializable
data class AppSettingsBackup(
    val darkTheme: Boolean?,
    val currencySymbol: String
)
