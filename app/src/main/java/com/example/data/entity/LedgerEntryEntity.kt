package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ledger_entries",
    indices = [
        Index("transactionId"),
        Index("accountId")
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val transactionId: Long,
    val accountId: Long? = null, // null for external/category balancing ledger
    val accountName: String,
    val debit: Long = 0L, // in minor units
    val credit: Long = 0L, // in minor units
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
