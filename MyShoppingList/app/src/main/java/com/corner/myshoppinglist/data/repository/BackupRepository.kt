package com.corner.myshoppinglist.data.repository

import androidx.room.withTransaction
import com.corner.myshoppinglist.data.backup.*
import com.corner.myshoppinglist.data.local.AppDatabase
import com.corner.myshoppinglist.data.local.entities.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupRepository(private val database: AppDatabase) {

    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportData(itemsOnly: Boolean): String {
        val masterItems = database.masterItemDao().getAllMasterItems().firstOrNull() ?: emptyList()
        
        val backupData = if (itemsOnly) {
            BackupData(
                masterItems = masterItems.map { it.toBackup() }
            )
        } else {
            val shoppingLists = database.shoppingListDao().getAllShoppingLists().firstOrNull() ?: emptyList()
            val shoppingItems = database.shoppingItemDao().getAllItems().firstOrNull() ?: emptyList()
            val categories = database.categoryDao().getAllCategories().firstOrNull() ?: emptyList()
            val stores = database.storeDao().getAllStores().firstOrNull() ?: emptyList()
            val storePrices = database.storeDao().getAllStorePrices().firstOrNull() ?: emptyList()
            val settings = database.settingsDao().getSettings().firstOrNull()

            val storeMap = stores.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }
            val masterItemMap = masterItems.associateBy { it.id }

            BackupData(
                masterItems = masterItems.map { it.toBackup() },
                shoppingLists = shoppingLists.map { list ->
                    val storeName = list.storeId?.let { storeMap[it] }?.name
                    val categoryName = list.categoryId?.let { categoryMap[it] }?.name
                    list.toBackup(storeName, categoryName)
                },
                shoppingItems = shoppingItems.map { item ->
                    val storeName = item.storeId?.let { storeMap[it] }?.name
                    item.toBackup(storeName)
                },
                categories = categories.map { it.toBackup() },
                stores = stores.map { it.toBackup() },
                storeItemPrices = storePrices.map { price ->
                    val itemName = masterItemMap[price.masterItemId]?.name
                    val storeName = storeMap[price.storeId]?.name
                    price.toBackup(itemName ?: "Unknown", storeName ?: "Unknown")
                },
                settings = settings?.toBackup()
            )
        }
        
        return json.encodeToString(backupData)
    }

    suspend fun restoreData(jsonData: String) {
        val backupData = json.decodeFromString<BackupData>(jsonData)
        
        database.withTransaction {
            // 1. Categories
            backupData.categories?.forEach { catBackup ->
                val existing = database.categoryDao().getCategoryByName(catBackup.name)
                if (existing == null) {
                    database.categoryDao().insertCategory(Category(name = catBackup.name, color = catBackup.color))
                }
            }

            // 2. Stores
            backupData.stores?.forEach { storeBackup ->
                val existing = database.storeDao().getStoreByName(storeBackup.name)
                if (existing == null) {
                    database.storeDao().insertStore(Store(name = storeBackup.name))
                }
            }

            // 3. Master Items
            backupData.masterItems?.forEach { itemBackup ->
                val existing = database.masterItemDao().getMasterItemByName(itemBackup.name)
                if (existing == null) {
                    database.masterItemDao().insertMasterItem(
                        MasterItem(
                            name = itemBackup.name,
                            averagePrice = itemBackup.averagePrice,
                            lastPrice = itemBackup.lastPrice,
                            lowestPrice = itemBackup.lowestPrice,
                            highestPrice = itemBackup.highestPrice,
                            purchaseCount = itemBackup.purchaseCount,
                            lastPurchaseDate = itemBackup.lastPurchaseDate
                        )
                    )
                }
            }

            // 4. Store Item Prices
            backupData.storeItemPrices?.forEach { priceBackup ->
                val item = database.masterItemDao().getMasterItemByName(priceBackup.itemName)
                val store = database.storeDao().getStoreByName(priceBackup.storeName)
                if (item != null && store != null) {
                    database.storeDao().insertStorePrice(
                        StoreItemPrice(
                            masterItemId = item.id,
                            storeId = store.id,
                            lastPrice = priceBackup.lastPrice,
                            lowestPrice = priceBackup.lowestPrice,
                            highestPrice = priceBackup.highestPrice,
                            lastUpdated = priceBackup.lastUpdated
                        )
                    )
                }
            }

            // 5. Shopping Lists and Items
            val listIdMap = mutableMapOf<Long, Long>() // Old ID to New ID
            backupData.shoppingLists?.forEach { listBackup ->
                val store = listBackup.storeName?.let { database.storeDao().getStoreByName(it) }
                val category = listBackup.categoryName?.let { database.categoryDao().getCategoryByName(it) }
                
                val newListId = database.shoppingListDao().insertShoppingList(
                    ShoppingList(
                        name = listBackup.name,
                        createdDate = listBackup.createdDate,
                        completedDate = listBackup.completedDate,
                        isCompleted = listBackup.isCompleted,
                        storeId = store?.id,
                        categoryId = category?.id
                    )
                )
                listIdMap[listBackup.id] = newListId
            }

            backupData.shoppingItems?.forEach { itemBackup ->
                val newListId = listIdMap[itemBackup.listId]
                if (newListId != null) {
                    val store = itemBackup.storeName?.let { database.storeDao().getStoreByName(it) }
                    database.shoppingItemDao().insertItem(
                        ShoppingItem(
                            listId = newListId,
                            itemName = itemBackup.itemName,
                            estimatedPrice = itemBackup.estimatedPrice,
                            actualPrice = itemBackup.actualPrice,
                            quantity = itemBackup.quantity,
                            unit = itemBackup.unit,
                            purchased = itemBackup.purchased,
                            orderIndex = itemBackup.orderIndex,
                            notes = itemBackup.notes,
                            storeId = store?.id
                        )
                    )
                }
            }

            // 6. Settings
            backupData.settings?.let { settingsBackup ->
                database.settingsDao().updateSettings(
                    AppSettings(
                        darkTheme = settingsBackup.darkTheme,
                        currencySymbol = settingsBackup.currencySymbol
                    )
                )
            }
        }
    }

    // Extension functions for mapping
    private fun MasterItem.toBackup() = MasterItemBackup(name, averagePrice, lastPrice, lowestPrice, highestPrice, purchaseCount, lastPurchaseDate)
    private fun ShoppingList.toBackup(storeName: String?, categoryName: String?) = ShoppingListBackup(id, name, createdDate, completedDate, isCompleted, storeName, categoryName)
    private fun ShoppingItem.toBackup(storeName: String?) = ShoppingItemBackup(listId, itemName, estimatedPrice, actualPrice, quantity, unit, purchased, orderIndex, notes, storeName)
    private fun Category.toBackup() = CategoryBackup(name, color)
    private fun Store.toBackup() = StoreBackup(name)
    private fun StoreItemPrice.toBackup(itemName: String, storeName: String) = StoreItemPriceBackup(itemName, storeName, lastPrice, lowestPrice, highestPrice, lastUpdated)
    private fun AppSettings.toBackup() = AppSettingsBackup(darkTheme, currencySymbol)
}
