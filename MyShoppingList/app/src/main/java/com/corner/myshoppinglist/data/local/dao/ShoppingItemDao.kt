package com.corner.myshoppinglist.data.local.dao

import androidx.room.*
import com.corner.myshoppinglist.data.local.entities.ShoppingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY purchased ASC, orderIndex ASC")
    fun getItemsForList(listId: Long): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE id = :id")
    suspend fun getItemById(id: Long): ShoppingItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem): Long

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Delete
    suspend fun deleteItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun deleteItemsByListId(listId: Long)

    @Query("SELECT * FROM shopping_items WHERE itemName LIKE '%' || :query || '%'")
    fun searchItems(query: String): Flow<List<ShoppingItem>>

    @Query("""
        SELECT si.*, sl.name as listName, sl.createdDate as listDate, s.name as storeName
        FROM shopping_items si 
        INNER JOIN shopping_lists sl ON si.listId = sl.id 
        LEFT JOIN stores s ON COALESCE(si.storeId, sl.storeId) = s.id
        WHERE si.itemName = :itemName AND si.purchased = 1 
        ORDER BY sl.createdDate DESC
    """)
    fun getPurchaseHistory(itemName: String): Flow<List<PurchaseHistoryItem>>
}

data class PurchaseHistoryItem(
    @Embedded val shoppingItem: ShoppingItem,
    val listName: String,
    val listDate: Long,
    val storeName: String?
)
