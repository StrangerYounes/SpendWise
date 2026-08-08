package com.corner.myshoppinglist.data.model

import java.util.UUID

data class ScannedItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Double = 1.0,
    val unit: String? = null,
    val unitPrice: Double? = null,
    val totalPrice: Double? = null,
    val isSelected: Boolean = true
)
