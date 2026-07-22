package com.corner.myshoppinglist.data.repository

import com.corner.myshoppinglist.data.local.dao.*
import com.corner.myshoppinglist.data.local.entities.MasterItem
import com.corner.myshoppinglist.data.local.entities.Photo
import com.corner.myshoppinglist.data.local.entities.ShoppingItem
import com.corner.myshoppinglist.data.local.entities.ShoppingList
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao,
    private val masterItemDao: MasterItemDao,
    private val photoDao: PhotoDao,
    private val statsDao: StatsDao
) {
    // Shopping Lists
    val allShoppingLists: Flow<List<ShoppingListWithDetails>> = shoppingListDao.getAllShoppingListsWithDetails()
    
    // Stats
    val itemStats: Flow<List<ItemStat>> = statsDao.getItemStats()
    val expensiveItems: Flow<List<ExpensiveItem>> = statsDao.getMostExpensiveItems()
    val spentPerMonth: Flow<List<PeriodStat>> = statsDao.getSpentPerMonth()

    fun searchShoppingLists(query: String): Flow<List<ShoppingListWithDetails>> =
        shoppingListDao.searchShoppingListsWithDetails(query)

    suspend fun getShoppingListById(id: Long): ShoppingList? =
        shoppingListDao.getShoppingListById(id)

    suspend fun insertShoppingList(shoppingList: ShoppingList): Long =
        shoppingListDao.insertShoppingList(shoppingList)

    suspend fun updateShoppingList(shoppingList: ShoppingList) =
        shoppingListDao.updateShoppingList(shoppingList)

    suspend fun deleteShoppingList(shoppingList: ShoppingList) =
        shoppingListDao.deleteShoppingList(shoppingList)

    // Shopping Items
    fun getItemsForList(listId: Long): Flow<List<ShoppingItem>> =
        shoppingItemDao.getItemsForList(listId)

    suspend fun insertShoppingItem(item: ShoppingItem) {
        shoppingItemDao.insertItem(item)
    }

    suspend fun updateShoppingItem(item: ShoppingItem) {
        val oldItem = shoppingItemDao.getItemById(item.id)
        shoppingItemDao.updateItem(item)

        // If actual price was just entered or changed, update MasterItem
        if (item.actualPrice != null && item.actualPrice != oldItem?.actualPrice) {
            updateMasterItem(item)
        }
    }

    suspend fun deleteShoppingItem(item: ShoppingItem) =
        shoppingItemDao.deleteItem(item)

    fun searchItems(query: String): Flow<List<ShoppingItem>> =
        shoppingItemDao.searchItems(query)

    // Master Items
    val allMasterItems: Flow<List<MasterItem>> = masterItemDao.getAllMasterItems()

    fun searchMasterItems(query: String): Flow<List<MasterItem>> =
        masterItemDao.searchMasterItems(query)

    suspend fun updateMasterItemManual(item: MasterItem) {
        masterItemDao.updateMasterItem(item)
    }

    suspend fun insertMasterItem(item: MasterItem) {
        masterItemDao.insertMasterItem(item)
    }

    suspend fun deleteMasterItem(item: MasterItem) {
        masterItemDao.deleteMasterItem(item)
    }

    private suspend fun updateMasterItem(shoppingItem: ShoppingItem) {
        val name = shoppingItem.itemName
        val price = shoppingItem.actualPrice ?: return
        val date = System.currentTimeMillis()

        val existingMaster = masterItemDao.getMasterItemByName(name)
        if (existingMaster == null) {
            val newMaster = MasterItem(
                name = name,
                averagePrice = price,
                lastPrice = price,
                lowestPrice = price,
                highestPrice = price,
                purchaseCount = 1,
                lastPurchaseDate = date
            )
            masterItemDao.insertMasterItem(newMaster)
        } else {
            val newCount = existingMaster.purchaseCount + 1
            val newAverage = (existingMaster.averagePrice * existingMaster.purchaseCount + price) / newCount
            val updatedMaster = existingMaster.copy(
                averagePrice = newAverage,
                lastPrice = price,
                lowestPrice = minOf(existingMaster.lowestPrice, price),
                highestPrice = maxOf(existingMaster.highestPrice, price),
                purchaseCount = newCount,
                lastPurchaseDate = date
            )
            masterItemDao.updateMasterItem(updatedMaster)
        }
    }

    // Photos
    fun getPhotosForList(listId: Long): Flow<List<Photo>> =
        photoDao.getPhotosForList(listId)

    suspend fun insertPhoto(photo: Photo) =
        photoDao.insertPhoto(photo)

    suspend fun deletePhoto(photo: Photo) =
        photoDao.deletePhoto(photo)

    suspend fun duplicateList(list: ShoppingList, items: List<ShoppingItem>): Long {
        val newListId = shoppingListDao.insertShoppingList(
            list.copy(
                id = 0,
                name = "Copy of ${list.name}",
                createdDate = System.currentTimeMillis(),
                completedDate = null,
                isCompleted = false
            )
        )
        items.forEach { item ->
            shoppingItemDao.insertItem(
                item.copy(
                    id = 0,
                    listId = newListId,
                    purchased = false,
                    actualPrice = null
                )
            )
        }
        return newListId
    }
}
