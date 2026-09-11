package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): UserSettingsEntity?

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<UserSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettingsEntity)

    @Update
    suspend fun updateSettings(settings: UserSettingsEntity)

    @Query("UPDATE user_settings SET failedPinAttempts = :attempts, lockoutUntilMillis = :lockout WHERE id = 1")
    suspend fun updateLockout(attempts: Int, lockout: Long)

    @Query("UPDATE user_settings SET pinHash = :hash, pinSalt = :salt WHERE id = 1")
    suspend fun updatePin(hash: String, salt: String)

    @Query("UPDATE user_settings SET themeMode = :theme WHERE id = 1")
    suspend fun updateTheme(theme: String)

    @Query("UPDATE user_settings SET currencySymbol = :currency WHERE id = 1")
    suspend fun updateCurrency(currency: String)

    @Query("UPDATE user_settings SET biometricEnabled = :enabled WHERE id = 1")
    suspend fun updateBiometric(enabled: Boolean)

    @Query("UPDATE user_settings SET notificationsEnabled = :enabled WHERE id = 1")
    suspend fun updateNotifications(enabled: Boolean)
}
