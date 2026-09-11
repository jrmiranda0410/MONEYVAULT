package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [
        Index("timestamp"),
        Index("action")
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val action: String, // "Transaction created", "Transaction edited", "Transaction voided", "Account created", "Account modified", "PIN changed"
    val targetId: String, // e.g. "TX#1", "ACC#2"
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
