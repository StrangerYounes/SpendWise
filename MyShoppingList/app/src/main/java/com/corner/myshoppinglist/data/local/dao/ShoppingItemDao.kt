package com.corner.myshoppinglist.data.local.dao

import androidx.room.*
import com.corner.myshoppinglist.data.local.entities.ShoppingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY orderIndex ASC")
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
}
