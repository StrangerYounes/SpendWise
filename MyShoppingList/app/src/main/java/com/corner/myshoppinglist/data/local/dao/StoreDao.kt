package com.corner.myshoppinglist.data.local.dao

import androidx.room.*
import com.corner.myshoppinglist.data.local.entities.Store
import com.corner.myshoppinglist.data.local.entities.StoreItemPrice
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores ORDER BY name ASC")
    fun getAllStores(): Flow<List<Store>>

    @Query("SELECT * FROM stores WHERE id = :id")
    suspend fun getStoreById(id: Long): Store?

    @Query("SELECT * FROM stores WHERE name = :name LIMIT 1")
    suspend fun getStoreByName(name: String): Store?

    @Query("SELECT * FROM store_item_prices")
    fun getAllStorePrices(): Flow<List<StoreItemPrice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: Store): Long

    @Update
    suspend fun updateStore(store: Store)

    @Delete
    suspend fun deleteStore(store: Store)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorePrice(price: StoreItemPrice)

    @Query("SELECT * FROM store_item_prices WHERE masterItemId = :masterItemId")
    fun getPricesForItem(masterItemId: Long): Flow<List<StoreItemPrice>>

    @Query("SELECT * FROM store_item_prices WHERE masterItemId = :masterItemId AND storeId = :storeId")
    suspend fun getPriceForItemAtStore(masterItemId: Long, storeId: Long): StoreItemPrice?
    
    @Query("SELECT * FROM store_item_prices WHERE masterItemId = :masterItemId ORDER BY lastPrice ASC LIMIT 1")
    suspend fun getCheapestPriceForItem(masterItemId: Long): StoreItemPrice?

    @Query("""
        SELECT sip.*, s.name as storeName 
        FROM store_item_prices sip 
        INNER JOIN stores s ON sip.storeId = s.id 
        WHERE sip.masterItemId = :masterItemId 
        ORDER BY sip.lastPrice ASC
    """)
    fun getStorePricesWithNames(masterItemId: Long): Flow<List<StorePriceDetail>>
}

data class StorePriceDetail(
    @Embedded val storeItemPrice: StoreItemPrice,
    val storeName: String
)
