package com.corner.myshoppinglist.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class Store(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
