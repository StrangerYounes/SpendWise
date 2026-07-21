package com.corner.myshoppinglist.data.local.dao

import androidx.room.*
import com.corner.myshoppinglist.data.local.entities.Photo
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE shoppingListId = :listId")
    fun getPhotosForList(listId: Long): Flow<List<Photo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: Photo): Long

    @Delete
    suspend fun deletePhoto(photo: Photo)
}
