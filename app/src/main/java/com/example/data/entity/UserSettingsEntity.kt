package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val pinHash: String,
    val pinSalt: String,
    val biometricEnabled: Boolean = false,
    val currencySymbol: String = "₹",
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val notificationsEnabled: Boolean = true,
    val failedPinAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0L
)
