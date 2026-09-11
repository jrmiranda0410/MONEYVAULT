package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.LedgerEntryEntity
import kotlinx.coroutines.flow.Flow

data class LedgerBalanceSummary(
    val totalDebit: Long,
    val totalCredit: Long
)

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries WHERE transactionId = :transactionId ORDER BY id ASC")
    fun getEntriesForTransaction(transactionId: Long): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE transactionId = :transactionId ORDER BY id ASC")
    suspend fun getEntriesForTransactionSync(transactionId: Long): List<LedgerEntryEntity>

    @Query("SELECT * FROM ledger_entries ORDER BY timestamp DESC, id DESC")
    fun getAllEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT COALESCE(SUM(debit), 0) AS totalDebit, COALESCE(SUM(credit), 0) AS totalCredit FROM ledger_entries")
    suspend fun getLedgerSummary(): LedgerBalanceSummary

    @Query("SELECT COALESCE(SUM(debit), 0) AS totalDebit, COALESCE(SUM(credit), 0) AS totalCredit FROM ledger_entries")
    fun getLedgerSummaryFlow(): Flow<LedgerBalanceSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LedgerEntryEntity>)

    @Query("DELETE FROM ledger_entries WHERE transactionId = :transactionId")
    suspend fun deleteEntriesForTransaction(transactionId: Long)
}
