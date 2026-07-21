package com.corner.myshoppinglist.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdDate: Long = System.currentTimeMillis(),
    val completedDate: Long? = null,
    val isCompleted: Boolean = false
)
