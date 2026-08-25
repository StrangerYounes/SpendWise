package com.corner.myshoppinglist.data.repository

import com.corner.myshoppinglist.data.local.dao.*
import com.corner.myshoppinglist.data.local.entities.*
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao,
    private val masterItemDao: MasterItemDao,
    private val photoDao: PhotoDao,
    private val statsDao: StatsDao,
    private val storeDao: StoreDao,
    private val categoryDao: CategoryDao
) {
    // Shopping Lists
    val allShoppingLists: Flow<List<ShoppingListWithDetails>> = shoppingListDao.getAllShoppingListsWithDetails()
    
    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
    suspend fun getCategoryByName(name: String) = categoryDao.getCategoryByName(name)
    suspend fun getCategoryById(id: Long) = categoryDao.getCategoryById(id)
    
    // Stores
    val allStores: Flow<List<Store>> = storeDao.getAllStores()

    suspend fun getStoreById(id: Long): Store? = storeDao.getStoreById(id)
    suspend fun insertStore(store: Store): Long = storeDao.insertStore(store)
    suspend fun updateStore(store: Store) = storeDao.updateStore(store)
    suspend fun deleteStore(store: Store) = storeDao.deleteStore(store)

    // Store Prices
    fun getPricesForItem(masterItemId: Long): Flow<List<StoreItemPrice>> = 
        storeDao.getPricesForItem(masterItemId)
    
    suspend fun getCheapestPriceForItem(masterItemId: Long): StoreItemPrice? =
        storeDao.getCheapestPriceForItem(masterItemId)

    fun getStorePricesWithNames(masterItemId: Long): Flow<List<StorePriceDetail>> =
        storeDao.getStorePricesWithNames(masterItemId)

    suspend fun getPriceForItemAtStore(masterItemId: Long, storeId: Long): StoreItemPrice? =
        storeDao.getPriceForItemAtStore(masterItemId, storeId)
    
    // Stats
    val itemStats: Flow<List<ItemStat>> = statsDao.getItemStats()
    val expensiveItems: Flow<List<ExpensiveItem>> = statsDao.getMostExpensiveItems()
    val spentPerMonth: Flow<List<PeriodStat>> = statsDao.getSpentPerMonth()
    val spentByCategory: Flow<List<CategoryStat>> = statsDao.getSpentByCategory()
    val spentByStore: Flow<List<StoreStat>> = statsDao.getSpentByStore()

    fun getItemStats(startDate: Long, endDate: Long) = statsDao.getItemStatsFiltered(startDate, endDate)
    fun getExpensiveItems(startDate: Long, endDate: Long) = statsDao.getMostExpensiveItemsFiltered(startDate, endDate)
    fun getSpentPerMonth(startDate: Long, endDate: Long) = statsDao.getSpentPerMonthFiltered(startDate, endDate)
    fun getSpentByCategory(startDate: Long, endDate: Long) = statsDao.getSpentByCategoryFiltered(startDate, endDate)
    fun getSpentByStore(startDate: Long, endDate: Long) = statsDao.getSpentByStoreFiltered(startDate, endDate)

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
        // If item was already tracked, just update the item itself
        // If it's newly purchased and not tracked, update the master item too
        if (item.purchased && !item.isTracked) {
            val updatedItem = item.copy(isTracked = true)
            shoppingItemDao.updateItem(updatedItem)
            updateMasterItem(updatedItem)
        } else {
            shoppingItemDao.updateItem(item)
        }
    }

    suspend fun deleteShoppingItem(item: ShoppingItem) =
        shoppingItemDao.deleteItem(item)

    fun searchItems(query: String): Flow<List<ShoppingItem>> =
        shoppingItemDao.searchItems(query)

    fun getPurchaseHistory(itemName: String): Flow<List<PurchaseHistoryItem>> =
        shoppingItemDao.getPurchaseHistory(itemName)

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

    suspend fun mergeMasterItems(sourceItem: MasterItem, targetItem: MasterItem) {
        // 1. Update all shopping items that used the source name
        shoppingItemDao.updateItemNames(sourceItem.name, targetItem.name)

        // 2. Update/Merge store prices
        val sourcePrices = storeDao.getPricesForItemDirect(sourceItem.id)
        for (sourcePrice in sourcePrices) {
            val existingTargetPrice = storeDao.getPriceForItemAtStore(targetItem.id, sourcePrice.storeId)
            if (existingTargetPrice == null) {
                // Just move it
                storeDao.insertStorePrice(sourcePrice.copy(masterItemId = targetItem.id))
            } else {
                // Merge: keep best/latest info
                val mergedPrice = existingTargetPrice.copy(
                    lastPrice = if (sourcePrice.lastUpdated > existingTargetPrice.lastUpdated) sourcePrice.lastPrice else existingTargetPrice.lastPrice,
                    lowestPrice = minOf(sourcePrice.lowestPrice, existingTargetPrice.lowestPrice),
                    highestPrice = maxOf(sourcePrice.highestPrice, existingTargetPrice.highestPrice),
                    lastUpdated = maxOf(sourcePrice.lastUpdated, existingTargetPrice.lastUpdated)
                )
                storeDao.insertStorePrice(mergedPrice)
            }
        }

        // 3. Recalculate target item stats using simple average of unit prices
        val totalPurchaseCount = targetItem.purchaseCount + sourceItem.purchaseCount
        val totalQty = targetItem.totalQuantity + sourceItem.totalQuantity
        val newAverage = if (totalPurchaseCount > 0) {
            (targetItem.averagePrice * targetItem.purchaseCount + sourceItem.averagePrice * sourceItem.purchaseCount) / totalPurchaseCount
        } else targetItem.averagePrice

        val updatedTarget = targetItem.copy(
            averagePrice = newAverage,
            lastPrice = if (sourceItem.lastPurchaseDate > targetItem.lastPurchaseDate) sourceItem.lastPrice else targetItem.lastPrice,
            lowestPrice = if (targetItem.purchaseCount == 0 && sourceItem.purchaseCount == 0) 0.0
                          else if (targetItem.purchaseCount == 0) sourceItem.lowestPrice
                          else if (sourceItem.purchaseCount == 0) targetItem.lowestPrice
                          else minOf(targetItem.lowestPrice, sourceItem.lowestPrice),
            highestPrice = maxOf(targetItem.highestPrice, sourceItem.highestPrice),
            totalQuantity = totalQty,
            purchaseCount = totalPurchaseCount,
            lastPurchaseDate = maxOf(targetItem.lastPurchaseDate, sourceItem.lastPurchaseDate)
        )
        masterItemDao.updateMasterItem(updatedTarget)

        // 4. Delete source item
        masterItemDao.deleteMasterItem(sourceItem)
    }

    private suspend fun updateMasterItem(shoppingItem: ShoppingItem) {
        val shoppingList = shoppingListDao.getShoppingListById(shoppingItem.listId) ?: return
        
        // Only track items from the "Groceries" category in the smart database
        val category = shoppingList.categoryId?.let { categoryDao.getCategoryById(it) }
        val categoryName = category?.name ?: "Groceries" // Default is Groceries
        
        if (categoryName != "Groceries") return

        val name = shoppingItem.itemName
        val unitPrice = shoppingItem.actualPrice ?: shoppingItem.estimatedPrice ?: 0.0
        val qty = shoppingItem.quantity
        val date = System.currentTimeMillis()

        // Get effective storeId: item-level override OR list-level default
        val effectiveStoreId = shoppingItem.storeId ?: shoppingList.storeId

        val existingMaster = masterItemDao.getMasterItemByName(name)
        val masterId = if (existingMaster == null) {
            val newMaster = MasterItem(
                name = name,
                averagePrice = unitPrice,
                lastPrice = unitPrice,
                lowestPrice = unitPrice,
                highestPrice = unitPrice,
                totalQuantity = qty,
                purchaseCount = 1,
                lastPurchaseDate = date
            )
            masterItemDao.insertMasterItem(newMaster)
        } else {
            val newCount = existingMaster.purchaseCount + 1
            val newAverage = (existingMaster.averagePrice * existingMaster.purchaseCount + unitPrice) / newCount
            
            val updatedMaster = existingMaster.copy(
                averagePrice = newAverage,
                lastPrice = unitPrice,
                lowestPrice = if (existingMaster.purchaseCount == 0) unitPrice else minOf(existingMaster.lowestPrice, unitPrice),
                highestPrice = maxOf(existingMaster.highestPrice, unitPrice),
                totalQuantity = existingMaster.totalQuantity + qty,
                purchaseCount = newCount,
                lastPurchaseDate = date
            )
            masterItemDao.updateMasterItem(updatedMaster)
            existingMaster.id
        }

        // Update StorePrice if we have a storeId
        if (effectiveStoreId != null) {
            val existingStorePrice = storeDao.getPriceForItemAtStore(masterId, effectiveStoreId)
            if (existingStorePrice == null) {
                storeDao.insertStorePrice(
                    StoreItemPrice(
                        masterItemId = masterId,
                        storeId = effectiveStoreId,
                        lastPrice = unitPrice,
                        lowestPrice = unitPrice,
                        highestPrice = unitPrice,
                        lastUpdated = date
                    )
                )
            } else {
                storeDao.insertStorePrice(
                    existingStorePrice.copy(
                        lastPrice = unitPrice,
                        lowestPrice = minOf(existingStorePrice.lowestPrice, unitPrice),
                        highestPrice = maxOf(existingStorePrice.highestPrice, unitPrice),
                        lastUpdated = date
                    )
                )
            }
        }
    }

    // Photos
    fun getPhotosForList(listId: Long): Flow<List<Photo>> =
        photoDao.getPhotosForList(listId)

    suspend fun insertPhoto(photo: Photo) =
        photoDao.insertPhoto(photo)

    suspend fun deletePhoto(photo: Photo) =
        photoDao.deletePhoto(photo)
}
