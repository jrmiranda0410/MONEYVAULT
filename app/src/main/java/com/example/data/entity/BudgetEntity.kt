package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index("categoryId"),
        Index(value = ["categoryId", "month", "year"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val categoryId: Long? = null, // null means Overall Monthly Budget
    val monthlyLimit: Long, // minor units
    val month: Int, // 1 - 12
    val year: Int // e.g. 2026
)
