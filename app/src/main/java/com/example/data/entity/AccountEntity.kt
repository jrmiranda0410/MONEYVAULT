package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType(val displayName: String) {
    BANK("Bank"),
    CASH("Cash"),
    SAVINGS("Savings"),
    UPI("UPI"),
    WALLET("Wallet"),
    OTHER_BANK("Other Bank"),
    CREDIT_CARD("Credit Card"),
    OTHER("Other")
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String, // from AccountType
    val openingBalance: Long, // in minor units (paise)
    val currentBalance: Long, // in minor units (paise)
    val isArchived: Boolean = false
)
