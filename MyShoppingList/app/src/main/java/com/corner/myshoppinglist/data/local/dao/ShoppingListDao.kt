package com.corner.myshoppinglist.data.local.dao

import androidx.room.*
import com.corner.myshoppinglist.data.local.entities.ShoppingList
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    @Query("""
        SELECT sl.*, 
        COUNT(si.id) as itemCount, 
        SUM(COALESCE(si.estimatedPrice, 0) * si.quantity) as estimatedTotal,
        SUM(CASE WHEN si.purchased THEN COALESCE(si.actualPrice, si.estimatedPrice, 0) * si.quantity ELSE 0 END) as actualTotal
        FROM shopping_lists sl
        LEFT JOIN shopping_items si ON sl.id = si.listId
        GROUP BY sl.id
        ORDER BY sl.createdDate DESC
    """)
    fun getAllShoppingListsWithDetails(): Flow<List<ShoppingListWithDetails>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    suspend fun getShoppingListById(id: Long): ShoppingList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(shoppingList: ShoppingList): Long

    @Update
    suspend fun updateShoppingList(shoppingList: ShoppingList)

    @Delete
    suspend fun deleteShoppingList(shoppingList: ShoppingList)

    @Query("""
        SELECT sl.*, 
        COUNT(si.id) as itemCount, 
        SUM(COALESCE(si.estimatedPrice, 0) * si.quantity) as estimatedTotal,
        SUM(CASE WHEN si.purchased THEN COALESCE(si.actualPrice, si.estimatedPrice, 0) * si.quantity ELSE 0 END) as actualTotal
        FROM shopping_lists sl
        LEFT JOIN shopping_items si ON sl.id = si.listId
        WHERE sl.name LIKE '%' || :query || '%'
        GROUP BY sl.id
        ORDER BY sl.createdDate DESC
    """)
    fun searchShoppingListsWithDetails(query: String): Flow<List<ShoppingListWithDetails>>
}

data class ShoppingListWithDetails(
    @Embedded val shoppingList: ShoppingList,
    val itemCount: Int,
    val estimatedTotal: Double,
    val actualTotal: Double
)
