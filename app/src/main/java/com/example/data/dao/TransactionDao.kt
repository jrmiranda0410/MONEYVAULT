package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, timeFormatted DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, timeFormatted DESC, id DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionFlowById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE dateEpochDay = :epochDay AND isVoided = 0")
    fun getTransactionsForDate(epochDay: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateEpochDay >= :startEpochDay AND dateEpochDay <= :endEpochDay AND isVoided = 0")
    fun getTransactionsForDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateEpochDay >= :startEpochDay AND dateEpochDay <= :endEpochDay AND isVoided = 0")
    suspend fun getTransactionsForDateRangeSync(startEpochDay: Long, endEpochDay: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isVoided = 0")
    suspend fun getAllActiveTransactionsSync(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isVoided = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun voidTransaction(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("""
        SELECT * FROM transactions 
        WHERE (:type IS NULL OR type = :type)
          AND (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:startDate IS NULL OR dateEpochDay >= :startDate)
          AND (:endDate IS NULL OR dateEpochDay <= :endDate)
          AND (:query = '' OR description LIKE '%' || :query || '%' OR merchantOrSource LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY dateEpochDay DESC, timeFormatted DESC, id DESC
    """)
    fun filterTransactions(
        type: String?,
        accountId: Long?,
        categoryId: Long?,
        startDate: Long?,
        endDate: Long?,
        query: String
    ): Flow<List<TransactionEntity>>
}
