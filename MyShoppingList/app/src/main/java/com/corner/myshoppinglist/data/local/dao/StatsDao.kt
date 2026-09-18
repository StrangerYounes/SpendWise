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
        SELECT itemName, SUM(quantity) as totalQuantity, SUM(COALESCE(si.actualPrice, si.estimatedPrice, 0) * si.quantity) as totalSpent
        FROM shopping_items si
        JOIN shopping_lists sl ON si.listId = sl.id
        WHERE si.purchased = 1 AND COALESCE(sl.completedDate, sl.createdDate) BETWEEN :startDate AND :endDate
        GROUP BY itemName
        ORDER BY totalSpent DESC
    """)
    fun getItemStatsFiltered(startDate: Long, endDate: Long): Flow<List<ItemStat>>

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
        SELECT itemName, MAX(COALESCE(si.actualPrice, si.estimatedPrice, 0)) as maxPrice
        FROM shopping_items si
        JOIN shopping_lists sl ON si.listId = sl.id
        WHERE si.purchased = 1 AND COALESCE(sl.completedDate, sl.createdDate) BETWEEN :startDate AND :endDate
        GROUP BY itemName
        ORDER BY maxPrice DESC
        LIMIT 5
    """)
    fun getMostExpensiveItemsFiltered(startDate: Long, endDate: Long): Flow<List<ExpensiveItem>>

    @Query("""
        SELECT 
            strftime('%Y-%m', COALESCE(sl.completedDate, sl.createdDate) / 1000, 'unixepoch') as period,
            SUM(COALESCE(actualPrice, estimatedPrice, 0) * quantity) as totalSpent
        FROM shopping_items si
        JOIN shopping_lists sl ON si.listId = sl.id
        WHERE si.purchased = 1
        GROUP BY period
        ORDER BY period DESC
    """)
    fun getSpentPerMonth(): Flow<List<PeriodStat>>

    @Query("""
        SELECT 
            strftime('%Y-%m', COALESCE(sl.completedDate, sl.createdDate) / 1000, 'unixepoch') as period,
            SUM(COALESCE(actualPrice, estimatedPrice, 0) * quantity) as totalSpent
        FROM shopping_items si
        JOIN shopping_lists sl ON si.listId = sl.id
        WHERE si.purchased = 1 AND COALESCE(sl.completedDate, sl.createdDate) BETWEEN :startDate AND :endDate
        GROUP BY period
        ORDER BY period DESC
    """)
    fun getSpentPerMonthFiltered(startDate: Long, endDate: Long): Flow<List<PeriodStat>>

    @Query("""
        SELECT 
            c.name as categoryName,
            c.color as categoryColor,
            SUM(COALESCE(si.actualPrice, si.estimatedPrice, 0) * si.quantity) as totalSpent
        FROM shopping_items si
        JOIN shopping_lists sl ON si.listId = sl.id
        JOIN categories c ON COALESCE(sl.categoryId, (SELECT id FROM categories WHERE name = 'Groceries' LIMIT 1)) = c.id
        WHERE si.purchased = 1
        GROUP BY c.id
        ORDER BY totalSpent DESC
    """)
    fun getSpentByCategory(): Flow<List<CategoryStat>>

    @Query("""
        SELECT 
            c.name as categoryName,
            c.color as categoryColor,
            SUM(COALESCE(si.actualPrice, si.estimatedPrice, 0) * si.quantity) as totalSpent
        FROM shopping_items si
        JOIN shopping_lists sl ON si.listId = sl.id
        JOIN categories c ON COALESCE(sl.categoryId, (SELECT id FROM categories WHERE name = 'Groceries' LIMIT 1)) = c.id
        WHERE si.purchased = 1 AND COALESCE(sl.completedDate, sl.createdDate) BETWEEN :startDate AND :endDate
        GROUP BY c.id
        ORDER BY totalSpent DESC
    """)
    fun getSpentByCategoryFiltered(startDate: Long, endDate: Long): Flow<List<CategoryStat>>

    @Query("""
        SELECT 
            s.name as storeName,
            SUM(COALESCE(si.actualPrice, si.estimatedPrice, 0) * si.quantity) as totalSpent
        FROM shopping_items si
        JOIN shopping_lists sl ON si.listId = sl.id
        JOIN stores s ON COALESCE(si.storeId, sl.storeId) = s.id
        WHERE si.purchased = 1
        GROUP BY s.id
        ORDER BY totalSpent DESC
    """)
    fun getSpentByStore(): Flow<List<StoreStat>>

    @Query("""
        SELECT 
            s.name as storeName,
            SUM(COALESCE(si.actualPrice, si.estimatedPrice, 0) * si.quantity) as totalSpent
        FROM shopping_items si
        JOIN shopping_lists sl ON si.listId = sl.id
        JOIN stores s ON COALESCE(si.storeId, sl.storeId) = s.id
        WHERE si.purchased = 1 AND COALESCE(sl.completedDate, sl.createdDate) BETWEEN :startDate AND :endDate
        GROUP BY s.id
        ORDER BY totalSpent DESC
    """)
    fun getSpentByStoreFiltered(startDate: Long, endDate: Long): Flow<List<StoreStat>>
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

data class CategoryStat(
    val categoryName: String,
    val categoryColor: Int,
    val totalSpent: Double
)

data class StoreStat(
    val storeName: String,
    val totalSpent: Double
)
