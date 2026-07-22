package com.corner.myshoppinglist.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "shopping_lists",
    foreignKeys = [
        ForeignKey(
            entity = Store::class,
            parentColumns = ["id"],
            childColumns = ["storeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["storeId"])]
)
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdDate: Long = System.currentTimeMillis(),
    val completedDate: Long? = null,
    val isCompleted: Boolean = false,
    val storeId: Long? = null
)
