package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CategoryType {
    INCOME,
    EXPENSE
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String, // "INCOME" or "EXPENSE"
    val iconName: String = "Category",
    val colorHex: Long = 0xFF4CAF50
)
