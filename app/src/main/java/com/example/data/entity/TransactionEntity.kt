package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),
        Index("toAccountId"),
        Index("categoryId"),
        Index("dateEpochDay")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String, // INCOME, EXPENSE, TRANSFER
    val amount: Long, // minor units (paise)
    val accountId: Long, // primary account (expense source / income dest / transfer from)
    val toAccountId: Long? = null, // transfer destination account
    val categoryId: Long? = null,
    val dateEpochDay: Long, // LocalDate.toEpochDay()
    val timeFormatted: String, // HH:mm
    val merchantOrSource: String = "",
    val description: String = "",
    val notes: String = "",
    val isVoided: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
