package com.corner.myshoppinglist.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    @Query("""
        SELECT itemName, SUM(quantity) as totalQuantity, SUM(COALESCE(actualPrice, estimatedPrice, 0) * quantity) as totalSpent
        FROM shopping_items
        WHERE purchased = 1
        GROUP BY itemName
        ORDER BY totalSpent DESC
    """)
    fun getItemStats(): Flow<List<ItemStat>>

    @Query("""
        SELECT itemName, MAX(COALESCE(actualPrice, estimatedPrice, 0)) as maxPrice
        FROM shopping_items
        WHERE purchased = 1
        GROUP BY itemName
        ORDER BY maxPrice DESC
        LIMIT 5
    """)
    fun getMostExpensiveItems(): Flow<List<ExpensiveItem>>

    @Query("""
        SELECT 
            strftime('%Y-%m', createdDate / 1000, 'unixepoch') as period,
            SUM(COALESCE(actualPrice, estimatedPrice, 0) * quantity) as totalSpent
        FROM shopping_items si
        JOIN shopping_lists sl ON si.listId = sl.id
        WHERE si.purchased = 1
        GROUP BY period
        ORDER BY period DESC
    """)
    fun getSpentPerMonth(): Flow<List<PeriodStat>>
}

data class ItemStat(
    val itemName: String,
    val totalQuantity: Double,
    val totalSpent: Double
)

data class ExpensiveItem(
    val itemName: String,
    val maxPrice: Double
)

data class PeriodStat(
    val period: String,
    val totalSpent: Double
)
