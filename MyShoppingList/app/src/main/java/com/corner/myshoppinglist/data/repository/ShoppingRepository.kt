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
        shoppingItemDao.updateItem(item)

        // If item is purchased and has a price (actual or estimated), update tracking
        if (item.purchased) {
            updateMasterItem(item)
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

    private suspend fun updateMasterItem(shoppingItem: ShoppingItem) {
        val name = shoppingItem.itemName
        val price = shoppingItem.actualPrice ?: shoppingItem.estimatedPrice ?: return
        val date = System.currentTimeMillis()

        // Get effective storeId: item-level override OR list-level default
        val effectiveStoreId = shoppingItem.storeId ?: shoppingListDao.getShoppingListById(shoppingItem.listId)?.storeId

        val existingMaster = masterItemDao.getMasterItemByName(name)
        val masterId = if (existingMaster == null) {
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
                        lastPrice = price,
                        lowestPrice = price,
                        highestPrice = price,
                        lastUpdated = date
                    )
                )
            } else {
                storeDao.insertStorePrice(
                    existingStorePrice.copy(
                        lastPrice = price,
                        lowestPrice = minOf(existingStorePrice.lowestPrice, price),
                        highestPrice = maxOf(existingStorePrice.highestPrice, price),
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
