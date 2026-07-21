package com.corner.myshoppinglist.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingList::class,
            parentColumns = ["id"],
            childColumns = ["shoppingListId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shoppingListId"])]
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shoppingListId: Long,
    val uri: String
)
