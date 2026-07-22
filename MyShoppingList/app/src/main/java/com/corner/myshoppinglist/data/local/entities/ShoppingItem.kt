package com.corner.myshoppinglist.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val itemName: String,
    val estimatedPrice: Double? = null,
    val actualPrice: Double? = null,
    val quantity: Double = 1.0,
    val unit: String? = null,
    val purchased: Boolean = false,
    val orderIndex: Int = 0,
    val notes: String? = null
)
