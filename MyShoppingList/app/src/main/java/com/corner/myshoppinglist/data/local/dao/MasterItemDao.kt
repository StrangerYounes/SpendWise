package com.corner.myshoppinglist.data.local.dao

import androidx.room.*
import com.corner.myshoppinglist.data.local.entities.MasterItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterItemDao {
    @Query("SELECT * FROM master_items ORDER BY name COLLATE NOCASE ASC")
    fun getAllMasterItems(): Flow<List<MasterItem>>

    @Query("SELECT * FROM master_items WHERE name = :name")
    suspend fun getMasterItemByName(name: String): MasterItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterItem(item: MasterItem): Long

    @Update
    suspend fun updateMasterItem(item: MasterItem)

    @Delete
    suspend fun deleteMasterItem(item: MasterItem)

    @Query("SELECT * FROM master_items WHERE name LIKE :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchMasterItems(query: String): Flow<List<MasterItem>>
}
